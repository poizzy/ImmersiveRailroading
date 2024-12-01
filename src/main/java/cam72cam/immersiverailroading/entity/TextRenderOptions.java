package cam72cam.immersiverailroading.entity;

import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagCompound;

import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TextRenderOptions {
    public Identifier id;
    public String newText;
    public final int resX;
    public final int resY;
    public Font.TextAlign align;
    public final boolean flipped;
    public final String componentId;
    public int fontSize;
    public int fontX;
    public final int fontGap;
    public final List<Integer> fontId;
    public String hexCode;
    public final boolean fullbright;
    public int textureHeight;
    public final boolean useAlternative;
    public int lineSpacingPixels;
    public int offset;
    public boolean global;

    public List<String> linked = new ArrayList<>();

    public boolean selectable = true;

    // Used for number plates etc...
    public boolean unique = false;

    public boolean isNumberPlate = false;

    public String lastText = "";

    public List<String> filter = new ArrayList<>();

    public boolean assigned = false;

    public TextRenderOptions(Identifier id, String newText, int resX, int resY, Font.TextAlign align, boolean flipped,
                             String componentId, int fontSize, int fontX, int fontGap, List<Integer> fontId, String hexCode,
                             boolean fullbright, int textureHeight, boolean useAlternative, int lineSpacingPixels, int offset, boolean global) {
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
        this.fontId = fontId;
        this.hexCode = hexCode;
        this.fullbright = fullbright;
        this.textureHeight = textureHeight;
        this.useAlternative = useAlternative;
        this.lineSpacingPixels = lineSpacingPixels;
        this.offset = offset;
        this.global = global;
    }

    private TextRenderOptions(TextRenderOptions options) {
        this.id = options.id;
        this.newText = options.newText;
        this.resX = options.resX;
        this.resY = options.resY;
        this.align = options.align;
        this.flipped = options.flipped;
        this.componentId = options.componentId;
        this.fontSize = options.fontSize;
        this.fontX = options.fontX;
        this.fontGap = options.fontGap;
        this.fontId = options.fontId;
        this.hexCode = options.hexCode;
        this.fullbright = options.fullbright;
        this.textureHeight = options.textureHeight;
        this.useAlternative = options.useAlternative;
        this.lineSpacingPixels = options.lineSpacingPixels;
        this.offset = options.offset;
        this.global = options.global;
        this.linked = options.linked;
        this.selectable = options.selectable;
        this.unique = options.unique;
        this.isNumberPlate = options.isNumberPlate;
        this.filter = options.filter;
        this.assigned = options.assigned;
    }

    public void setLinked(List<String> linked) {
        this.linked.addAll(linked);
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public void setIsNumberPlate(boolean numberPlate) {
        this.isNumberPlate = numberPlate;
        this.unique = true;
    }

    public void setFilter(List<String> filter) {
        this.filter.addAll(filter);
    }

    public TextRenderOptions clone() {
        return new TextRenderOptions(this);
    }

    public TextRenderOptions(TagCompound compound) throws IOException {
        this.id = new Identifier(decompressString(compound.getString("id")));
        this.newText = decompressString(compound.getString("nT"));
        this.resX = compound.getInteger("rX");
        this.resY = compound.getInteger("rY");
        this.align = Font.TextAlign.valueOf(decompressString(compound.getString("a")));
        this.flipped = compound.getBoolean("f");
        this.componentId = decompressString(compound.getString("cId"));
        this.fontSize = compound.getInteger("fS");
        this.fontX = compound.getInteger("fX");
        this.fontGap = compound.getInteger("fG");
        this.fontId = null;
        this.hexCode = decompressString(compound.getString("hC"));
        this.fullbright = compound.getBoolean("fb");
        this.textureHeight = compound.getInteger("tH");
        this.useAlternative = compound.getBoolean("uA");
        this.lineSpacingPixels = compound.getInteger("lSP");
        this.offset = compound.getInteger("o");
        this.global = compound.getBoolean("g");
    }

    public void serializeTextRenderOptions(TagCompound compound) throws IOException {
        compound.setString("id", compressString(this.id.toString()));
        compound.setString("nT", compressString(this.newText));
        compound.setInteger("rX", this.resX);
        compound.setInteger("rY", this.resY);
        compound.setString("a", compressString(this.align.name()));
        compound.setBoolean("f", this.flipped);
        compound.setString("cId", compressString(this.componentId));
        compound.setInteger("fS", this.fontSize);
        compound.setInteger("fX", this.fontX);
        compound.setInteger("fG", this.fontGap);
        if (this.fontId != null) {
            compound.setString("overlay", this.fontId.toString());
        }
        compound.setString("hC", compressString(this.hexCode));
        compound.setBoolean("fb", this.fullbright);
        compound.setInteger("tH", this.textureHeight);
        compound.setBoolean("uA", this.useAlternative);
        compound.setInteger("lSP", this.lineSpacingPixels);
        compound.setInteger("o", this.offset);
        compound.setBoolean("g", this.global);
    }

    public static String compressString(String str) throws IOException {
        if (str == null || str.isEmpty()) {
            return str;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
        gzipOutputStream.close();
        return byteArrayOutputStream.toString("ISO-8859-1");
    }

    public static String decompressString(String compressedStr) throws IOException {
        if (compressedStr == null || compressedStr.isEmpty()) {
            return compressedStr;
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedStr.getBytes("ISO-8859-1"));
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int bytesRead;
        while ((bytesRead = gzipInputStream.read(buffer)) > 0) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return byteArrayOutputStream.toString();
    }

    @Override
    public String toString() {
        return this.componentId;
    }
}
