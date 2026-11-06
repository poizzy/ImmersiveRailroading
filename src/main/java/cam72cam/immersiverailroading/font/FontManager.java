package cam72cam.immersiverailroading.font;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.model.common.util.ImageUtils;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.ResourceCache;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FontManager {
    private static final int MAX_ATLAS_WIDTH = 8192;
    public static final Identifier DEFAULT = new Identifier("minecraft", "textures/font/ascii.png");

    private static final Map<Identifier, Font> availableFonts;
    private static final Map<Identifier, Region> regions;
    private static final List<FontLocation> fontsToLoad;

    public static Texture TEXTURE;

    public record FontLocation(Identifier image, Identifier json) {}
    public record Region(Identifier name, int x, int y, int width, int height) {
        public int transformX(int inX) {
            return x() + inX;
        }
        public int transformY(int inY) {
            return y() + inY;
        }
    }

    static {
        availableFonts = new HashMap<>();
        regions = new HashMap<>();
        fontsToLoad = new ArrayList<>();
        addFont(new FontLocation(DEFAULT, new Identifier(ImmersiveRailroading.MODID, "textures/font/ascii.json")));
    }

    public static void addFont(Identifier font) {
        Identifier json = new Identifier(font.getDomain(), font.getPath().replace(".png", ".json"));
        addFont(new FontLocation(font, json));
    }

    public static void addFont(FontLocation location) {
        fontsToLoad.add(location);
    }

    public static Font getFontOrDefault(Identifier png) {
        if (png.equals(DEFAULT) || !availableFonts.containsKey(png)) {
            return availableFonts.get(DEFAULT);
        }
        return availableFonts.get(png);
    }

    protected static Region getRegion(Identifier key) {
        return regions.get(key);
    }

    public static void initializeFonts() {
        record Sprite(Identifier loc, BufferedImage img) {}
        List<Sprite> sprites = new ArrayList<>();

        for (FontLocation loc : fontsToLoad) {
            try (InputStream in = loc.image().getResourceStream()) {
                BufferedImage image = ImageIO.read(in);
                if (image == null) {
                    throw new IOException("Unsupported image format: " + loc);
                }
                sprites.add(new Sprite(loc.image(), image));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        sprites.sort((a, b) -> Integer.compare(b.img().getHeight(), a.img().getHeight()));
        final int padding = 5;
        int x = padding;
        int y = padding;
        int rowHeight = 0;

        int atlasWidth = 0;

        for (Sprite sprite : sprites) {
            int width = sprite.img().getWidth();
            int height = sprite.img().getHeight();

            atlasWidth += width;

            if (width + 2 * padding > MAX_ATLAS_WIDTH) {
                throw new IllegalArgumentException(sprite.loc.getPath() + " is wider than the texture atlas! Max width: " + MAX_ATLAS_WIDTH);
            }

            if (x + width + padding > MAX_ATLAS_WIDTH) {
                x = padding;
                y += rowHeight + padding;
                rowHeight = 0;
            }

            regions.put(sprite.loc(), new Region(sprite.loc(), x, y, width, height));
            x += width + padding;
            rowHeight = Math.max(rowHeight, height);
        }

        atlasWidth = Math.min(atlasWidth, MAX_ATLAS_WIDTH);
        int atlasHeight = y + rowHeight + padding;

        BufferedImage atlas = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = atlas.createGraphics();
        for (Sprite sprite : sprites) {
            Region region = regions.get(sprite.loc());
            graphics2D.drawImage(sprite.img(), region.x(), region.y(), null);
        }
        graphics2D.dispose();

        Supplier<ResourceCache.GenericByteBuffer> getBuffer = () -> new ResourceCache.GenericByteBuffer(ImageUtils.toRGBA(atlas));

        TEXTURE = new OBJTextureSheet(atlasWidth, atlasHeight, getBuffer, 0);

        loadFonts(atlasHeight, atlasWidth);
    }

    private static void loadFonts(int atlasHeight, int atlasWidth) {
        for (FontLocation fonts : fontsToLoad) {
            Identifier json = fonts.json();
            Map<Character, Font.Glyph> loadedGlyphs;
            try (InputStream in = json.getResourceStream()) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Font.Glyph>>() {}.getType();
                InputStreamReader reader = new InputStreamReader(in);
                loadedGlyphs = ((Map<String, Font.Glyph>) gson.fromJson(reader, type)).entrySet().stream().collect(Collectors.toMap(k -> k.getKey().charAt(0), Map.Entry::getValue));
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Identifier key = fonts.image();
            Font font = new Font(key, loadedGlyphs, atlasHeight, atlasWidth);
            availableFonts.put(key, font);
        }
    }
}
