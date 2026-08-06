package moe.liar.trms;

import java.util.List;
import moe.liar.horizon.extension.ExtensionConcurrencyContext;
import moe.liar.horizon.extension.ExtensionContext;
import moe.liar.horizon.extension.ExtensionNetworkContext;
import moe.liar.horizon.extension.event.PlayerQuitEvent;
import moe.liar.horizon.extension.event.ServerTickEvent;
import moe.liar.horizon.extension.event.entity.LivingEvent;
import moe.liar.horizon.extension.event.player.PlayerEvent;
import moe.liar.horizon.extension.network.PayloadRequirement;
import moe.liar.trms.common.MoldWeaponAssembly;
import moe.liar.trms.common.MoldWeaponAssembly.ConnectionPoint;
import moe.liar.trms.common.TrmsProtocol;
import net.minecraft.ChatFormatting;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server-authoritative sessions for combining a casting and a wooden handle. */
final class TrmsWeaponAssemblyNetwork {
    private static final long SESSION_TICKS = 1_200L;
    private static final long START_COOLDOWN_TICKS = 4L;
    private static final int MAX_SESSIONS = 4_096;
    private static final TrmsWeaponAssemblySessions<TrmsWeaponPart> SESSIONS =
            new TrmsWeaponAssemblySessions<>(SESSION_TICKS, START_COOLDOWN_TICKS, MAX_SESSIONS);
    private static ExtensionNetworkContext network;

    private TrmsWeaponAssemblyNetwork() {
    }

    static void register(ExtensionContext context) {
        network = context.network();
        network.registerClientboundPayload(TrmsAssemblyBeginPayload.TYPE, TrmsAssemblyBeginPayload.STREAM_CODEC,
                ConnectionProtocol.PLAY, TrmsProtocol.CARVING_TRANSPORT_VERSION, PayloadRequirement.REQUIRED);
        network.registerServerboundPayload(TrmsAssemblyStartPayload.TYPE, TrmsAssemblyStartPayload.STREAM_CODEC,
                ConnectionProtocol.PLAY, TrmsProtocol.CARVING_TRANSPORT_VERSION, PayloadRequirement.REQUIRED,
                TrmsAssemblyStartPayload.class, (listener, payload) -> receiveStart(context.concurrency(), listener));
        network.registerServerboundPayload(TrmsAssemblyConfirmPayload.TYPE, TrmsAssemblyConfirmPayload.STREAM_CODEC,
                ConnectionProtocol.PLAY, TrmsProtocol.CARVING_TRANSPORT_VERSION, PayloadRequirement.REQUIRED,
                TrmsAssemblyConfirmPayload.class, (listener, payload) -> receiveConfirm(context.concurrency(), listener, payload));
        network.registerServerboundPayload(TrmsAssemblyCancelPayload.TYPE, TrmsAssemblyCancelPayload.STREAM_CODEC,
                ConnectionProtocol.PLAY, TrmsProtocol.CARVING_TRANSPORT_VERSION, PayloadRequirement.REQUIRED,
                TrmsAssemblyCancelPayload.class, (listener, payload) -> receiveCancel(context.concurrency(), listener, payload));
        context.events().listen(PlayerQuitEvent.class,
                event -> SESSIONS.remove(event.player().getUUID()));
        context.events().listen(LivingEvent.Death.class, event -> {
            if (event.livingEntity() instanceof ServerPlayer player) {
                SESSIONS.remove(player.getUUID());
            }
        });
        context.events().listen(PlayerEvent.Respawn.class, event -> {
            SESSIONS.remove(event.oldPlayer().getUUID());
            SESSIONS.remove(event.player().getUUID());
        });
        context.events().listen(ServerTickEvent.class,
                event -> SESSIONS.purgeExpired(Integer.toUnsignedLong(event.tickCount())));
    }

    private static void receiveStart(ExtensionConcurrencyContext concurrency, ServerCommonPacketListenerImpl listener) {
        if (!(listener instanceof ServerGamePacketListenerImpl gameListener)) return;
        ServerPlayer player = gameListener.getPlayer();
        concurrency.submitOnRegion(player, () -> startOnOwner(player));
    }

    private static void startOnOwner(ServerPlayer player) {
        if (player.isSpectator() || !player.isCrouching()) return;
        long now = currentServerTick(player);
        ItemStack casting = player.getMainHandItem();
        ItemStack stick = player.getOffhandItem();
        TrmsWeaponPart part = casting.get(TrmsContent.weaponPartComponent());
        if (!casting.is(TrmsContent.weaponPartItem()) || part == null || !stick.is(Items.STICK)) return;

        List<ConnectionPoint> points = MoldWeaponAssembly.legalConnectionPoints(part.pattern().commonPattern());
        if (points.isEmpty()) {
            player.sendSystemMessage(Component.translatable("item.trms.assembled_weapon.no_points")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        TrmsWeaponAssemblySessions.BeginResult<TrmsWeaponPart> begin =
                SESSIONS.begin(player.getUUID(), player.getId(), part, now);
        if (!begin.created()) return;
        TrmsWeaponAssemblySessions.Session<TrmsWeaponPart> session = begin.session();
        List<TrmsAssemblyBeginPayload.TrmsAssemblyPoint> networkPoints = points.stream()
                .map(point -> new TrmsAssemblyBeginPayload.TrmsAssemblyPoint((byte) point.x(), (byte) point.z()))
                .toList();
        network.sendRequired(player, new TrmsAssemblyBeginPayload(session.id(), part.pattern(), part.material(), networkPoints));
    }

    private static void receiveConfirm(ExtensionConcurrencyContext concurrency, ServerCommonPacketListenerImpl listener,
                                       TrmsAssemblyConfirmPayload payload) {
        if (!(listener instanceof ServerGamePacketListenerImpl gameListener)) return;
        ServerPlayer player = gameListener.getPlayer();
        concurrency.submitOnRegion(player, () -> confirmOnOwner(player, payload));
    }

    private static void confirmOnOwner(ServerPlayer player, TrmsAssemblyConfirmPayload payload) {
        TrmsWeaponAssemblySessions.Session<TrmsWeaponPart> session = SESSIONS.get(player.getUUID());
        if (session == null || !session.id().equals(payload.sessionId())
                || session.entityId() != player.getId()) return;
        if (player.isSpectator() || currentServerTick(player) >= session.expiresAt()) {
            SESSIONS.remove(player.getUUID(), session.id());
            return;
        }
        ItemStack casting = player.getMainHandItem();
        ItemStack stick = player.getOffhandItem();
        TrmsWeaponPart part = casting.get(TrmsContent.weaponPartComponent());
        ConnectionPoint point;
        try {
            point = new ConnectionPoint(payload.connectionX(), payload.connectionZ());
        } catch (IllegalArgumentException exception) {
            return;
        }
        if (!casting.is(TrmsContent.weaponPartItem()) || part == null || !part.equals(session.input())
                || !stick.is(Items.STICK) || !MoldWeaponAssembly.isLegalConnection(part.pattern().commonPattern(), point.x(), point.z())) {
            SESSIONS.remove(player.getUUID(), session.id());
            return;
        }

        ItemStack result = new ItemStack(TrmsContent.assembledWeaponItem());
        result.set(TrmsContent.assembledWeaponComponent(), new TrmsAssembledWeapon(
                part.pattern(), part.material(), MoldWeaponAssembly.STICK_MATERIAL, point.x(), point.z()));
        casting.shrink(1);
        stick.shrink(1);
        SESSIONS.remove(player.getUUID(), session.id());
        if (casting.isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, result);
        } else if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
    }

    private static void receiveCancel(ExtensionConcurrencyContext concurrency, ServerCommonPacketListenerImpl listener,
                                      TrmsAssemblyCancelPayload payload) {
        if (!(listener instanceof ServerGamePacketListenerImpl gameListener)) return;
        ServerPlayer player = gameListener.getPlayer();
        concurrency.submitOnRegion(player, () -> {
            SESSIONS.remove(player.getUUID(), payload.sessionId());
        });
    }

    private static long currentServerTick(ServerPlayer player) {
        return Integer.toUnsignedLong(player.level().getServer().getTickCount());
    }
}
