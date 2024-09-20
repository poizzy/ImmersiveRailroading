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

    public void drawText(DirectDraw draw, String text, Vec3d pos, RenderState state) {
        double currentX = pos.x;
        double currentY = pos.y;
        double currentZ = pos.z;

        for (char c : text.toCharArray()) {
            Glyph glyph = getGlyphForChar(c);
            if (glyph == null) continue;

            double u = (double) glyph.x / textureWidth;
            double v = (double) glyph.y / textureHeight;
            double u1 = (double) (glyph.x + glyph.width) / textureWidth;
            double v1 = (double) (glyph.y + glyphHeight) / textureHeight;

            v = 1.0 - v;
            v1 = 1.0 -v1;

            draw.vertex(currentX, currentY, currentZ).uv(u, v);  // Bottom-left
            draw.vertex(currentX + glyph.width, currentY, currentZ).uv(u1, v);  // Bottom-right
            draw.vertex(currentX + glyph.width, currentY + glyphHeight, currentZ).uv(u1, v1);  // Top-right
            draw.vertex(currentX, currentY + glyphHeight, currentZ).uv(u, v1);  // Top-left

            currentX += glyph.width + gap;  // Move by glyph width + gap
        }
    }

}
