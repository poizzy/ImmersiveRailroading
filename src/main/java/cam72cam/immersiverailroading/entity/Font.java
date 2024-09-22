package cam72cam.immersiverailroading.entity;

import cam72cam.mod.math.Vec3d;
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

    private double textureHeight;

    private static class Glyph {
        double x, y;
        int width;
    }

    private final int textureId;
    private final int glyphHeight;
    private final int textureWidth;
    private final int gap;
    private final Map<Character, Glyph> glyphs = new HashMap<>();

    public Font(int textureId, int textureWidth, int textureHeight, int gap, InputStream jsonInputStream) {
        this.textureId = textureId;
        this.glyphHeight = textureHeight;
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

            // Convert loadedGlyphs (which uses String keys) to Character keys
            for (Map.Entry<String, Glyph> entry : loadedGlyphs.entrySet()) {
                glyphs.put(entry.getKey().charAt(0), entry.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Glyph getGlyphForChar(char c) {
        return glyphs.getOrDefault(c, glyphs.get(' '));
    }

    public enum TextAlign {
        LEFT, CENTER, RIGHT
    }

    public void drawText(DirectDraw draw, String text, Vec3d bottomLeft, Vec3d topRight, RenderState state, int resolutionWidth, int resolutionHeight, boolean flipDirection, TextAlign alignment) {
        Vec3d directionX = new Vec3d(topRight.x - bottomLeft.x, 0, topRight.z - bottomLeft.z).normalize();  // X-Z direction vector
        Vec3d directionY = new Vec3d(0, topRight.y - bottomLeft.y, 0).normalize();  // Y direction for height

        boolean isZAligned = Math.abs(directionX.x) < 1e-6;

        double boxHeight = topRight.y - bottomLeft.y;
        double boxWidth = directionX.length();

        double scaleX = boxWidth / resolutionWidth;
        double scaleY = boxHeight / resolutionHeight;

        double totalTextWidth = 0;
        for (char c : text.toCharArray()) {
            Glyph glyph = getGlyphForChar(c);
            if (glyph != null) {
                totalTextWidth += glyph.width * scaleX;
            }
        }

        double totalTextHeight = glyphHeight * scaleY;

        double yOffset;
        if (flipDirection) {
            yOffset = -(boxHeight / 2 + totalTextHeight / 2);
        } else {
            yOffset = boxHeight / 2 - totalTextHeight / 2;
        }

        Vec3d startPos;
        if (flipDirection) {
            directionX = directionX.scale(-1);

            switch (alignment) {
                case RIGHT:
                    startPos = bottomLeft.add(directionX.scale(-totalTextWidth)).add(0, -yOffset, 0);
                    break;
                case CENTER:
                    startPos = topRight.subtract(directionX.scale((boxWidth + totalTextWidth) / 4)).add(0, yOffset, 0);
                    break;
                case LEFT:
                default:
                    startPos = topRight.add(0, yOffset, 0);
                    break;
            }
        } else {
            switch (alignment) {
                case RIGHT:
                    startPos = bottomLeft.add(directionX.scale(boxWidth - totalTextWidth)).add(0, yOffset, 0);
                    break;
                case CENTER:
                    startPos = bottomLeft.add(directionX.scale((boxWidth - totalTextWidth) / 2)).add(0, yOffset, 0);
                    break;
                case LEFT:
                default:
                    startPos = bottomLeft.add(0, yOffset, 0);
                    break;
            }
        }

        Vec3d currentPos = startPos;

        for (char c : text.toCharArray()) {
            Glyph glyph = getGlyphForChar(c);
            if (glyph == null) continue;

            double u = (double) glyph.x / textureWidth;
            double v = (double) glyph.y / textureHeight;
            double u1 = (double) (glyph.x + glyph.width) / textureWidth;
            double v1 = (double) (glyph.y + glyphHeight) / textureHeight;

            v = 1.0 - v;
            v1 = 1.0 - v1;

            double scaledGlyphWidth = glyph.width * scaleX;
            double scaledGlyphHeight = glyphHeight * scaleY;

            Vec3d bottomLeftPos = currentPos;
            Vec3d bottomRightPos = currentPos.add(directionX.scale(scaledGlyphWidth));
            Vec3d topRightPos = bottomRightPos.add(0, scaledGlyphHeight, 0);
            Vec3d topLeftPos = bottomLeftPos.add(0, scaledGlyphHeight, 0);

            draw.vertex(bottomLeftPos.x, bottomLeftPos.y, bottomLeftPos.z).uv(u, v);
            draw.vertex(bottomRightPos.x, bottomRightPos.y, bottomRightPos.z).uv(u1, v);
            draw.vertex(topRightPos.x, topRightPos.y, topRightPos.z).uv(u1, v1);
            draw.vertex(topLeftPos.x, topLeftPos.y, topLeftPos.z).uv(u, v1);

            if (isZAligned) {
                currentPos = currentPos.add(directionX.scale(scaledGlyphWidth + gap * scaleX));
            } else {
                currentPos = currentPos.add(directionX.scale(scaledGlyphWidth + gap * scaleX));
            }
        }
    }
}
