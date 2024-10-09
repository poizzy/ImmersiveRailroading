package cam72cam.immersiverailroading.entity;

import cam72cam.mod.resource.Identifier;

public class TextRenderOptions {
    public final Identifier id;
    public final String newText;
    public final int resX;
    public final int resY;
    public final Font.TextAlign align;
    public final boolean flipped;
    public final String componentId;
    public final int fontSize;
    public final int fontX;
    public final int fontGap;
    public final Identifier overlay;
    public final String hexCode;
    public final boolean fullbright;
    public final int textureHeight;
    public final boolean useAlternative;
    public final int lineSpacingPixels;
    public final int offset;

    public TextRenderOptions(Identifier id, String newText, int resX, int resY, Font.TextAlign align, boolean flipped,
                             String componentId, int fontSize, int fontX, int fontGap, Identifier overlay, String hexCode,
                             boolean fullbright, int textureHeight, boolean useAlternative, int lineSpacingPixels, int offset) {
        this.id = id;
        this.newText = newText;
        this.resX = resX;
        this.resY = resY;
        this.align = align;
        this.flipped = flipped;
        this.componentId = componentId;
        this.fontSize = fontSize;
        this.fontX = fontX;
        this.fontGap = fontGap;
        this.overlay = overlay;
        this.hexCode = hexCode;
        this.fullbright = fullbright;
        this.textureHeight = textureHeight;
        this.useAlternative = useAlternative;
        this.lineSpacingPixels = lineSpacingPixels;
        this.offset = offset;
    }
}
