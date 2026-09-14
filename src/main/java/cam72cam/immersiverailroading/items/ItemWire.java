package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.MastConnector;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.MastDefinition;
import cam72cam.immersiverailroading.registry.WireDefinition;
import cam72cam.immersiverailroading.tile.TileDummy;
import cam72cam.immersiverailroading.tile.TileMast;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;

import java.util.*;

import static cam72cam.immersiverailroading.library.ChatText.*;

public class ItemWire extends CustomItem {

    public ItemWire() {
        super(ImmersiveRailroading.MODID, "item_wire");
    }

    @Override
    public void onClickAir(Player player, World world, Player.Hand hand) {
        super.onClickAir(player, world, hand);
        List<TileMast> masts = world.getBlockEntities(TileMast.class);
        TileMast nearest = masts.stream().sorted(Comparator.comparingDouble(te -> player.getPosition().distanceToSquared(new Vec3d(te.getPos())))).toList().getFirst();
        if (nearest == null) {
            return;
        }
        MastDefinition def = nearest.getDefinition();
        for (MastConnector connector : def.connectors.values()) {
            Vec3d localEyes = player.getPositionEyes()
                    .subtract(new Vec3d(nearest.getPos()))
                    .subtract(0.5, 0, 0.5)
                    .subtract(nearest.getOffset())
                    .rotateYaw(-nearest.getAngle());
            Vec3d localLook = player.getLookVector().rotateYaw(-nearest.getAngle());

            IBoundingBox box = connector.getBoundingBox();
            if (box.intersectsSegment(localEyes, localEyes.add(localLook.scale(10)))) {
                ItemStack stack = player.getHeldItem(hand);
                Data data = new Data(stack);

                if (data.firstMast == null) {
                    data.firstMast = nearest.getPos();
                    data.firstDim = world.getId();
                    data.firstConnector = connector.id;
                    data.write();
                    return;
                }

                if (nearest.getPos().equals(data.firstMast)) return;

                if (data.firstDim != world.getId()) {
                    player.sendMessage(WIRE_DIM_MISMATCH.getMessage());
                    return;
                }

                nearest.addWire(data.firstMast, data.defID, data.firstConnector, connector.id);
                new WirePacket(nearest.getPos(), data.defID, data.firstMast, data.firstConnector, connector.id).sendToAll();
                clearData(data);
                break;
            }
        }
    }

    @Override
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d inBlockPos) {
        if (world.isClient) {
            return ClickResult.PASS;
        }

        ItemStack itemStack = player.getHeldItem(hand);
        Data data = new Data(itemStack);

        if (world.isBlock(pos, IRBlocks.BLOCK_MAST) && player.isCrouching()) {
            TileMast tm = world.getBlockEntity(pos, TileMast.class);
            tm.removeWires();
            clearData(data);
            return ClickResult.ACCEPTED;
        } else if (world.isBlock(pos, IRBlocks.BLOCK_DUMMY) && player.isCrouching()) {
            TileMast tm = world.getBlockEntity(pos, TileDummy.class).getParentTile();
            tm.removeWires();
            clearData(data);
            return ClickResult.ACCEPTED;
        } else if (player.isCrouching()) {
            clearData(data);
            return ClickResult.ACCEPTED;
        }

        return ClickResult.REJECTED;
    }

    @Override
    public List<ItemStack> getItemVariants(CreativeTab creativeTab) {
        List<ItemStack> items = new ArrayList<>();
        if (creativeTab != null && creativeTab.equals(ItemTabs.WIRE_TAB)) {
            for (WireDefinition def : DefinitionManager.getWires()) {
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
    public String getCustomName(ItemStack stack) {
        WireDefinition wireDefinition = DefinitionManager.getWire(new Data(stack).defID);
        if (wireDefinition == null) {
            return "";
        }
        return wireDefinition.name;
    }

    private void clearData(Data data) {
        data.firstMast = null;
        data.firstDim = null;
        data.write();
    }

    public static class WirePacket extends Packet {
        @TagField("def")
        public String def;
        @TagField("firstMast")
        public Vec3i firstMast;
        @TagField
        public Vec3i secondMast;
        @TagField
        public Integer targetConnector;
        @TagField
        public Integer parentConnector;

        public WirePacket(){}

        public WirePacket(Vec3i secondMast, String def, Vec3i firstMast, int targetConnector, int parentConnector) {
            this.def = def;
            this.firstMast = firstMast;
            this.secondMast = secondMast;
            this.targetConnector = targetConnector;
            this.parentConnector = parentConnector;
        }

        @Override
        protected void handle() {
            getWorld().getBlockEntity(secondMast, TileMast.class).addWire(firstMast, def, targetConnector, parentConnector);
        }
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.WIRE_TAB);
    }

    public static class Data extends ItemDataSerializer {
        @TagField
        public Vec3i firstMast;
        @TagField
        public String defID;
        @TagField
        public Integer firstDim;
        @TagField
        public Integer firstConnector;

        public Data(ItemStack stack) {
            super(stack);
        }
    }
}
