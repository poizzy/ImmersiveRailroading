package cam72cam.immersiverailroading.entity;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.MinecraftTexture;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GL11;
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
    private int overlayTextureId;
    private Map<Integer, TextField> textFields = new HashMap<>();


    public RenderText() {
        draw = new DirectDraw();
    }

    public static RenderText getInstance(String name) {
        RenderText instance = instances.computeIfAbsent(name, k -> new RenderText());
        return instance;
    }

    private class TextField {
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

        TextField(String text, Identifier id, Vec3d vec3dmin, Vec3d vec3dmax,
                  InputStream json, int resX, int resY, Font.TextAlign align,
                  boolean flipDir, int fontSize, int fontLength, int fontGap,
                  Identifier overlayId) {
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
        }
    }

    public void setText(int componentId, String text, Identifier id, Vec3d vec3dmin, Vec3d vec3dmax,
                        InputStream json, int resX, int resY, Font.TextAlign align,
                        boolean flipDir, int fontSize, int fontLength, int fontGap,
                        Identifier overlayId) {
        textFields.put(componentId, new TextField(text, id, vec3dmin, vec3dmax, json, resX, resY, align, flipDir, fontSize, fontLength, fontGap, overlayId));
    }

    public void textRender(RenderState state) {
        RenderState renderText = state.clone();
        if (!GLContext.getCapabilities().OpenGL11) {
            return;
        }
        for (Map.Entry<Integer, TextField> entry : textFields.entrySet()) {
            TextField field = entry.getValue();

            if (!fontInitialized) {
                if (field.overlayId != null) {
                    overlayTextureId = new MinecraftTexture(field.overlayId).getId();
                }
                textureId = new MinecraftTexture(field.id).getId();
                myFont = new Font(textureId, field.fontLength, field.fontSize, field.fontGap, field.json);
                fontInitialized = true;
            }

            draw = new DirectDraw();
            myFont.drawText(draw, field.text, field.vec3dmin, field.vec3dmax, renderText, field.resX, field.resY, field.flipDirection, field.textAlign);

            GL11.glEnable(GL11.GL_BLEND);
            // TODO? Re-add Overlay if possible
            if (field.overlayId != null) {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, overlayTextureId);
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            draw.draw(renderText);
            GL11.glDisable(GL11.GL_BLEND);
        }
    }
}
