package cam72cam.immersiverailroading.rail;

import cam72cam.mod.world.World;

import java.util.HashMap;
import java.util.Map;

public class RailRegionAPI {
    private static final Map<Integer, RailRegionManager> managers = new HashMap<>();

    public static RailRegionManager get(World world) {
        return managers.computeIfAbsent(world.getId(),
                id -> new RailRegionManager(world.getWorldFolder(), id));
    }
}
