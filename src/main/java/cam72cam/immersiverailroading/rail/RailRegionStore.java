package cam72cam.immersiverailroading.rail;

import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagSerializer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class RailRegionStore implements Closeable {
    // "IRTR"
    private static final int MAGIC = 0x4525352;
    private final File file;
    private final RandomAccessFile raf;
    private final FileChannel channel;
    private final Map<Integer, Long> index = new HashMap<>();

    public RailRegionStore(File file) throws IOException {
        this.file = file;
        this.file.getParentFile().mkdirs();
        this.raf = new RandomAccessFile(file, "rw");
        this.channel = raf.getChannel();
        scanFile();
    }

    private void scanFile() throws IOException {
        long pos = 0;
        while (pos < channel.size()) {
            ByteBuffer header = ByteBuffer.allocate(8);
            channel.read(header, pos);
            header.flip();

            int length = header.getInt();
            int nodeId = header.getInt();
            index.put(nodeId, pos);

            pos += 8 + length;
        }
    }

    public void writeNode(RailNode node) throws IOException {
        byte[] bytes = node.serialize().toBytes();

        ByteBuffer buf = ByteBuffer.allocate(8 + bytes.length);
        buf.putInt(bytes.length);
        buf.putInt(node.getId());
        buf.put(bytes);
        buf.flip();

        long offset = channel.size();
        channel.write(buf, offset);
        index.put(node.getId(), offset);
    }

    public RailNode readNode(int nodeId) throws IOException {
        Long offset = index.get(nodeId);
        if (offset == null) return null;

        ByteBuffer header = ByteBuffer.allocate(8);
        channel.read(header, offset);
        header.flip();

        int length = header.getInt();
        header.getInt();
        ByteBuffer data = ByteBuffer.allocate(length);
        channel.read(data, offset + 8);
        data.flip();

        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        return new RailNode(new TagCompound(bytes));
    }

    @Override
    public void close() throws IOException {
        // TODO: write index at end of file
        channel.close();
        raf.close();
    }
}
