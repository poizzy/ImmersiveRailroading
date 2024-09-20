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
    private String text;
    private Identifier id;
    private Vec3d vec3d;
    private boolean fontInitialized;
    private String oldText;
    private boolean dirty;
    private DirectDraw draw;
    private int textureId;
    private InputStream json;

    public RenderText() {
        dirty = true;
        draw = new DirectDraw();
    }

    public static RenderText getInstance(String name) {
        RenderText instance = instances.computeIfAbsent(name, k -> new RenderText());
        return instance;
    }

    public void setText(String text, Identifier id, Vec3d vec3d, InputStream json) {
        if (!text.equals(oldText)) {
            this.text = text;
            this.id = id;
            this.vec3d = vec3d;
            this.json = json;
            oldText = text;
            dirty = true;  // Mark text as dirty if it has changed
        }
    }

    public void textRender(RenderState state) {
        if (id != null) {
            if (!GLContext.getCapabilities().OpenGL11) {
                return;
            }

            if (!fontInitialized) {
                textureId = new MinecraftTexture(id).getId();
                myFont = new Font(textureId, 625, 19, 1, json);
                fontInitialized = true;
            }

            // Only redraw if the text has changed (is dirty)
            if (dirty && myFont != null) {
                draw = new DirectDraw();  // Clear any existing vertices before drawing new ones
                myFont.drawText(draw, text, vec3d, state);
                dirty = false;  // Reset dirty flag once the text is rendered
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            draw.draw(state);
        }
    }
}
