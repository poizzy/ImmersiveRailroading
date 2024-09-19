package cam72cam.immersiverailroading.entity;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.MinecraftTexture;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GLContext;

import java.util.HashMap;
import java.util.Map;

public class RenderText {
    private Font myFont;
    private DirectDraw draw;
    private static Map<String, RenderText> instances = new HashMap<>();
    private String text;
    private Identifier id;
    private Vec3d vec3d;
    private boolean fontInitialized;

    public RenderText() {
        draw = new DirectDraw();
    }

    public static RenderText getInstance(String name) {
        RenderText instance = instances.get(name);
        if (instance == null) {
            instance = new RenderText();
            instances.put(name, instance);
        }
        return instance;
    }

    public void setText(String text, Identifier id, Vec3d vec3d) {
        this.text = text;
        this.id = id;
        this.vec3d = vec3d;
        fontInitialized = false;
    }

    public void textRender(RenderState state) {
        if (!GLContext.getCapabilities().OpenGL11) {
            return;
        }

        if (!fontInitialized) {
            int textureId = new MinecraftTexture(id).getId();
            myFont = new Font(textureId, 625, 19, 95, 1);
            fontInitialized = true;
        }

        drawText(text, vec3d, state);
    }

    private void drawText(String text, Vec3d vec3d, RenderState state) {
        if (myFont != null) {
            myFont.drawText(draw, text, vec3d);
            draw.draw(state);
        }
    }
}
