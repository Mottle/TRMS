package moe.liar.trms.client;

import moe.liar.trms.common.MoldFillMaterial;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/** Snapshot consumed by the render thread; it never reads the live block entity during submit. */
public final class MoldBlockEntityRenderState extends BlockEntityRenderState {
    public MoldPattern pattern = MoldPattern.EMPTY;
    public long revision;
    public @Nullable MoldFillMaterial fillMaterial;
    public int coolingTicks;
    public boolean isBlank;
    public MoldMeshBuilder.WorldLighting worldLighting;
    public MoldMeshBuilder.WorldLighting fillWorldLighting;
    public Direction facing = Direction.NORTH;
    public boolean showFrontArrow;
    public boolean showCarvingGuide;
    MoldCarvingGuide.Layout carvingGuide = MoldCarvingGuide.Layout.EMPTY;
    MoldCarvingGuide.Cell hoveredCarvingCell;
    int hoveredCarvingAlpha;
}
