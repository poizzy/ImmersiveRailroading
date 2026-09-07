package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.MastDefinition;
import cam72cam.immersiverailroading.render.item.MastSnappingUtil;
import cam72cam.immersiverailroading.tile.TileMast;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemMast extends CustomItem {
    public ItemMast() {
        super(ImmersiveRailroading.MODID, "item_mast");
    }

    @Override
    public List<ItemStack> getItemVariants(CreativeTab creativeTab) {
        List<ItemStack> items = new ArrayList<>();

        if (creativeTab != null && creativeTab.equals(ItemTabs.MAST_TAB)) {
            for (MastDefinition def : DefinitionManager.getMasts()) {
                ItemStack stack = new ItemStack(this, 1);
                Data data = new Data(stack);
                data.defID = def.defID;
                data.write();
                items.add(stack);
            }
        }

        return items;
    }

    @Override
    public void onClickAir(Player player, World world, Player.Hand hand) {
        if (world.isClient && hand.equals(Player.Hand.PRIMARY)) {
            GuiTypes.MAST.open(player);
        }
    }

    @Override
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d inBlockPos) {
        if (world.isClient) {
            return ClickResult.ACCEPTED;
        }

        Data data = new Data(player.getHeldItem(hand));
        Vec3i target = world.isReplaceable(pos) ? pos : pos.offset(facing);
        float rotation = (-(Math.round(player.getRotationYawHead() / 15) * 15) - 90);

        if (BlockUtil.isIRRail(world, pos)) {
            TileRailBase base = world.getBlockEntity(pos, TileRailBase.class);
            TileRail parent = base instanceof TileRail p ? p : base.getParentTile();

            VecYPR onTrack = parent != null && parent.info != null ? MastSnappingUtil.getClosestPointOnTrack(world, parent, new Vec3d(pos).add(inBlockPos)) : null;
            if (onTrack == null) {
                return ClickResult.REJECTED;
            }

            int offset = 2;
            rotation = MastSnappingUtil.rightSideYaw(onTrack.getYaw(), player.getRotationYawHead());
            Vec3i off = new Vec3i(VecUtil.fromYaw(offset, rotation));
            target = new Vec3i(onTrack.x, onTrack.y, onTrack.z).add(off);

            target = world.isReplaceable(target) ? target : target.offset(facing);

            rotation -= 90;
        }

        if (world.isAir(target) || world.isReplaceable(target)) {
            world.setBlock(target, IRBlocks.BLOCK_MAST);

            TileMast te = world.getBlockEntity(target, TileMast.class);
            te.setup(data.defID, rotation, data.getDistance());
            te.markDirty();

            return ClickResult.ACCEPTED;
        }
        return ClickResult.REJECTED;
    }

    @Override
    public String getCustomName(ItemStack stack) {
        MastDefinition def = DefinitionManager.getMast(new Data(stack).defID);
        if (def == null) {
            return "";
        }
        return def.name;
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.MAST_TAB);
    }


    public static class Data extends ItemDataSerializer {
        @TagField
        public String defID;
        @TagField
        public Float distance;

        public Data(ItemStack stack) {
            super(stack);
        }

        public float getDistance() {
            return this.distance != null ? distance : 0;
        }
    }
}
