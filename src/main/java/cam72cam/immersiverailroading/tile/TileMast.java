package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.blocks.BlockDummy;
import cam72cam.immersiverailroading.items.ItemMast;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.MastDefinition;
import cam72cam.immersiverailroading.util.RollAndOffsetInfo;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.serialization.*;

import java.util.*;
import java.util.stream.Collectors;

public class TileMast extends BlockEntity {
    @TagSync
    @TagField("defID")
    public String definitionID;
    @TagField(value = "OverheadWires", mapper = WireTagMapper.class)
    private List<OverheadWire> wires = new ArrayList<>();
    @TagSync
    @TagField
    private float angle = 0;
    @TagSync
    @TagField
    private Vec3d offset;
    @TagField(mapper = BoundingBoxTagMapper.class)
    private List<IBoundingBox> boundingBoxes = new ArrayList<>();
    @TagField(mapper = DummyPosTagMapper.class)
    public List<Vec3i> dummyPos = new ArrayList<>();

    public void addWire(Vec3i firstMast, String defId, String firstConnector, String secondConnector) {
        wires.add(new OverheadWire(getWorld().getBlockEntity(firstMast, TileMast.class), this, defId, firstConnector, secondConnector));
        this.markDirty();
    }

    public void setup(String definitionID, float angle, Vec3d offset) {
        this.definitionID = definitionID;
        this.angle = angle;
        this.offset = offset;
        placeDummy();
    }

    private void placeDummy() {
        MastDefinition definition = getDefinition();
        Map<String, ModelGroup> collisionGroups = definition.model.getGroups().entrySet().stream().filter(e -> e.getKey().contains("COLLISION")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!collisionGroups.isEmpty()) {
            for (ModelGroup group : collisionGroups.values()) {
                IBoundingBox boundingBox = IBoundingBox.from(group.min().add(offset), group.max().add(offset)).offset(new Vec3d(0.5, 0, 0.5));
                boundingBoxes.add(boundingBox);
            }
        } else {
            Vec3d max = definition.model.maxOfGroups(definition.model.groups());
            IBoundingBox fallback = IBoundingBox.from(new Vec3d(0, 0, 0), new Vec3d(1, max.y, 1));
            boundingBoxes.add(fallback);
        }

        for (IBoundingBox box : boundingBoxes) {
            double highestY = box.max().y;
            double lowestY = box.min().y;

            int blockDelta = (int) (highestY - lowestY) - 1;

            Vec3i placePos = getPos().up();

            for (int i = 0; i <= blockDelta; i++) {
                getWorld().setBlock(placePos, IRBlocks.BLOCK_DUMMY);
                TileDummy td = getWorld().getBlockEntity(placePos, TileDummy.class);
                td.setParent(this);
                td.setBoundingBox(box.offset(new Vec3d(0, -i - 1, 0)));
                dummyPos.add(placePos);
                placePos = placePos.up();
            }
        }

        boundingBoxes.sort(Comparator.comparingDouble(bb -> bb.center().distanceToSquared(Vec3d.ZERO)));
    }

    public MastDefinition getDefinition() {
        return DefinitionManager.getMast(definitionID);
    }

    public Vec3d getConnectionPoint(String name) {
        Vec3d rotated = getDefinition().connectorPos.get(name).rotateYaw(angle).add(0.5, 0, 0.5);
        return new Vec3d(this.getPos()).add(rotated);
    }

    public List<OverheadWire> getWires() {
        return wires;
    }

    public float getAngle() {
        return angle;
    }

    public Vec3d getOffset() {
        return offset != null ? offset : Vec3d.ZERO;
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
        return boundingBoxes.getFirst();
    }

    @Override
    public ItemStack onPick() {
        ItemStack stack = new ItemStack(IRItems.ITEM_MAST, 1);
        ItemMast.Data data = new ItemMast.Data(stack);
        data.defID = definitionID;
        data.write();
        return stack;
    }

    @Override
    public void onBreak() {
        for (OverheadWire wire : wires) {
            wire.removed();
        }

        for (Vec3i pos : dummyPos) {
            getWorld().breakBlock(pos, false);
        }
    }

    private static class DummyPosTagMapper implements TagMapper<List<Vec3i>> {

        @Override
        public TagAccessor<List<Vec3i>> apply(Class<List<Vec3i>> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>((tagCompound, vec3is) -> tagCompound.setList(fieldName, vec3is, vec3i -> new TagCompound().setVec3i("vec", vec3i)),
                    (tagCompound -> tagCompound.getList(fieldName, compound -> compound.getVec3i("vec"))));
        }
    }

    private static class BoundingBoxTagMapper implements TagMapper<List<IBoundingBox>> {

        @Override
        public TagAccessor<List<IBoundingBox>> apply(Class<List<IBoundingBox>> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>((tagCompound, iBoundingBoxes) ->
                    tagCompound.setList(fieldName, iBoundingBoxes, bb -> new TagCompound().setVec3d("min", bb.min()).setVec3d("max", bb.max())),
                    (tagCompound -> tagCompound.getList(fieldName, compound -> IBoundingBox.from(compound.getVec3d("min"), compound.getVec3d("max")))));
        }
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
