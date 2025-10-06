package cam72cam.immersiverailroading.rail;

import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RailGraphManager {
    private final World world;
    private final RailRegionManager regionManager;

    private final Map<Integer, RailNode> nodes = new HashMap<>();
    private final Map<Integer, Vec3i> idToStartPos = new HashMap<>();
    private final AtomicInteger nextId;

    public RailGraphManager(World world) {
        this.world = world;
        this.regionManager = RailRegionAPI.get(world);
        int persisted = regionManager.loadNextIdOrDefault(1);
        this.nextId = new AtomicInteger(Math.max(1, persisted));
    }

    public RailNode createNode(Vec3i start, Vec3i end, RailSettings settings) {
        int id = nextId.getAndIncrement();
        RailNode node = new RailNode(id, start, end, settings);
        nodes.put(id, node);
        idToStartPos.put(id, start);
        try {
            regionManager.saveNode(node);
            regionManager.storeNextId(nextId.get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return node;
    }

    public RailNode getNode(int id) {
        RailNode n = nodes.get(id);
        if (n != null) return n;

        Vec3i pos = idToStartPos.get(id);
        if (pos == null) {
            return null;
        }
        try {
            RailNode loaded = regionManager.loadNode(id, pos);
            if (loaded != null) {
                nodes.put(id, loaded);
                idToStartPos.put(id, loaded.getStart());
            }
            return loaded;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerNodePosition(int id, Vec3i startPos) {
        idToStartPos.put(id, startPos);
    }

    public void saveNode(RailNode node) {
        nodes.put(node.getId(), node);
        idToStartPos.put(node.getId(), node.getStart());
        try {
            regionManager.saveNode(node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void tick() {}
}
