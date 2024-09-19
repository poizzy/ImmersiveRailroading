package cam72cam.immersiverailroading.entity;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.DirectDraw;
import org.lwjgl.opengl.GL11;

public class Font {
    private final int textureId;
    private final int glyphWidth;
    private final int glyphHeight;
    private final int textureWidth;
    private final int characterCount;
    private final int gap;  // The gap between characters (the blue line width)

    public Font(int textureId, int textureWidth, int textureHeight, int characterCount, int gap) {
        this.textureId = textureId;
        this.glyphWidth = (textureWidth - (characterCount - 1) * gap) / characterCount;  // Adjust for gaps
        this.glyphHeight = textureHeight;  // Full height of the texture is the glyph height
        this.textureWidth = textureWidth;
        this.characterCount = characterCount;
        this.gap = gap;
    }

    private double[] getUVForChar(char c) {
        int ascii = (int) c;
        // Assuming characters are in ASCII order, starting from space ' ' (ASCII 32)
        int index = ascii - 32;  // Adjust for the starting ASCII value

        // Calculate the texture coordinates, adjusting for the gap (blue line)
        double u = (index * (glyphWidth + gap)) / (double) textureWidth;
        double u1 = (u + glyphWidth) / (double) textureWidth;
        double v = 0.0;
        double v1 = 1.0;  // Full height

        return new double[]{u, v, u1, v1};
    }

    public void drawText(DirectDraw draw, String text, Vec3d pos) {
        Vec3d currentPos = pos;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        for (char c : text.toCharArray()) {
            // Get UV coordinates for the character
            double[] uv = getUVForChar(c);
            double u = uv[0], v = uv[1], u1 = uv[2], v1 = uv[3];

            // Draw the character as a quad
            draw.vertex(currentPos).uv(u, v);  // Bottom-left
            draw.vertex(currentPos.add(glyphWidth, 0, 0)).uv(u1, v);  // Bottom-right
            draw.vertex(currentPos.add(glyphWidth, glyphHeight, 0)).uv(u1, v1);  // Top-right
            draw.vertex(currentPos.add(0, glyphHeight, 0)).uv(u, v1);  // Top-left

            // Move the position along the x-axis for the next character
            currentPos = currentPos.add(glyphWidth + gap, 0, 0);  // Account for the gap (blue line)
        }
    }
}
