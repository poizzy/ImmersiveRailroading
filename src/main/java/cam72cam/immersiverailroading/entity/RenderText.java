package cam72cam.immersiverailroading.entity;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.*;
import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GLContext;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RenderText {
    private Font myFont;
    private static Map<String, RenderText> instances = new HashMap<>();
    private boolean fontInitialized;
    private DirectDraw draw;
    private int textureId;
    private Map<String, TextField> textFields = new HashMap<>();


    public RenderText() {
        draw = new DirectDraw();
    }

    public static RenderText getInstance(String name) {
        RenderText instance = instances.computeIfAbsent(name, k -> new RenderText());
        return instance;
    }

    private class TextField {
        private final int texX;
        boolean fullbright;
        Vec3d normal;
        String text;
        Identifier id;
        Vec3d vec3dmin;
        Vec3d vec3dmax;
        InputStream json;
        int resX;
        int resY;
        Font.TextAlign textAlign;
        boolean flipDirection;
        int fontSize;
        int fontLength;
        int fontGap;
        Identifier overlayId;
        String hexCode;

        TextField(String text, Identifier id, Vec3d vec3dmin, Vec3d vec3dmax,
                  InputStream json, int resX, int resY, Font.TextAlign align,
                  boolean flipDir, int fontSize, int fontLength, int fontGap,
                  Identifier overlayId, Vec3d normal, String hexCode, boolean fullbright, int texX) {
            this.text = text;
            this.id = id;
            this.vec3dmin = vec3dmin;
            this.vec3dmax = vec3dmax;
            this.json = json;
            this.resX = resX;
            this.resY = resY;
            this.textAlign = align;
            this.flipDirection = flipDir;
            this.fontSize = fontSize;
            this.fontLength = fontLength;
            this.fontGap = fontGap;
            this.overlayId = overlayId;
            this.normal = normal;
            this.hexCode = hexCode;
            this.fullbright = fullbright;
            this.texX = texX;
        }
    }

    public void setText(String componentId, String text, Identifier id, Vec3d vec3dmin, Vec3d vec3dmax,
                        InputStream json, int resX, int resY, Font.TextAlign align,
                        boolean flipDir, int fontSize, int fontLength, int fontGap,
                        Identifier overlayId, Vec3d normal, String hexCode, boolean fullbright, int texX) {
        textFields.put(componentId, new TextField(text, id, vec3dmin, vec3dmax, json, resX, resY, align, flipDir, fontSize, fontLength, fontGap, overlayId, normal, hexCode, fullbright, texX));
    }

    public void textRender(RenderState state) {
        if (!GLContext.getCapabilities().OpenGL11) {
            return;
        }
        for (Map.Entry<String, TextField> entry : textFields.entrySet()) {
            TextField field = entry.getValue();

            if (!fontInitialized) {
                textureId = new MinecraftTexture(field.id).getId();
                myFont = new Font(textureId, field.fontLength, field.fontSize, field.fontGap, field.json, field.texX);
                fontInitialized = true;
            }
            RenderState renderText = state.clone()
                    .texture(Texture.wrap(field.id))
                    .lightmap(field.fullbright ? 1 : 0, 1)
                    .blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA));

            int r = 0, g = 0, b = 0;
            if (field.hexCode != null && field.hexCode.matches("^#([A-Fa-f0-9]{6})$")) {
                // Parse hex string and extract RGB values
                r = Integer.parseInt(field.hexCode.substring(1, 3), 16);
                g = Integer.parseInt(field.hexCode.substring(3, 5), 16);
                b = Integer.parseInt(field.hexCode.substring(5, 7), 16);
            }

            float rFloat = r / 255.0f;
            float gFloat = g / 255.0f;
            float bFloat = b / 255.0f;
            float alpha = 1.0f;// Full opacity

            renderText.color(rFloat, gFloat, bFloat, alpha);

            draw = new DirectDraw();
            myFont.drawText(draw, field.text, field.vec3dmin, field.vec3dmax, renderText, field.resX, field.resY, field.textAlign, field.normal, field.flipDirection, field.hexCode);

            draw.draw(renderText);
        }
    }
}
