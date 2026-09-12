package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemMast;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;

public class TileDummy extends BlockEntity {
    @TagField
    private Vec3i parent;
    @TagField
    private Vec3d min;
    @TagField
    private Vec3d max;

    public TileMast getParentTile() {
        return getWorld().getBlockEntity(parent, TileMast.class);
    }

    public void setParent(TileMast parent) {
        this.parent = parent.getPos();
    }

    public void setBoundingBox(IBoundingBox box) {
        this.min = box.min();
        this.max = box.max();
    }

    @Override
    public IBoundingBox getBoundingBox() {
        return IBoundingBox.from(min, max);
    }

    @Override
    public boolean tryBreak(Player player) {
        return true;
    }

    @Override
    public void onBreak() {
        getWorld().breakBlock(parent, true);
    }

    @Override
    public ItemStack onPick() {
        ItemStack stack = new ItemStack(IRItems.ITEM_MAST, 1);
        ItemMast.Data data = new ItemMast.Data(stack);
        data.defID = getParentTile().definitionID;
        data.write();
        return stack;
    }
}
