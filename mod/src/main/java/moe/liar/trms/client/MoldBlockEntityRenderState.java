package moe.liar.trms.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/** Snapshot consumed by the render thread; it never reads the live block entity during submit. */
public final class MoldBlockEntityRenderState extends BlockEntityRenderState {
    public MoldPattern pattern = MoldPattern.EMPTY;
    public long revision;
    public MoldMeshBuilder.WorldLighting worldLighting;
    public Direction facing = Direction.NORTH;
    public boolean showCarvingGuide;
    MoldCarvingGuide.Layout carvingGuide = MoldCarvingGuide.Layout.EMPTY;
    MoldCarvingGuide.Cell hoveredCarvingCell;
    int hoveredCarvingAlpha;
}
