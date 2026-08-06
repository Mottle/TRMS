package moe.liar.trms.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Non-pausing multiplayer selection screen for the server-authoritative assembly session. */
final class WeaponAssemblyScreen extends Screen {
    private final UUID sessionId;
    private final MoldPattern pattern;
    private final moe.liar.trms.common.MoldFillMaterial material;
    private final Set<Point> legalPoints;
    private WeaponAssemblyLayout layout;
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
        layout = WeaponAssemblyLayout.forScreen(width, height);
        hovered = null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        hovered = pointAt(mouseX, mouseY);

        graphics.centeredText(font, title, width / 2, layout.originY() - 24, 0xFFFFFFFF);
        for (int z = 1; z <= WeaponAssemblyLayout.GRID_SIZE; z++) {
            for (int x = 1; x <= WeaponAssemblyLayout.GRID_SIZE; x++) {
                int left = layout.cellLeft(x);
                int top = layout.cellTop(z);
                int color = pattern.isCarved(x, z) ? materialColor() : 0x30101010;
                fillCell(graphics, left, top, color);
            }
        }
        for (Point point : legalPoints) {
            int left = layout.cellLeft(point.x());
            int top = layout.cellTop(point.z());
            int color = point.equals(hovered) ? 0xFFB8FFB8 : 0xA050D878;
            int inset = Math.min(2, Math.max(0, layout.cellSize() / 4));
            graphics.fill(RenderPipelines.GUI, left + inset, top + inset,
                    left + layout.cellSize() - inset, top + layout.cellSize() - inset, color);
            if (point.equals(hovered)) {
                for (int index = 0; index < WeaponAssemblyLayout.HANDLE_LENGTH; index++) {
                    int handleTop = top + (index + 1) * layout.cellSize();
                    fillCell(graphics, left, handleTop, 0xFF8A5A32);
                }
            }
        }
        graphics.centeredText(font, Component.translatable("screen.trms.weapon_assembly.help"),
                width / 2, layout.contentBottom() + 5, 0xFFD0D0D0);
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

    private Point pointAt(double mouseX, double mouseY) {
        int x = (int) Math.floor((mouseX - layout.originX()) / layout.cellSize()) + 1;
        int z = (int) Math.floor((mouseY - layout.originY()) / layout.cellSize()) + 1;
        Point point = new Point(x, z);
        return legalPoints.contains(point) ? point : null;
    }

    private void fillCell(GuiGraphicsExtractor graphics, int left, int top, int color) {
        graphics.fill(RenderPipelines.GUI, left, top,
                left + layout.cellSize(), top + layout.cellSize(), color);
    }

    private int materialColor() {
        MoldFillVisual visual = MoldFillVisual.forMaterial(material);
        return visual == null ? 0xFFC0C0C0 : visual.baseColor();
    }

    private record Point(int x, int z) {
    }
}
