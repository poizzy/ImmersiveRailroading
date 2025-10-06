package cam72cam.immersiverailroading.rail;

import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.track.CubicCurve;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.world.World;

import java.io.*;

public class RailNode {
    @TagField
    private int id;
    @TagField
    private Vec3i nodeStart;
    @TagField
    private Vec3i nodeEnd;
    @TagField
    private double length;

    @TagField
    private int nextNode;
    @TagField
    private Vec3i nextNodeStart;

    @TagField
    private boolean isSwitch;
    @TagField
    private boolean switchDir;
    @TagField
    private int nextNodeR;
    @TagField
    private int nextNodeL;

    @TagField
    private RailSettings settings;
    @TagField
    private CubicCurve curve;

    public RailNode(int id, Vec3i start, Vec3i end, RailSettings settings) {
        this.id = id;
        this.nodeStart = start;
        this.nodeEnd = end;
        this.length = end.subtract(start).length();
        this.settings = settings;
        this.isSwitch = settings.type == TrackItems.SWITCH;
    }

    public RailNode(TagCompound compound) {
        try {
            TagSerializer.deserialize(compound, this);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    public int getId() {
        return id;
    }

    public Vec3i getStart() {
        return nodeStart;
    }

    public Vec3i getEnd() {
        return nodeEnd;
    }

    public double getLength() {
        return length;
    }

    public RailSettings getSettings() {
        return settings;
    }

    public boolean isSwitch() {
        return isSwitch;
    }

    // ------------------------------------------------------------------------
    // Curve interpolation
    // ------------------------------------------------------------------------

    public Vec3d interpolate(double distance) {
        if (curve == null) {
            double t = Math.max(0, Math.min(1, distance / Math.max(1e-6, length)));
            return new Vec3d(nodeStart).scale(1 - t).add(new Vec3d(nodeEnd).scale(t));
        }
        double t = Math.max(0, Math.min(1, distance / Math.max(1e-6, length)));
        return curve.position(t);
    }

    public Vec3d interpolate(Vec3d motion) {
        return interpolate(motion.length());
    }

    // ------------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------------

    public TagCompound serialize() {
        TagCompound compound = new TagCompound();
        try {
            TagSerializer.serialize(compound, this);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
        return compound;
    }

    public void save(World world) {
        try {
            RailRegionManager mgr = RailRegionAPI.get(world);
            mgr.saveNode(this);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save RailNode " + id, e);
        }
    }

    public RailNode next(World world) {
        try {
            RailRegionManager mgr = RailRegionAPI.get(world);
            return mgr.loadNode(nextNode, nextNodeStart);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
