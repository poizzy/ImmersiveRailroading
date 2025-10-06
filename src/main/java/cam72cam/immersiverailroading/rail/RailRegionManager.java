package cam72cam.immersiverailroading.rail;

import cam72cam.mod.math.Vec3i;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * <pre>
 *     {@code
 *      <dimRoot>/manifest.bin:
 *          magic: 0x49524D46 ('IRMF')
 *          version: int (1)
 *          nextId: int (>=1)
 *     }
 * </pre>
 */
public class RailRegionManager {
    private static final int MANIFEST_MAGIC = 0x49524D46;
    private static final int MANIFEST_VERSION = 1;

    private final File dimRoot;
    private final File regionFolder;
    private final File manifestFile;
    private final LinkedHashMap<RailRegionKey, RailRegionStore> openRegions = new LinkedHashMap<>(16, 0.75f, true);

    private static final int MAX_OPEN = 8;

    public RailRegionManager(File worldFolder, int dimId) {
        this.dimRoot = new File(worldFolder, "ir_tracks/" + dimId + "/");
        this.regionFolder = new File(dimRoot, "region");
        this.manifestFile = new File(dimRoot, "manifest.bin");
        this.regionFolder.mkdirs();
        this.dimRoot.mkdirs();
    }

    private RailRegionStore openRegion(RailRegionKey key) throws IOException {
        RailRegionStore store = openRegions.get(key);
        if (store != null) return store;

        File file = new File(regionFolder, key.filename());
        store = new RailRegionStore(file);
        openRegions.put(key, store);

        if (openRegions.size() > MAX_OPEN) {
            Iterator<RailRegionStore> it = openRegions.values().iterator();
            RailRegionStore oldest = it.next();
            it.remove();
            oldest.close();
        }
        return store;
    }

    public void saveNode(RailNode node) throws IOException {
        RailRegionKey key = RailRegionKey.fromBlock(node.getStart());
        openRegion(key).writeNode(node);
    }

    public RailNode loadNode(int nodeId, Vec3i pos) throws IOException {
        RailRegionKey key = RailRegionKey.fromBlock(pos);
        return openRegion(key).readNode(nodeId);
    }

    public int loadNextIdOrDefault(int def) {
        if (!manifestFile.exists()) return def;
        try (RandomAccessFile raf = new RandomAccessFile(manifestFile, "r")) {
            if (raf.length() < 12) return def;
            int magic = raf.readInt();
            int ver = raf.readInt();
            int nextId = raf.readInt();
            if (magic != MANIFEST_MAGIC) return def;
            if (ver != MANIFEST_VERSION) return Math.max(def, nextId);
            return Math.max(def, nextId);
        } catch (IOException e) {
            return def;
        }
    }

    public void storeNextId(int nextId) throws IOException {
        File tmp = new File(dimRoot, "manifest.tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp); FileChannel ch = fos.getChannel()) {
            ByteBuffer buf = ByteBuffer.allocate(12);
            buf.putInt(MANIFEST_MAGIC);
            buf.putInt(MANIFEST_VERSION);
            buf.putInt(nextId);
            buf.flip();
            ch.write(buf);
            ch.force(true);
        }
        if (!tmp.renameTo(manifestFile)) {
            try (FileOutputStream fos = new FileOutputStream(manifestFile); FileChannel ch = fos.getChannel()) {
                ByteBuffer buf = ByteBuffer.allocate(12);
                buf.putInt(MANIFEST_MAGIC);
                buf.putInt(MANIFEST_VERSION);
                buf.putInt(nextId);
                buf.flip();
                ch.write(buf);
                ch.force(true);
            }
        }
    }
}
