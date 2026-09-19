package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.library.TextFieldConfig;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.opengl.VBO;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class TextBuilder {
    private final TextFieldConfig.Immutable config;
    private VBO vbo;

    public TextBuilder(TextFieldConfig.Immutable config) {
        this.config = config;
    }

    public VBO refresh(TextFieldConfig.Mutable mutableConfig) {
        if (mutableConfig.dirty || vbo == null) {
            build(mutableConfig);
        }
        return vbo;
    }

    public void build(TextFieldConfig.Mutable mutable) {
        String[] lines = mutable.text.split("\n");
    }

    public class VertexBuilder {
        private final VAOLayout layout = VAOLayout.POS_TEX_COLOR_NORMAL;
        private final FloatArrayList buffer = new FloatArrayList();
        private int index = 0;

        public void addChar(CharData data) {
            int stride = layout.getStride();
            int base = stride * index;
            // pos
            buffer.add(base + layout.getOffset(VAOLayout.Usage.POSITION), (float) data.pos().x);
            buffer.add(base + layout.getOffset(VAOLayout.Usage.POSITION) + 1, (float) data.pos().y);
            buffer.add(base+ layout.getOffset(VAOLayout.Usage.POSITION) + 2, (float) data.pos().z);
            // uv
            buffer.add(base + layout.getOffset(VAOLayout.Usage.UV), data.uv().x);
            buffer.add(base + layout.getOffset(VAOLayout.Usage.UV) + 1, data.uv().y);
            // color
            buffer.add(base + layout.getOffset(VAOLayout.Usage.COLOR), (float) data.color().r());
            buffer.add(base + layout.getOffset(VAOLayout.Usage.COLOR) + 1, (float) data.color().g());
            buffer.add(base + layout.getOffset(VAOLayout.Usage.COLOR) + 2, (float) data.color().b());
            buffer.add(base + layout.getOffset(VAOLayout.Usage.COLOR) + 3, (float) data.color().a());
            // normal
            buffer.add(base + layout.getOffset(VAOLayout.Usage.NORMAL), (float) data.normal().x);
            buffer.add(base + layout.getOffset(VAOLayout.Usage.NORMAL) + 1, (float) data.normal().y);
            buffer.add(base + layout.getOffset(VAOLayout.Usage.NORMAL) + 2, (float) data.normal().z);

            index++;
        }

        public VertexBuffer build() {
            return new VertexBuffer(buffer.elements(), true);
        }

    }

    public record CharData(Vec3d pos, Vec2f uv, Color color, Vec3d normal) {}
}
