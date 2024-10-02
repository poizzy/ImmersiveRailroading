package cam72cam.immersiverailroading.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.Buffers;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Font {

    private final double textureHeight;

    private Vec3d crossProduct(Vec3d vec1, Vec3d vec2) {
        double crossX = vec1.y * vec2.z - vec1.z * vec2.y;
        double crossY = vec1.z * vec2.x - vec1.x * vec2.z;
        double crossZ = vec1.x * vec2.y - vec1.y * vec2.x;
        return new Vec3d(crossX, crossY, crossZ);
    }

    private double dot(Vec3d vec1, Vec3d vec2) {
        return vec1.x * vec2.x + vec1.y * vec2.y + vec1.z * vec2.z;
    }

    private double getDistance2D(double x1, double z1, double x2, double z2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2));
    }

    private static class Glyph {
        double x, y;
        int width;
    }

    private final int textureId;
    private final int glyphHeight;
    private final int textureWidth;
    private final int gap;
    private final Map<Character, Glyph> glyphs = new HashMap<>();

    public Font(int textureId, int textureWidth, int glyphHeight, int gap, InputStream jsonInputStream, int textureHeight) {
        this.textureId = textureId;
        this.glyphHeight = glyphHeight;
        this.textureHeight = textureHeight;
        this.textureWidth = textureWidth;
        this.gap = gap;

        loadGlyphData(jsonInputStream);
    }

    private void loadGlyphData(InputStream jsonInputStream) {
        try (InputStreamReader reader = new InputStreamReader(jsonInputStream)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Glyph>>() {}.getType();
            Map<String, Glyph> loadedGlyphs = gson.fromJson(reader, type);

            for (Map.Entry<String, Glyph> entry : loadedGlyphs.entrySet()) {
                glyphs.put(entry.getKey().charAt(0), entry.getValue());
            }
        } catch (Exception e) {
            ModCore.error(String.format("An error occured while loading font %s : %s", jsonInputStream, e));
        }
    }

    private Glyph getGlyphForChar(char c) {
        return glyphs.getOrDefault(c, glyphs.get(' '));
    }

    public enum TextAlign {
        LEFT, CENTER, RIGHT
    }

    public void drawText(DirectDraw draw, String text, Vec3d minVector, Vec3d maxVector, RenderState state, int resolutionWidth, int resolutionHeight, TextAlign alignment, Vec3d normal, boolean switchPlusMinus, String fontColor, boolean useAlternative, int fontGap) {
        normal = normal.normalize();
        Vec3d up = new Vec3d(0, 1, 0);

        if (Math.abs(dot(normal, up)) > 0.999) {
            up = new Vec3d(1, 0, 0);
        }

        Vec3d directionX = crossProduct(normal, up).normalize();

        if (directionX.lengthSquared() < 1e-8) {
            up = new Vec3d(1, 0, 0);
            directionX = crossProduct(normal, up).normalize();
        }
        Vec3d directionY = crossProduct(directionX, normal).normalize();

        double boxHeight = maxVector.y - minVector.y;
        double boxWidth = directionX.length();
        double boxWidth2 = useAlternative ? boxWidth : getDistance2D(maxVector.x, maxVector.z, minVector.x, minVector.z);

        double scaleX = boxWidth / resolutionWidth;
        double scaleY = boxHeight / resolutionHeight;

        double totalTextWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Glyph glyph = getGlyphForChar(c);
            if (glyph != null) {
                totalTextWidth += (glyph.width + gap) * scaleX;
            }
        }
        if (totalTextWidth > 0) {
            totalTextWidth -= gap * scaleX;
        }

        double glyphHeightScaled = glyphHeight * scaleY;
        double totalTextHeight = glyphHeightScaled;
        double verticalOffset = (boxHeight - totalTextHeight) / 2;
        double yOffset = (boxHeight + totalTextHeight) / 2;

        Vec3d startPos;
        double horizontalOffset;
        switch (alignment) {
            case RIGHT:
                horizontalOffset = boxWidth2 - totalTextWidth;
                break;
            case CENTER:
                horizontalOffset = (boxWidth2 - totalTextWidth) / 2;
                break;
            case LEFT:
            default:
                horizontalOffset = 0;
                break;
        }

        if (!switchPlusMinus) {
            startPos = minVector
                    .add(directionX.scale(horizontalOffset))
                    .add(directionY.scale(verticalOffset));
        } else {
            startPos = maxVector
                    .add(directionX.scale(horizontalOffset))
                    .subtract(directionY.scale(yOffset));
        }

        Vec3d currentPos = startPos;

        Buffers.FloatBuffer vertexBuffer = new Buffers.FloatBuffer(text.length() * 12 * 4);

        for (char c : text.toCharArray()) {
            Glyph glyph = getGlyphForChar(c);
            if (glyph == null) continue;

            double u = glyph.x / textureWidth;
            double u1 = (glyph.x + glyph.width) / textureWidth;
            double v = (glyph.y + glyphHeight) / textureHeight;
            double v1 = glyph.y / textureHeight;

            double scaledGlyphWidth = glyph.width * scaleX;
            double scaledGlyphHeight = glyphHeightScaled;

            Vec3d bottomLeftPos = currentPos;
            Vec3d bottomRightPos = currentPos.add(directionX.scale(scaledGlyphWidth));
            Vec3d topRightPos = bottomRightPos.add(directionY.scale(scaledGlyphHeight));
            Vec3d topLeftPos = bottomLeftPos.add(directionY.scale(scaledGlyphHeight));

            addVertexToBuffer(vertexBuffer, bottomLeftPos, u, v);
            addVertexToBuffer(vertexBuffer, bottomRightPos, u1, v);
            addVertexToBuffer(vertexBuffer, topRightPos, u1, v1);
            addVertexToBuffer(vertexBuffer, topLeftPos, u, v1);

            currentPos = currentPos.add(directionX.scale(scaledGlyphWidth + gap * scaleX));
        }

        float[] vertexData = vertexBuffer.array();
        int stride = 5;

        for (int i = 0; i < vertexData.length; i += stride) {
            draw.vertex(
                    vertexData[i],     // x
                    vertexData[i + 1], // y
                    vertexData[i + 2]  // z
            ).uv(
                    vertexData[i + 3], // u
                    vertexData[i + 4]  // v
            );
        }
    }

    private void addVertexToBuffer(Buffers.FloatBuffer buffer, Vec3d pos, double u, double v) {
        buffer.add((float) pos.x);
        buffer.add((float) pos.y);
        buffer.add((float) pos.z);
        buffer.add((float) u);
        buffer.add((float) v);
    }
}
