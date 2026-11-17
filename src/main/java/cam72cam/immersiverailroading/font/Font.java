package cam72cam.immersiverailroading.font;

import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.plaf.synth.Region;
import java.util.Map;
import java.util.function.Supplier;

public class Font {
    public static class Glyph {
        int x;
        int y;
        int width;
        int height;
    }

    public Identifier key;
    public Map<Character, Glyph> glyphs;
    public int atlasHeight;
    public int atlasWidth;

    protected Font(Identifier key, Map<Character, Glyph> glyphs, int atlasHeight, int atlasWidth) {
        this.key = key;
        this.glyphs = glyphs;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
    }

    public int getCharWidthPx(char c) {
        Glyph g = glyphs.get(c);
        if (g == null) {
            g = glyphs.get(' ');
        }
        return g.width;
    }

    public int getCharHeightPx(char c) {
        Glyph g = glyphs.get(c);
        if (g == null) {
            g = glyphs.get(' ');
        }
        return g.height;
    }

    public Pair<Vec2f, Vec2f> getUV(char c) {
        Glyph g = glyphs.get(c);
        if (g == null) {
            g = glyphs.get(' ');
        }
        if (g == null) {
            g = glyphs.get(glyphs.keySet().iterator().next());
        }

        FontManager.Region region = FontManager.getRegion(key);

        int x = region.transformX(g.x);
        int y = region.transformY(g.y);

        float u0 = (float) x / atlasWidth;
        float u1 = (float) (x + g.width) / atlasWidth;

        float v0 = (float) y / atlasHeight;
        float v1 = (float) (y + g.height) / atlasHeight;

        return Pair.of(new Vec2f(u0, v0), new Vec2f(u1, v1));
    }
}
