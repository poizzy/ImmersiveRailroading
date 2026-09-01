package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.MastDefinition;
import cam72cam.immersiverailroading.tile.TileMast;
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
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d inBlockPos) {
        if (world.isClient) {
            return ClickResult.ACCEPTED;
        }

        Vec3i target = world.isReplaceable(pos) ? pos : pos.offset(facing);

        if (world.isAir(target) || world.isReplaceable(target)) {
            Data data = new Data(player.getHeldItem(hand));
            world.setBlock(target, IRBlocks.BLOCK_MAST);

            TileMast te = world.getBlockEntity(target, TileMast.class);
            int rotation = (-(Math.round(player.getRotationYawHead() / 15) * 15) - 90);
            te.setup(data.defID, rotation);
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


        public Data(ItemStack stack) {
            super(stack);
        }
    }
}
