package cam72cam.immersiverailroading.textUtil;

import cam72cam.mod.ModCore;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FontLoader {
    public static final Map<Identifier, Font> fonts = new HashMap<>();

    /**
     * Get or create a font from an Identifier
     * @param font Identifier that points to the png of the font
     * @return The font itself
     * @see Font
     */
    public static Font getOrCreateFont(Identifier font) {
        return fonts.computeIfAbsent(font, FontLoader::loadFont);
    }

    /**
     * Private method to load a new font from an identifier
     */
    private static Font loadFont(Identifier font) {
        Map<Character, Font.Glyph> glyphs = new HashMap<>();
        Identifier json = new Identifier(font.getDomain(), font.getPath().replaceAll(".png", ".json"));
        try {
            InputStream stream = json.getResourceStream();

            try (InputStreamReader reader = new InputStreamReader(stream)) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Font.Glyph>>() {
                }.getType();
                Map<String, Font.Glyph> loadedGlyphs = gson.fromJson(reader, type);
                for (Map.Entry<String, Font.Glyph> entry : loadedGlyphs.entrySet()) {
                    glyphs.put(entry.getKey().charAt(0), entry.getValue());
                }
            }

            int height = 0;
            int width = 0;
            try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(font.getResourceStream())) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(imageInputStream);
                    width = reader.getWidth(0);
                    height = reader.getHeight(0);
                    reader.dispose();
                } else {
                    ModCore.error("If you see this, something went wrong that shouldn't go wrong");
                }
            }

            Texture image = Texture.wrap(font);

            return new Font(height, width, image, glyphs);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
