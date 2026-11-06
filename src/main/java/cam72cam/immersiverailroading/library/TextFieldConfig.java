package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.font.FontManager;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.Face;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.util.FaceAccessor;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class TextFieldConfig {
    public final String name;
    public final Supplier<Mutable> mutable;
    public final Immutable immutable;

    private TextFieldConfig(String name, Supplier<Mutable> mutable, Immutable immutable) {
        this.name = name;
        this.mutable = mutable;
        this.immutable = immutable;
    }

    public static TextFieldConfig fromDataBlock(DataBlock data) {
        String object = data.getValue("name").asString();
        if (object == null) {
            throw new RuntimeException("An Error occurred during text field init: Missing name");
        }

        Immutable immutable = initImmutable(data, object);
        Supplier<Mutable> mutable = initMutable(data);

        return new TextFieldConfig(object, mutable, immutable);
    }

    private static Immutable initImmutable(DataBlock data, String object) {
        DataBlock resolution = data.getBlock("resolution");
        int resX = resolution.getValue("x").asInteger(128);
        int resY = resolution.getValue("y").asInteger(9);

        DataBlock config = data.getBlock("config");
        List<String> linked = new ArrayList<>();
        if (config.getValues("linked") != null) {
            config.getValues("linked").forEach(l -> linked.add(l.asString()));
        }

        List<String> filter = new ArrayList<>();
        if (config.getValues("filter") != null) {
            config.getValues("filter").forEach(f -> filter.add(f.asString()));
        }

        boolean fullbright = config.getValue("fullbright").asBoolean(false);
        boolean global = config.getValue("global").asBoolean(false);
        boolean selectable = config.getValue("selectable").asBoolean(true);
        boolean unique = config.getValue("unique").asBoolean(false);

        List<Identifier> availableFonts = new ArrayList<>();
        if (config.getValues("availableFonts") != null) {
            config.getValues("availableFonts").forEach(f -> availableFonts.add(f.asIdentifier()));
        }

        return new Immutable(object, resX, resY, linked, filter, fullbright, global, selectable, unique, availableFonts);
    }

    private static Supplier<Mutable> initMutable(DataBlock data) {
        DataBlock config = data.getBlock("config");

        String color = config.getValue("color").asString("#000000");
        String text = config.getValue("text").asString("");
        Align align = Align.valueOf(config.getValue("align").asString("left").toUpperCase());
        VerticalAlign verticalAlign = VerticalAlign.valueOf(config.getValue("verticalAlign").asString("center").toUpperCase());
        Identifier font = config.getValue("font").asIdentifier(FontManager.DEFAULT);
        int gap = config.getValue("gap").asInteger(1);
        int offset = config.getValue("offset").asInteger(0);
        float scale = config.getValue("scale").asFloat(1);

        return () -> new Mutable(color, text, align, verticalAlign, font, gap, offset, scale);
    }

    public static class Mutable {
        @TagField
        public String color;
        @TagField
        public String text;
        @TagField(typeHint = Align.class)
        public Align align;
        @TagField(typeHint = VerticalAlign.class)
        public VerticalAlign verticalAlign;
        @TagField
        public Identifier font;
        @TagField
        public int gap;
        @TagField
        public int offset;
        @TagField
        public float scale;

        public boolean dirty = false;

        public Mutable(String color, String text, Align align, VerticalAlign verticalAlign, Identifier font, int gap, int offset, float scale) {
            this.color = color;
            this.text = text;
            this.align = align;
            this.verticalAlign = verticalAlign;
            this.font = font;
            this.gap = gap;
            this.offset = offset;
            this.scale = scale;
        }

        public Mutable setColor(String color) {
            this.color = color;
            this.dirty = true;
            return this;
        }

        public Mutable setText(String text) {
            this.text = text;
            this.dirty = true;
            return this;
        }

        public Mutable setAlign(Align align) {
            this.align = align;
            this.dirty = true;
            return this;
        }

        public Mutable setVerticalAlign(VerticalAlign verticalAlign) {
            this.verticalAlign = verticalAlign;
            this.dirty = true;
            return this;
        }

        public Mutable setFont(Identifier font) {
            this.font = font;
            this.dirty = true;
            return this;
        }

        public Mutable setGap(int gap) {
            this.gap = gap;
            this.dirty = true;
            return this;
        }

        public Mutable setOffset(int offset) {
            this.offset = offset;
            this.dirty = true;
            return this;
        }

        public Mutable setScale(float scale) {
            this.scale = scale;
            this.dirty = true;
            return this;
        }

        public Mutable setDirty(boolean dirty) {
            this.dirty = dirty;
            return this;
        }
    }

    public static final class Immutable {
        public final String object;
        private final int resolutionX;
        private final int resolutionY;

        public final List<String> linked;
        public final List<String> filter;
        public final boolean fullbright;
        public final boolean global;
        public final boolean selectable;
        public final boolean unique;
        public final List<Identifier> availableFonts;

        public RenderData renderData;

        public Immutable(String object, int resolutionX, int resolutionY, List<String> linked, List<String> filter,
                         boolean fullbright, boolean global, boolean selectable, boolean unique,
                         List<Identifier> availableFonts) {
            this.object = object;
            this.resolutionX = resolutionX;
            this.resolutionY = resolutionY;
            this.linked = Collections.unmodifiableList(linked);
            this.filter = Collections.unmodifiableList(filter);
            this.fullbright = fullbright;
            this.global = global;
            this.selectable = selectable;
            this.unique = unique;
            this.availableFonts = Collections.unmodifiableList(availableFonts);
        }

        public void initRenderData(String key, Model model) {
            FaceAccessor accessor = model.getFaceAccessor().ofGroup(key);
            List<Face> faces = accessor.stream().map(FaceAccessor::snapFace).toList();
            List<Face.Vertex> vertices = faces.stream().flatMap(f -> Stream.of(f.vertex0, f.vertex1, f.vertex2)).toList();

            Face.Vertex topLeft = vertices.stream().min(Comparator.<Face.Vertex>comparingDouble(v -> v.uv.x).thenComparingDouble(v -> v.uv.y)).get();
            Face.Vertex topRight = vertices.stream().filter(v -> Math.abs(v.uv.y - topLeft.uv.y) < 1e-4).max(Comparator.comparingDouble(v -> v.uv.x)).get();
            Face.Vertex bottomLeft = vertices.stream().filter(v -> Math.abs(v.uv.x - topLeft.uv.x) < 1e-4).max(Comparator.comparingDouble(v -> v.uv.y)).get();

            Vec3d xAxis = topRight.pos.subtract(topLeft.pos);
            Vec3d yAxis = bottomLeft.pos.subtract(topLeft.pos);

            double width = xAxis.length();
            double height = yAxis.length();

            double pixelSizeX = width / resolutionX;
            double pixelSizeY = height / resolutionY;

            Vec3d normal = faces.getFirst().normal;

            Vec3d windingNormal = xAxis.crossProduct(yAxis).normalize();
            boolean flippedNormal = windingNormal.dotProduct(normal) < 0;
            if (!flippedNormal) {
                xAxis = xAxis.scale(-1);
            }

            Vec3d origin = flippedNormal ? topLeft.pos : topRight.pos;

            this.renderData = new RenderData(origin, xAxis, yAxis, normal, pixelSizeX, pixelSizeY, flippedNormal);
        }

        public record RenderData(Vec3d origin, Vec3d xAxis, Vec3d yAxis, Vec3d normal, double pixelSizeX, double pixelSizeY, boolean flippedNormal) {}
    }

    public enum Align {
        RIGHT,
        CENTER,
        LEFT;

        private static final Align[] vals = values();

        public Align next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    public enum VerticalAlign {
        TOP,
        CENTER,
        BOTTOM;

        private static final VerticalAlign[] vals = values();

        public VerticalAlign next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }
}
