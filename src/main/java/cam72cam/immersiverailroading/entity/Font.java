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

    private Vec3d crossProduct(Vec3d vec1, Vec3d vec2) {
        double crossX = vec1.y * vec2.z - vec1.z * vec2.y;
        double crossY = vec1.z * vec2.x - vec1.x * vec2.z;
        double crossZ = vec1.x * vec2.y - vec1.y * vec2.x;
        return new Vec3d(crossX, crossY, crossZ);
    }

    private double dot(Vec3d vec1, Vec3d vec2) {
        return vec1.x * vec2.x + vec1.y * vec2.y + vec1.z * vec2.z;
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

    public void drawText(DirectDraw draw, String text, Vec3d minVector, Vec3d maxVector, RenderState state, int resolutionWidth, int resolutionHeight, TextAlign alignment, Vec3d normal, boolean switchPlusMinus, String fontColor) {
        // Normalize the normal vector for plane alignment
        normal = normal.normalize();

        // Determine the direction of the text on the plane
        Vec3d up = new Vec3d(0, 1, 0);

        // Avoid using the up vector if it's too close to the normal direction
        if (Math.abs(dot(normal, up)) > 0.999) {
            up = new Vec3d(1, 0, 0);  // Adjust 'up' to avoid being parallel to 'normal'
        }

        // Calculate the X and Y axes relative to the plane defined by the normal
        Vec3d directionX = crossProduct(normal, up).normalize();  // Horizontal text direction


        if (directionX.lengthSquared() < 1e-8) {
            up = new Vec3d(1, 0, 0); // In extreme cases, use x-axis as up
            directionX = crossProduct(normal, up).normalize();
        }
        Vec3d directionY = crossProduct(directionX, normal).normalize();

        // Calculate box dimensions using min and max vectors
        double boxHeight = maxVector.y - minVector.y;
        double boxWidth = directionX.length();

        // Calculate scale factors for text fitting into the box
        double scaleX = boxWidth / resolutionWidth;
        double scaleY = boxHeight / resolutionHeight;

        // Calculate the total text width based on glyph widths
        double totalTextWidth = 0;
        for (char c : text.toCharArray()) {
            Glyph glyph = getGlyphForChar(c);
            if (glyph != null) {
                totalTextWidth += glyph.width * scaleX;
            }
        }

        double glyphHeightScaled = glyphHeight * scaleY;  // Scaled height for glyphs
        double totalTextHeight = glyphHeightScaled;

        // Center the text vertically within the box
        double verticalOffset = (boxHeight - totalTextHeight) / 2;
        double yOffset = (boxHeight + totalTextHeight) / 2;

        // Determine starting position based on alignment
        Vec3d startPos;
        double horizontalOffset;
        switch (alignment) {
            case RIGHT:
                horizontalOffset = boxWidth - totalTextWidth;  // Right-align text
                break;
            case CENTER:
                horizontalOffset = (boxWidth - totalTextWidth) / 2;  // Center-align text
                break;
            case LEFT:
            default:
                horizontalOffset = 0;  // Left-align text
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

        // Initialize current position for drawing each glyph
        Vec3d currentPos = startPos;

        int r = 0, g = 0, b = 0;
        if (fontColor != null && fontColor.matches("^#([A-Fa-f0-9]{6})$")) {
            // Parse hex string and extract RGB values
            r = Integer.parseInt(fontColor.substring(1, 3), 16);
            g = Integer.parseInt(fontColor.substring(3, 5), 16);
            b = Integer.parseInt(fontColor.substring(5, 7), 16);
        }

        float rFloat = r / 255.0f;
        float gFloat = g / 255.0f;
        float bFloat = b / 255.0f;
        float alpha = 1.0f;// Full opacity

        // Render each character of the text
        for (char c : text.toCharArray()) {
            Glyph glyph = getGlyphForChar(c);
            if (glyph == null) continue;

            // Calculate texture UV coordinates for the glyph
            double u = (double) glyph.x / textureWidth;
            double v = (double) glyph.y / textureHeight;
            double u1 = (double) (glyph.x + glyph.width) / textureWidth;
            double v1 = (double) (glyph.y + glyphHeight) / textureHeight;

            // Flip V coordinates because texture space might be flipped vertically
            v = 1.0 - v;
            v1 = 1.0 - v1;

            // Scale glyph size to fit the text box
            double scaledGlyphWidth = glyph.width * scaleX;
            double scaledGlyphHeight = glyphHeightScaled;

            // Calculate the four corners of the glyph quad
            Vec3d bottomLeftPos = currentPos;
            Vec3d bottomRightPos = currentPos.add(directionX.scale(scaledGlyphWidth));
            Vec3d topRightPos = bottomRightPos.add(directionY.scale(scaledGlyphHeight));
            Vec3d topLeftPos = bottomLeftPos.add(directionY.scale(scaledGlyphHeight));

            draw.vertex(bottomLeftPos.x, bottomLeftPos.y, bottomLeftPos.z).uv(u, v).color(rFloat, gFloat, bFloat, alpha);
            draw.vertex(bottomRightPos.x, bottomRightPos.y, bottomRightPos.z).uv(u1, v).color(rFloat, gFloat, bFloat, alpha);
            draw.vertex(topRightPos.x, topRightPos.y, topRightPos.z).uv(u1, v1).color(rFloat, gFloat, bFloat, alpha);
            draw.vertex(topLeftPos.x, topLeftPos.y, topLeftPos.z).uv(u, v1).color(rFloat, gFloat, bFloat, alpha);

            // Move to the next glyph position along the X axis
            currentPos = currentPos.add(directionX.scale(scaledGlyphWidth + gap * scaleX));
        }
    }

}
