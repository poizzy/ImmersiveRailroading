package cam72cam.immersiverailroading.textUtil;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.model.animation.StockAnimation;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.net.Packet;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.util.With;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TextField {
    private String text = "";
    private Supplier<String> textSupplier;
    private Font font;
    private Identifier lastFont = new Identifier("");
    private RGBA color = RGBA.fromHex("#ffffff");
    private boolean fullbright = false;
    private GroupInfo group;
    private VertexBuffer currentVBO = new VertexBuffer(0, false);
    private VertexBuffer lastVBO = new VertexBuffer(0, false);
    private VBO buffer;
    private Align align = Align.LEFT;
    private float radius = 0;
    private int gap = 1;
    private int offset = 0;
    private String object;

    // TODO factory method or constructor?
    public static TextField createTextField(Mesh.Group group, int resolutionX, int resolutionY) {
        TextField instance = new TextField();
        instance.group = GroupInfo.initGroup(group, resolutionX, resolutionY);
        instance.object = group.name;
        return instance;
    }

    @SuppressWarnings("unused")
    public static TextField createTextField(Mesh.Group group, int resolutionX, int resolutionY, Consumer<TextField> defaults) {
        TextField instance = new TextField();
        defaults.accept(instance);
        instance.group = GroupInfo.initGroup(group, resolutionX, resolutionY);
        instance.object = group.name;
        return instance;
    }

    private static TextField createTextField(GroupInfo group, String name, Consumer<TextField> defaults) {
        TextField instance = new TextField();
        defaults.accept(instance);
        instance.group = group;
        instance.object = name;
        return instance;
    }

    /**
     * Set the text of the given instance of TextField
     * @param text new text for the given text field
     * @return Instance of TextField
     */
    public TextField setText(String text) {
        this.textSupplier = null;
        if (!text.equals(this.text)) {
            this.text = text;
        }
        return this;
    }

    // TODO: add support for Supplier, Readouts?
    @SuppressWarnings("All")
    public TextField setTextSupplier(Supplier<String> supplier) {
        this.text = null;
        this.textSupplier = supplier;
        return this;
    }

    /**
     * Set the font for the given instance of TextField
     * @param font Identifier that points to the png of the font
     * @return Instance of TextField
     */
    public TextField setFont(Identifier font) {
        if (!font.equals(this.lastFont)) {
            this.font = FontLoader.getOrCreateFont(font);
            this.lastFont = font;
        }
        return this;
    }

    /**
     * Should the rendered text be fullbright?
     * @param b fullbright?
     * @return Instance of TextField
     */
    public TextField setFullBright(boolean b) {
        if (this.fullbright != b) {
            this.fullbright = b;
        }
        return this;
    }

    /**
     * Color of the rendered text
     * @param hex Hex color code as a string
     * @return Instance of TextField
     */
    public TextField setColor(String hex) {
        if (!hex.equals(this.color.toString())) {
            this.color = RGBA.fromHex(hex);
        }
        return this;
    }

    /**
     * Set the alignment of the rendered text
     * @param align "left", "center" or "right"
     * @return Instance of TextField
     */

    public TextField setAlign(String align) {
        align = align.toUpperCase();
        if (!align.equals(this.align.toString())) {
            this.align = Align.valueOf(align);
        }
        return this;
    }

    // Curved text fields?
    // TODO add possibility to set a radius for the text
    public TextField setRadius(float radius) {
        if (radius != this.radius) {
            this.radius = radius;
        }
        return this;
    }

    /**
     * Set the gap between the rendered characters
     * @param gap Gap in px
     * @return Instance of TextField
     */
    public TextField setGap(int gap) {
        if (gap != this.gap) {
            this.gap = gap;
        }
        return this;
    }

    /**
     * Set the offset between two lines of text
     * @param offset Offset in px
     * @return Instance of TextField
     */
    public TextField setOffset(int offset) {
        if (offset != this.offset) {
            this.offset = offset;
        }
        return this;
    }

    // TODO add supplier
    @SuppressWarnings("unused")
    private String getText() {
        return this.textSupplier != null ? this.textSupplier.get() : this.text;
    }


    /**
     * Method to render the current instance of TextField
     */
    public final void postRender(EntityRollingStock stock, RenderState state, List<StockAnimation> animations, float partialTicks) {
        if (this.font == null) {
            return;
        }

        Matrix4 anim = null;
        for (StockAnimation animation : animations) {
            anim = animation.getMatrix(stock, this.object, partialTicks);
            if (anim != null) break;
        }

        Matrix4 finalAnim = anim;

        if (!Arrays.equals(this.currentVBO.data, this.lastVBO.data) || buffer == null) {
            if (buffer != null) {
                buffer.free();
            }

            if (this.currentVBO.data.length < 1) {
                createVBO();
            }

            buffer = new VBO(() -> this.currentVBO, s -> {
                s.texture(this.font.texture).lightmap(this.fullbright ? 1 : 0, 1);
                if (finalAnim != null) {
                    s.model_view().multiply(finalAnim);
                }
            });
            this.lastVBO = this.currentVBO;
        }
        double scale = 1;

        // I will probably remove this in the future
        if (Config.ConfigDebug.renderDebugLines) {
            try (With ctx = RenderContext.apply(state)) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(2.0f);

                GL11.glBegin(GL11.GL_LINES);

                // Tangent - Red
                GL11.glColor4d(1, 0, 0, 1);
                GL11.glVertex3d(group.origin.x, group.origin.y, group.origin.z);
                Vec3d tEnd = group.origin.add(group.tangent.scale(scale));
                GL11.glVertex3d(tEnd.x, tEnd.y, tEnd.z);

                // Bitangent - Green
                GL11.glColor4d(0, 1, 0, 1);
                GL11.glVertex3d(group.origin.x, group.origin.y, group.origin.z);
                Vec3d bEnd = group.origin.add(group.bitangent.scale(scale));
                GL11.glVertex3d(bEnd.x, bEnd.y, bEnd.z);

                // Normal - Blue
                GL11.glColor4d(0, 0, 1, 1);
                GL11.glVertex3d(group.origin.x, group.origin.y, group.origin.z);
                Vec3d nEnd = group.origin.add(group.normal.scale(scale));
                GL11.glVertex3d(nEnd.x, nEnd.y, nEnd.z);

                GL11.glEnd();

                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
        }

        try (VBO.Binding binding = buffer.bind(state)) {
            binding.draw();
        }
    }

    @SuppressWarnings("All")
    private void createVBO() {
        if (this.font == null) {
            return;
        }

        List<float[]> vertexList = new ArrayList<>();

        String[] lines = text.split("\n");

        int[] lineWidths = new int[lines.length * 2];
        int[] lineHeights = new int[lines.length * 2];
        int totalVertexCount = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineWidth = 0;
            int maxHeight = 0;

            for (char c : line.toCharArray()) {
                int width = font.getCharWidthPx(c);
                int height = font.getCharHeightPx(c);

                lineWidth += width + gap;
                maxHeight = Math.max(maxHeight, height);
                totalVertexCount += 6;
            }

            lineWidth -= gap;

            lineWidths[i] = lineWidth;
            lineHeights[i] = maxHeight;
        }

        int totalTextHeight = 0;
        for (int i = 0; i < lines.length; i++) {
            totalTextHeight += lineHeights[i];
            if (i < lines.length - 1) {
                totalTextHeight += offset;
            }
        }

        Vec3d layoutTangent = group.tangent.normalize();
        Vec3d layoutBitangent = group.bitangent.normalize();

        Vec3d baseYOffset = group.bitangent.scale(((totalTextHeight / 2.0f) * group.pixelSizeY) / 2.0f);

        VertexBuffer vbo = new VertexBuffer(totalVertexCount / 3, true);
        float[] buffer = vbo.data;
        int stride = vbo.stride;

        Vec3d lineCursor = group.origin.add(baseYOffset);

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            int lineWidth = lineWidths[lineIndex];
            int lineHeight = lineHeights[lineIndex];

            float alignOffset;
            switch (align) {
                case CENTER:
                    alignOffset = 0.5f;
                    break;
                case RIGHT:
                    alignOffset = 1.0f;
                    break;
                case LEFT:
                default:
                    alignOffset = 0.0f;
            }

            Vec3d xOffset = layoutTangent.scale((group.resolutionX - lineWidth) * group.pixelSizeX * alignOffset);

            Vec3d cursor = lineCursor.add(xOffset);

            for (char c : line.toCharArray()) {
                double charWidth = font.getCharWidthPx(c) * group.pixelSizeX;
                double charHeight = font.getCharHeightPx(c) * group.pixelSizeY;

                Pair<Vec2f, Vec2f> uvs = font.getUV(c);

                Vec3d p0 = cursor;
                Vec3d p1 = cursor.add(layoutTangent.scale(charWidth));
                Vec3d p2 = p1.add(layoutBitangent.scale(charHeight));
                Vec3d p3 = cursor.add(layoutBitangent.scale(charHeight));

                float u0 = uvs.getLeft().x;
                float v0 = uvs.getLeft().y;
                float u1 = uvs.getRight().x;
                float v1 = uvs.getRight().y;

                float[][] verts = new float[][]{
                        {(float) p2.x, (float) p2.y, (float) p2.z, u1, v1},
                        {(float) p1.x, (float) p1.y, (float) p1.z, u1, v0},
                        {(float) p0.x, (float) p0.y, (float) p0.z, u0, v0},
                        {(float) p3.x, (float) p3.y, (float) p3.z, u0, v1},
                        {(float) p2.x, (float) p2.y, (float) p2.z, u1, v1},
                        {(float) p0.x, (float) p0.y, (float) p0.z, u0, v0}
                };

                Collections.addAll(vertexList, verts);
                cursor = cursor.add(layoutTangent.scale(charWidth + gap * group.pixelSizeX));
            }

            if (lineIndex < lines.length -1) {
                double advance = lineHeights[lineIndex] + offset * group.pixelSizeY;
                lineCursor = lineCursor.add(group.bitangent.scale(advance));
            }
        }

        int vi = 0;
        for (float[] v : vertexList) {
            int b = vi * stride;
            buffer[b + vbo.vertexOffset + 0] = v[0];
            buffer[b + vbo.vertexOffset + 1] = v[1];
            buffer[b + vbo.vertexOffset + 2] = v[2];
            buffer[b + vbo.textureOffset + 0] = v[3];
            buffer[b + vbo.textureOffset + 1] = v[4];
            buffer[b + vbo.colorOffset + 0] = this.color.r;
            buffer[b + vbo.colorOffset + 1] = this.color.g;
            buffer[b + vbo.colorOffset + 2] = this.color.b;
            buffer[b + vbo.colorOffset + 3] = this.color.a;
            buffer[b + vbo.normalOffset + 0] =  (float) this.group.normal.x;
            buffer[b + vbo.normalOffset + 1] =  (float) this.group.normal.y;
            buffer[b + vbo.normalOffset + 2] =  (float) this.group.normal.z;
            vi++;
        }

        this.currentVBO = vbo;
    }


    private static class RGBA {
        float r;
        float g;
        float b;
        final float a = 1.0f;

        static RGBA fromHex(String hex) {
            RGBA instance = new RGBA();
            instance.r = Integer.parseInt(hex.substring(1, 3), 16) / 255f;
            instance.g = Integer.parseInt(hex.substring(3, 5), 16) / 255f;
            instance.b = Integer.parseInt(hex.substring(5, 7), 16) / 255f;
            return instance;
        }

        @Override
        public String toString() {
            return String.format("#%02X%02X%02X", (int) (this.r * 255f), (int) (this.g * 255f), (int) (this.b * 255f));
        }
    }

    public static class GroupInfo {
        public Vec3d origin;
        public Vec3d tangent;
        public Vec3d bitangent;
        public Vec3d normal;
        double pixelSizeX;
        double pixelSizeY;
        int resolutionX;
        boolean flippedNormal = false;

        public GroupInfo() {
        }

        public GroupInfo(Vec3d origin, Vec3d tangent, Vec3d bitangent, Vec3d normal, double pixelSizeX, double pixelSizeY, int resolutionX, boolean flippedNormal) {
            this.origin = origin;
            this.tangent = tangent;
            this.bitangent = bitangent;
            this.normal = normal;
            this.pixelSizeX = pixelSizeX;
            this.pixelSizeY = pixelSizeY;
            this.resolutionX = resolutionX;
            this.flippedNormal = flippedNormal;
        }

        public static GroupInfo initGroup(Mesh.Group group, int resX, int resY) {
            GroupInfo info = new GroupInfo();
            Mesh.Face face = group.faces.get(0);

            List<Pair<Vec3d, Vec2f>> vertices = group.faces.stream()
                    .flatMap(f -> IntStream.range(0, f.vertices.size())
                            .mapToObj(i -> Pair.of(f.vertices.get(i), f.uv.get(i))))
                    .distinct()
                    .collect(Collectors.toList());


            Vec3d topLeft = vertices.stream()
                    .min(Comparator.comparingDouble((Pair<Vec3d, Vec2f> p ) -> p.getRight().x).thenComparingDouble(p -> p.getRight().y))
                    .orElseThrow(RuntimeException::new).getLeft();

            Vec2f topLeftUv = vertices.stream()
                    .map(Pair::getRight)
                    .min(Comparator.comparingDouble((Vec2f uv) -> uv.x)
                            .thenComparingDouble(uv -> uv.y))
                    .orElseThrow(RuntimeException::new);

            Vec3d topRight = vertices.stream()
                    .filter(p -> Math.abs(p.getRight().y - topLeftUv.y) < 1e-4)
                    .max(Comparator.comparingDouble(p -> p.getRight().x))
                    .orElseThrow(RuntimeException::new).getLeft();

            Vec3d bottomLeft = vertices.stream()
                    .filter(p -> Math.abs(p.getRight().x -  topLeftUv.x) < 1e-4)
                    .max(Comparator.comparingDouble(p -> p.getRight().y))
                    .orElseThrow(RuntimeException::new).getLeft();

            Vec3d tangent = topRight.subtract(topLeft);
            Vec3d bitangent = bottomLeft.subtract(topLeft);

            double fieldWidth3d = tangent.length();
            double fieldHeight3d = bitangent.length();

            info.tangent = tangent;
            info.bitangent = bitangent;
            info.pixelSizeX = fieldWidth3d / resX;
            info.pixelSizeY = fieldHeight3d / resY;
            info.resolutionX = resX;

            info.normal = face.normal;

            Vec3d windingNormal = VecUtil.crossProduct(info.tangent, info.bitangent).normalize();
            if (VecUtil.dotProduct(windingNormal, info.normal) < 0) {
                info.flippedNormal = true;
            } else {
                info.tangent = info.tangent.scale(-1);
            }

            info.origin = info.flippedNormal ? topLeft : topRight;

            return info;
        }
    }

    public enum Align {
        RIGHT,
        CENTER,
        LEFT
    }

    public static class TextFieldMapMapper implements TagMapper<Map<String, TextField>> {

        @Override
        public TagAccessor<Map<String, TextField>> apply(Class<Map<String, TextField>> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> d.setMap(fieldName, o, k -> k, v -> new TagCompound()
                                    .setString("font", v.lastFont.toString())
                            .setString("color", v.color.toString())
                            .setBoolean("fullbright", v.fullbright)
                            .setEnum("align", v.align)
                            .setInteger("gap", v.gap)
                            .setString("text", v.text)
                            .setString("name", v.object)
                            .set("group", new TagCompound()
                                    .setVec3d("origin", v.group.origin)
                                    .setVec3d("tangent", v.group.tangent)
                                    .setVec3d("bitangent", v.group.bitangent)
                                    .setVec3d("normal", v.group.normal)
                                    .setDouble("pixelSizeX", v.group.pixelSizeX)
                                    .setDouble("pixelSizeY", v.group.pixelSizeY)
                                    .setInteger("resX", v.group.resolutionX)
                                    .setBoolean("flippedNormal", v.group.flippedNormal))),
                    (d) -> d.getMap(fieldName, k -> k, v -> {
                        TagCompound g = v.get("group");
                                TextField.GroupInfo group = new TextField.GroupInfo(
                                        g.getVec3d("origin"),
                                        g.getVec3d("tangent"),
                                        g.getVec3d("bitangent"),
                                        g.getVec3d("normal"),
                                        g.getDouble("pixelSizeX"),
                                        g.getDouble("pixelSizeY"),
                                        g.getInteger("resX"),
                                        g.getBoolean("flippedNormal")
                                );
                                return TextField.createTextField(group, v.getString("name"), t -> t.setFont(new Identifier(v.getString("font")))
                                        .setColor(v.getString("color"))
                                        .setFullBright(v.getBoolean("fullbright"))
                                        .setAlign(v.getEnum("align", Align.class).toString())
                                        .setGap(v.getInteger("gap"))
                                        .setText(v.getString("text"))
                                );
                            }
                    )
            );
        }
    }

    public static class PacketSyncTextField extends Packet {
        @TagField
        private EntityRollingStock stock;
        @TagField(value = "textFields", mapper = TextFieldMapMapper.class)
        private Map<String, TextField> textFields;

        public PacketSyncTextField() {}

        public PacketSyncTextField(EntityRollingStock stock, Map<String, TextField> textFields) {
            this.stock = stock;
            this.textFields = textFields;
        }

        @Override
        protected void handle() {
            if (stock instanceof EntityScriptableRollingStock) {
                EntityScriptableRollingStock s = (EntityScriptableRollingStock) stock;
                if (!textFields.equals(s.textFields)) {
                    s.textFields.clear();
                    s.textFields.putAll(textFields);
                    s.textFields.forEach((n, t) -> t.createVBO());
                }
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TextField textField = (TextField) object;
        return fullbright == textField.fullbright && Float.compare(radius, textField.radius) == 0 && gap == textField.gap && Objects.equals(text, textField.text) && lastFont.equals(textField.lastFont) && color.toString().equals(textField.color.toString()) && align == textField.align;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, lastFont, color, fullbright, align, radius, gap);
    }
}
