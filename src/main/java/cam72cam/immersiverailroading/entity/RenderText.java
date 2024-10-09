package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.model.animation.StockAnimation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.render.opengl.*;
import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GLContext;
import util.Matrix4;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RenderText {
    private static final Map<String, RenderText> instances = new HashMap<>();
    private DirectDraw draw;
    private final ConcurrentMap<String, TextField> textFields = new ConcurrentHashMap<>();


    public RenderText() {
        draw = new DirectDraw();
    }

    public static RenderText getInstance(String name) {
        return instances.computeIfAbsent(name, k -> new RenderText());
    }

    public static class TextField {
        private final int texY;
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
        Font myFont;
        boolean fontInitialized = false;
        boolean useAlternative;
        int lineSpacingPixels;
        int offset;

        TextField(String text, Identifier id, Vec3d vec3dmin, Vec3d vec3dmax,
                  InputStream json, int resX, int resY, Font.TextAlign align,
                  boolean flipDir, int fontSize, int fontLength, int fontGap,
                  Identifier overlayId, Vec3d normal, String hexCode, boolean fullbright, int texY, boolean useAlternative, int lineSpacingPixels, int offset) {
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
            this.texY = texY;
            this.useAlternative = useAlternative;
            this.lineSpacingPixels = lineSpacingPixels;
            this.offset = offset;
        }

        void initializeFont() {
            if (!fontInitialized) {
                int textureId = new MinecraftTexture(this.id).getId();
                this.myFont = new Font(textureId, this.fontLength, this.fontSize, this.fontGap, this.json, this.texY);
                this.fontInitialized = true;
            }
        }
    }

    public void setText(String componentId, String text, Identifier id, Vec3d vec3dmin, Vec3d vec3dmax,
                        InputStream json, int resX, int resY, Font.TextAlign align,
                        boolean flipDir, int fontSize, int fontLength, int fontGap,
                        Identifier overlayId, Vec3d normal, String hexCode, boolean fullbright, int texY, boolean useAlternative, int lineSpacingPixels, int offset) {
        textFields.put(componentId, new TextField(text, id, vec3dmin, vec3dmax, json, resX, resY, align, flipDir, fontSize, fontLength, fontGap, overlayId, normal, hexCode, fullbright, texY, useAlternative, lineSpacingPixels, offset));
    }

    public void textRender(RenderState state, List<StockAnimation> animations, EntityRollingStock stock, float partialTicks) {
        if (!GLContext.getCapabilities().OpenGL11) {
            return;
        }

        for (Map.Entry<String, TextField> entry : textFields.entrySet()) {
            TextField field = entry.getValue();

            field.initializeFont();

            RenderState renderText = state.clone()
                    .texture(Texture.wrap(field.id))
                    .lightmap(field.fullbright ? 1 : 0, 1)
                    .blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA));

            int r = 0, g = 0, b = 0;
            if (field.hexCode != null && field.hexCode.matches("^#([A-Fa-f0-9]{6})$")) {
                r = Integer.parseInt(field.hexCode.substring(1, 3), 16);
                g = Integer.parseInt(field.hexCode.substring(3, 5), 16);
                b = Integer.parseInt(field.hexCode.substring(5, 7), 16);
            }

            float rFloat = r / 255.0f;
            float gFloat = g / 255.0f;
            float bFloat = b / 255.0f;
            float alpha = 1.0f;

            renderText.color(rFloat, gFloat, bFloat, alpha);

            // This is so fuckin ugly I can't even put it into words
            Matrix4 transformationMatrix = null;
            for (StockAnimation animation : animations) {
                LinkedHashMap<String, OBJGroup> group = stock.getDefinition().getModel().groups;
                for (Map.Entry<String, OBJGroup> groupEntry : group.entrySet()) {
                    if (groupEntry.getKey().contains(entry.getKey())) {
                        Matrix4 animMatrix = animation.getMatrix(stock, groupEntry.getKey(), partialTicks);
                        if (animMatrix != null) {
                            transformationMatrix = animMatrix;
                            break;
                        }
                    }
                }

                if (transformationMatrix != null) {
                    Vec3d animatedVec3dmin = transformationMatrix.apply(field.vec3dmin);
                    Vec3d animatedVec3dmax = transformationMatrix.apply(field.vec3dmax);

                    draw = new DirectDraw();
                    field.myFont.drawText(draw, field.text, animatedVec3dmin, animatedVec3dmax, renderText,
                            field.resX, field.resY, field.textAlign, field.normal,
                            field.flipDirection, field.hexCode, field.useAlternative, field.lineSpacingPixels, field.offset);

                    draw.draw(renderText);
                } else {
                    draw = new DirectDraw();
                    field.myFont.drawText(draw, field.text, field.vec3dmin, field.vec3dmax, renderText,
                            field.resX, field.resY, field.textAlign, field.normal,
                            field.flipDirection, field.hexCode, field.useAlternative, field.lineSpacingPixels, field.offset);

                    draw.draw(renderText);
                }
            }
        }
    }
}