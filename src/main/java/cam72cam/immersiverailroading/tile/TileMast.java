package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemMast;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.MastDefinition;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.*;

import java.util.ArrayList;
import java.util.List;

public class TileMast extends BlockEntity {
    @TagSync
    @TagField("defID")
    private String definitionID;
    @TagField(value = "OverheadWires", mapper = WireTagMapper.class)
    private List<OverheadWire> wires = new ArrayList<>();
    @TagSync
    @TagField
    private int angle = 0;

    public void addWire(Vec3i firstMast, String defId) {
        wires.add(new OverheadWire(getWorld().getBlockEntity(firstMast, TileMast.class), this, defId));
        this.markDirty();
    }

    public void setup(String definitionID, int angle) {
        this.definitionID = definitionID;
        this.angle = angle;
    }

    public MastDefinition getDefinition() {
        return DefinitionManager.getMast(definitionID);
    }

    public Vec3d getConnectionPoint() {
        Vec3d rotated = getDefinition().connectionPos.rotateYaw(angle).add(0.5, 0, 0.5);
        return new Vec3d(this.getPos()).add(rotated);
    }

    public List<OverheadWire> getWires() {
        return wires;
    }

    public int getAngle() {
        return angle;
    }

    @Override
    public IBoundingBox getRenderBoundingBox() {
        MastDefinition def = getDefinition();
        Vec3d min = def.model.minOfGroups(def.model.groups());
        Vec3d max = def.model.maxOfGroups(def.model.groups());

        for (OverheadWire wire : wires) {
            Vec3d otherOffset = wire.delta.rotateYaw(180);
            min = min.min(otherOffset);
            max = max.max(otherOffset);
        }
        return IBoundingBox.from(min, max);
    }

    @Override
    public IBoundingBox getBoundingBox() {
        MastDefinition def = getDefinition();
        double height = def.model.maxOfGroups(def.model.groups()).y;
        return IBoundingBox.from(Vec3d.ZERO, new Vec3d(1, height, 1));
    }

    @Override
    public ItemStack onPick() {
        ItemStack stack = new ItemStack(IRItems.ITEM_MAST, 1);
        ItemMast.Data data = new ItemMast.Data(stack);
        data.defID = definitionID;
        data.write();
        return stack;
    }

    private static class WireTagMapper implements TagMapper<List<OverheadWire>> {

        @Override
        public TagAccessor<List<OverheadWire>> apply(Class<List<OverheadWire>> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>((tagCompound, overheadWires) -> {
                tagCompound.setList(fieldName, overheadWires, w -> {
                    TagCompound compound = new TagCompound();
                    try {
                        TagSerializer.serialize(compound, w);
                    } catch (SerializationException e) {
                        throw new RuntimeException(e);
                    }
                    return compound;
                });
            }, (tagCompound -> tagCompound.getList(fieldName, compound -> {
                OverheadWire wire = new OverheadWire();
                try {
                    TagSerializer.deserialize(compound, wire);
                } catch (SerializationException e) {
                    throw new RuntimeException(e);
                }
                return wire;
            })));
        }
    }
}
