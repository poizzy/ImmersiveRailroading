package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileMast;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockTypeEntity;

public class BlockMast extends BlockTypeEntity {
    public BlockMast() {
        super(ImmersiveRailroading.MODID, "block_mast");
    }

    @Override
    protected BlockEntity constructBlockEntity() {
        return new TileMast();
    }
}
