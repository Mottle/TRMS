package moe.liar.trms;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import moe.liar.trms.common.TrmsProtocol;
import moe.liar.horizon.extension.ExtensionContext;
import moe.liar.horizon.extension.ExtensionNetworkContext;
import moe.liar.horizon.extension.network.ExtensionConfigurationTaskContext;
import moe.liar.horizon.extension.network.ExtensionConfigurationTaskPhase;
import moe.liar.horizon.extension.network.PayloadRequirement;
import moe.liar.horizon.extension.network.PayloadTypes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Required configuration-phase proof that both independently built TRMS endpoints
 * speak the same gameplay protocol.
 *
 * <p>The required payload declarations prevent a vanilla or missing-mod client
 * from reaching play.  The nonce-bound response then provides a specific,
 * user-readable rejection when an installed but incompatible TRMS client
 * advertises another protocol revision.</p>
 */
final class TrmsProtocolHandshake {
    private static final int TIMEOUT_TICKS = 100;
    private static final int MAX_PENDING_TASKS = 256;
    /*
     * The extension API deliberately exposes no disconnect/timeout callback for
     * a configuration task.  Keeping an expiration alongside each nonce means
     * a client that disappears before replying leaves only a bounded transient
     * entry. The next handshake start or response reaps stale entries, while a
     * fixed cap also protects the quiet-server case where no later request
     * arrives to trigger cleanup.
     */
    private static final long PENDING_EXPIRY_NANOS = TimeUnit.SECONDS.toNanos(15L);
    private static final TrmsPendingHandshakeTasks<ExtensionConfigurationTaskContext> PENDING =
            new TrmsPendingHandshakeTasks<>(MAX_PENDING_TASKS);

    static final CustomPacketPayload.Type<TrmsProtocolChallengePayload> CHALLENGE_TYPE =
            PayloadTypes.create(TrmsProtocol.NAMESPACE, "protocol_challenge");
    static final CustomPacketPayload.Type<TrmsProtocolResponsePayload> RESPONSE_TYPE =
            PayloadTypes.create(TrmsProtocol.NAMESPACE, "protocol_response");

    private TrmsProtocolHandshake() {
    }

    static void register(ExtensionContext context) {
        ExtensionNetworkContext network = context.network();
        network.registerClientboundPayload(
                CHALLENGE_TYPE,
                TrmsProtocolChallengePayload.STREAM_CODEC,
                ConnectionProtocol.CONFIGURATION,
                TrmsProtocol.HANDSHAKE_TRANSPORT_VERSION,
                PayloadRequirement.REQUIRED
        );
        network.registerServerboundPayload(
                RESPONSE_TYPE,
                TrmsProtocolResponsePayload.STREAM_CODEC,
                ConnectionProtocol.CONFIGURATION,
                TrmsProtocol.HANDSHAKE_TRANSPORT_VERSION,
                PayloadRequirement.REQUIRED,
                TrmsProtocolResponsePayload.class,
                TrmsProtocolHandshake::handleResponse
        );
        network.configurationTasks().register(
                "protocol_handshake",
                ExtensionConfigurationTaskPhase.BEFORE_COMMON,
                0,
                TIMEOUT_TICKS,
                TrmsProtocolHandshake::start
        );
    }

    private static void start(ExtensionConfigurationTaskContext task) {
        if (!task.neoForgeConnection()) {
            task.fail(Component.literal("TRMS requires the NeoForge client mod (protocol "
                    + TrmsProtocol.VERSION + ")."));
            return;
        }
        if (!task.canSend(CHALLENGE_TYPE.id())) {
            task.fail(Component.literal("TRMS protocol negotiation is unavailable. Install a compatible TRMS client mod "
                    + "(server protocol " + TrmsProtocol.VERSION + ")."));
            return;
        }

        UUID nonce = UUID.randomUUID();
        long nowNanos = System.nanoTime();
        if (!PENDING.add(nonce, task, nowNanos + PENDING_EXPIRY_NANOS, nowNanos)) {
            task.fail(Component.literal("TRMS protocol negotiation is temporarily busy. Please reconnect shortly."));
            return;
        }
        task.send(new TrmsProtocolChallengePayload(nonce, TrmsProtocol.VERSION));
    }

    private static void handleResponse(
            net.minecraft.server.network.ServerCommonPacketListenerImpl listener,
            TrmsProtocolResponsePayload response) {
        Objects.requireNonNull(listener, "listener");
        ExtensionConfigurationTaskContext task = PENDING.remove(response.nonce(), System.nanoTime()).orElse(null);
        if (task == null) {
            return;
        }
        if (response.protocolVersion() != TrmsProtocol.VERSION) {
            task.fail(Component.literal("TRMS protocol mismatch: server requires " + TrmsProtocol.VERSION
                    + ", but the client provides " + response.protocolVersion() + ". "
                    + "Install matching TRMS client/server builds."));
            return;
        }
        task.finish();
    }

    record TrmsProtocolChallengePayload(UUID nonce, int expectedProtocol) implements CustomPacketPayload {
        static final StreamCodec<FriendlyByteBuf, TrmsProtocolChallengePayload> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC,
                TrmsProtocolChallengePayload::nonce,
                ByteBufCodecs.VAR_INT,
                TrmsProtocolChallengePayload::expectedProtocol,
                TrmsProtocolChallengePayload::new
        );

        TrmsProtocolChallengePayload {
            Objects.requireNonNull(nonce, "nonce");
            if (expectedProtocol < 1) {
                throw new IllegalArgumentException("TRMS protocol version must be positive");
            }
        }

        @Override
        public Type<TrmsProtocolChallengePayload> type() {
            return CHALLENGE_TYPE;
        }
    }

    record TrmsProtocolResponsePayload(UUID nonce, int protocolVersion) implements CustomPacketPayload {
        static final StreamCodec<FriendlyByteBuf, TrmsProtocolResponsePayload> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC,
                TrmsProtocolResponsePayload::nonce,
                ByteBufCodecs.VAR_INT,
                TrmsProtocolResponsePayload::protocolVersion,
                TrmsProtocolResponsePayload::new
        );

        TrmsProtocolResponsePayload {
            Objects.requireNonNull(nonce, "nonce");
            if (protocolVersion < 1) {
                throw new IllegalArgumentException("TRMS protocol version must be positive");
            }
        }

        @Override
        public Type<TrmsProtocolResponsePayload> type() {
            return RESPONSE_TYPE;
        }
    }
}
