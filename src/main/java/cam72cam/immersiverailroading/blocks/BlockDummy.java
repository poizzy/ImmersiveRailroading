package cam72cam.immersiverailroading.blocks;


import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileDummy;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockTypeEntity;

public class BlockDummy extends BlockTypeEntity {
    public BlockDummy() {
        super(ImmersiveRailroading.MODID, "DUMMY");
    }

    @Override
    protected BlockEntity constructBlockEntity() {
        return new TileDummy();
    }
}
