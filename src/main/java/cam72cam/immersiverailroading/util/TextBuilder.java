package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.font.Font;
import cam72cam.immersiverailroading.font.FontManager;
import cam72cam.immersiverailroading.library.TextFieldConfig;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class TextBuilder {
    private final TextFieldConfig.Definition config;

    public TextBuilder(TextFieldConfig.Definition config) {
        this.config = config;
    }

    public VBO build(TextFieldConfig.State content) {
        Font font = FontManager.getFontOrDefault(content.font);
        Pair<List<TextLine>, Integer> lineInfo = computeLines(content.text, font, content.gap, content.offset);
        List<TextLine> lines = lineInfo.getLeft();
        int totalHeight = lineInfo.getRight();

        Vec3d xAxis = config.renderData.xAxis();
        Vec3d yAxis = config.renderData.yAxis();

        Vec3d xAxisN = xAxis.normalize();
        Vec3d yAxisN = yAxis.normalize();

        double pixelSizeX = config.renderData.pixelSizeX() * content.scale;
        double pixelSizeY = config.renderData.pixelSizeY() * content.scale;

        float verticalAlignMultpl = switch (content.verticalAlign) {
            case TOP -> 0.0F;
            case CENTER -> 0.5F;
            case BOTTOM -> 1.0F;
        };

        float alignMultpl = switch (content.align) {
            case RIGHT -> 1.0F;
            case CENTER -> 0.5F;
            case LEFT -> 0.0F;
        };

        double textfieldHeight = yAxis.length() / pixelSizeY;

        double verticalOffset = verticalAlignMultpl * (textfieldHeight - totalHeight);
        Vec3d baseYOffset = yAxisN.scale((verticalOffset * pixelSizeY));
        Vec3d lineCursor = config.renderData.origin().add(baseYOffset);

        VertexBuilder builder = new VertexBuilder();

        Color color = new Color(content.color);

        for (int i = 0, linesSize = lines.size(); i < linesSize; i++) {
            TextLine line = lines.get(i);
            Vec3d xOffset = xAxisN.scale((config.resolutionX * content.scale - line.width()) * pixelSizeX * alignMultpl);
            Vec3d cursor = lineCursor.add(xOffset);
            Color currentColor = color;
            int charIndex = 0;

            while (charIndex < line.content().length()) {
                char c = line.content().charAt(charIndex);

                if (c == '&' && charIndex + 1 < line.content().length()) {
                    char code = line.content.charAt(charIndex + 1);
                    currentColor = Color.fromMinecraftCode(code);
                    charIndex += 2;
                    continue;
                }

                double charWidth = font.getCharWidthPx(c) * pixelSizeX;
                double charHeight = font.getCharHeightPx(c) * pixelSizeY;

                Pair<Vec2f, Vec2f> uvs = font.getUV(c);

                Vec3d p0 = cursor;
                Vec3d p1 = cursor.add(xAxisN.scale(charWidth));
                Vec3d p2 = p1.add(yAxisN.scale(charHeight));
                Vec3d p3 = cursor.add(yAxisN.scale(charHeight));

                float u0 = uvs.getLeft().x;
                float v0 = uvs.getLeft().y;
                float u1 = uvs.getRight().x;
                float v1 = uvs.getRight().y;

                builder.addVertex(p2, u1, v1, currentColor);
                builder.addVertex(p1, u1, v0, currentColor);
                builder.addVertex(p0, u0, v0, currentColor);

                builder.addVertex(p3, u0, v1, currentColor);
                builder.addVertex(p2, u1, v1, currentColor);
                builder.addVertex(p0, u0, v0, currentColor);

                cursor = cursor.add(xAxisN.scale(charWidth + content.gap * pixelSizeX));
                charIndex++;
            }

            if (i < lines.size() -1) {
                double advance = (line.height + content.offset) * pixelSizeY;
                lineCursor = lineCursor.add(yAxisN.scale(advance));
            }
        }

        VertexBuffer buffer = builder.build(config.renderData.normal());

        return new VBO(() -> buffer, state -> state.lightmap(config.fullbright ? 1 : 0, 1));
    }

    private Pair<List<TextLine>, Integer> computeLines(String text, Font font, int gap, int offset) {
        List<TextLine> lines = new ArrayList<>();
        int totalHeight = 0;
        for (String line : text.split("\n")) {
            int lineWidth = 0;
            int lineHeight = 0;

            int index = 0;

            while (index < line.length()) {
                char c = line.charAt(index);
                if (c == '&') {
                    index += 2;
                    continue;
                }

                lineWidth += font.getCharWidthPx(c) + gap;
                lineHeight = Math.max(lineHeight, font.getCharHeightPx(c));
                index++;
            }

            totalHeight += lineHeight + offset;

            lines.add(new TextLine(line, lineWidth - gap, lineHeight));
        }

        totalHeight -= offset;
        return Pair.of(lines, totalHeight);
    }

    private record TextLine(String content, int width, int height) {}

    public static class VertexBuilder {
        private static final Identifier LOCATION = new Identifier("TEXTFIELD_BUILDER");
        private static final VAOLayout layout = VAOLayout.POS_TEX_COLOR_NORMAL;
        // (x, y, z, u, v, r, g, b, a)
        private final List<float[]> vertices = new ArrayList<>();

        public void addVertex(Vec3d pos, float u, float v, Color color) {
            float[] array = {
                    (float) pos.x, (float) pos.y, (float) pos.z,
                    u, v,
                    (float) color.r(), (float) color.g(), (float) color.b(), (float) color.a()
            };
            vertices.add(array);
        }

        public VertexBuffer build(Vec3d normal) {
            int strideF = layout.getStride();
            int triCount = vertices.size();
            float[] data = new float[triCount * strideF];

            int posOff = layout.getOffset(VAOLayout.Usage.POSITION);
            int uvOff = layout.getOffset(VAOLayout.Usage.UV);
            int colorOff = layout.getOffset(VAOLayout.Usage.COLOR);
            int normalOff = layout.getOffset(VAOLayout.Usage.NORMAL);

            for (int i = 0; i < triCount; i++) {
                float[] vertex = vertices.get(i);

                int base = i * strideF;

                if (posOff != -1) {
                    data[base + posOff] = vertex[0];
                    data[base + posOff + 1] = vertex[1];
                    data[base + posOff + 2] = vertex[2];
                }

                if (uvOff != -1) {
                    data[base + uvOff] = vertex[3];
                    data[base + uvOff + 1] = vertex[4];
                }

                if (colorOff != -1) {
                    data[base + colorOff] = vertex[5];
                    data[base + colorOff + 1] = vertex[6];
                    data[base + colorOff + 2] = vertex[7];
                    data[base + colorOff + 3] = vertex[8];
                }

                if (normalOff != -1) {
                    data[base + normalOff] = (float) normal.x;
                    data[base + normalOff + 1] = (float) normal.y;
                    data[base + normalOff + 2] = (float) normal.z;
                }
            }

            return new VertexBuffer(data, true);
        }
    }
}
