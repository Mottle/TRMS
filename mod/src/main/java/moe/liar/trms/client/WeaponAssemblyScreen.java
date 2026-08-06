package moe.liar.trms.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import moe.liar.trms.common.MoldWeaponAssembly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Non-pausing multiplayer selection screen for the server-authoritative assembly session. */
final class WeaponAssemblyScreen extends Screen {
    private static final int GRID_SIZE = 14;
    private static final int CELL_SIZE = 12;
    private static final int HANDLE_LENGTH = MoldWeaponAssembly.HANDLE_LENGTH;
    private final UUID sessionId;
    private final MoldPattern pattern;
    private final moe.liar.trms.common.MoldFillMaterial material;
    private final Set<Point> legalPoints;
    private Point hovered;
    private boolean confirmed;

    WeaponAssemblyScreen(AssemblyBeginPayload payload) {
        super(Component.translatable("screen.trms.weapon_assembly"));
        sessionId = payload.sessionId();
        pattern = payload.pattern();
        material = payload.material();
        legalPoints = new HashSet<>();
        for (AssemblyBeginPayload.ConnectionPoint point : payload.legalPoints()) {
            legalPoints.add(new Point(point.x(), point.z()));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        hovered = null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        int originX = width / 2 - GRID_SIZE * CELL_SIZE / 2;
        int originY = height / 2 - (GRID_SIZE + HANDLE_LENGTH + 2) * CELL_SIZE / 2;
        hovered = pointAt(mouseX, mouseY, originX, originY);

        graphics.centeredText(font, title, width / 2, originY - 24, 0xFFFFFFFF);
        for (int z = 1; z <= GRID_SIZE; z++) {
            for (int x = 1; x <= GRID_SIZE; x++) {
                int left = originX + (x - 1) * CELL_SIZE;
                int top = originY + (z - 1) * CELL_SIZE;
                int color = pattern.isCarved(x, z) ? materialColor() : 0x30101010;
                graphics.fill(RenderPipelines.GUI, left, top, left + CELL_SIZE - 1, top + CELL_SIZE - 1, color);
            }
        }
        for (Point point : legalPoints) {
            int left = originX + (point.x() - 1) * CELL_SIZE;
            int top = originY + (point.z() - 1) * CELL_SIZE;
            int color = point.equals(hovered) ? 0xFFB8FFB8 : 0xA050D878;
            graphics.fill(RenderPipelines.GUI, left + 2, top + 2, left + CELL_SIZE - 3,
                    top + CELL_SIZE - 3, color);
            if (point.equals(hovered)) {
                for (int index = 0; index < HANDLE_LENGTH; index++) {
                    int handleTop = top + (index + 1) * CELL_SIZE;
                    graphics.fill(RenderPipelines.GUI, left, handleTop,
                            left + CELL_SIZE - 1, handleTop + CELL_SIZE - 1, 0xFF8A5A32);
                }
            }
        }
        graphics.centeredText(font, Component.translatable("screen.trms.weapon_assembly.help"),
                width / 2, originY + (GRID_SIZE + HANDLE_LENGTH + 1) * CELL_SIZE, 0xFFD0D0D0);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 1) {
            onClose();
            return true;
        }
        if (button == 0 && hovered != null && legalPoints.contains(hovered)) {
            confirmed = true;
            ClientPacketDistributor.sendToServer(new AssemblyConfirmPayload(sessionId,
                    (byte) hovered.x(), (byte) hovered.z()));
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return true;
    }

    @Override
    public void onClose() {
        if (!confirmed) {
            ClientPacketDistributor.sendToServer(new AssemblyCancelPayload(sessionId));
        }
        Minecraft.getInstance().setScreen(null);
    }

    private Point pointAt(double mouseX, double mouseY, int originX, int originY) {
        int x = (int) Math.floor((mouseX - originX) / CELL_SIZE) + 1;
        int z = (int) Math.floor((mouseY - originY) / CELL_SIZE) + 1;
        Point point = new Point(x, z);
        return legalPoints.contains(point) ? point : null;
    }

    private int materialColor() {
        MoldFillVisual visual = MoldFillVisual.forMaterial(material);
        return visual == null ? 0xFFC0C0C0 : visual.baseColor();
    }

    private record Point(int x, int z) {
    }
}
