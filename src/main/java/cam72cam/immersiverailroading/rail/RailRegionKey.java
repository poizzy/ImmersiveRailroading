package cam72cam.immersiverailroading.rail;

import cam72cam.mod.math.Vec3i;

public class RailRegionKey {
    public static final int REGION_SIZE = 512;
    public final int rx, rz;

    public RailRegionKey(int rx, int rz) {
        this.rx = rx;
        this.rz = rz;
    }

    public static RailRegionKey fromBlock(Vec3i pos) {
        int rx = Math.floorDiv(pos.x, REGION_SIZE);
        int rz = Math.floorDiv(pos.z, REGION_SIZE);
        return new RailRegionKey(rx, rz);
    }

    public String filename() {
        return String.format("r.%d.%d.trk", rx, rz);
    }
}
