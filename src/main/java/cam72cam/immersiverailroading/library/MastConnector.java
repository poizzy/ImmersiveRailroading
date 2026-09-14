package cam72cam.immersiverailroading.library;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.serialization.*;

public class MastConnector {
    @TagField
    public int id;
    @TagField
    public Vec3d a = Vec3d.ZERO;
    @TagField
    public Vec3d b = Vec3d.ZERO;
    private IBoundingBox boundingBox;
    private DirectDraw drawCache;

    public MastConnector(int id) {
        this.id = id;
    }

    public MastConnector() {}

    public IBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = IBoundingBox.from(a, b).grow(new Vec3d(0.1, 0.1, 0.1));
        }
        return boundingBox;
    }

    public Vec3d getA(Vec3d origin, float rotation) {
        return origin.add(a.rotateYaw(rotation));
    }

    public Vec3d getB(Vec3d origin, float rotation) {
        return origin.add(b.rotateYaw(rotation));
    }

    public void RenderPreview(RenderState state) {
        if (drawCache == null) {
            drawCache = new DirectDraw();

            IBoundingBox bb = getBoundingBox();
            Vec3d min = bb.min();
            Vec3d max = bb.max();

            double x0 = min.x, y0 = min.y, z0 = min.z;
            double x1 = max.x, y1 = max.y, z1 = max.z;

            double r = 1, g = 0, b = 0, alpha = 0.4;

            // Bottom (-Y)
            drawCache.vertex(x0, y0, z0).color(r, g, b, alpha);
            drawCache.vertex(x1, y0, z0).color(r, g, b, alpha);
            drawCache.vertex(x1, y0, z1).color(r, g, b, alpha);
            drawCache.vertex(x0, y0, z1).color(r, g, b, alpha);

            // Top (+Y)
            drawCache.vertex(x0, y1, z0).color(r, g, b, alpha);
            drawCache.vertex(x0, y1, z1).color(r, g, b, alpha);
            drawCache.vertex(x1, y1, z1).color(r, g, b, alpha);
            drawCache.vertex(x1, y1, z0).color(r, g, b, alpha);

            // North (-Z)
            drawCache.vertex(x0, y0, z0).color(r, g, b, alpha);
            drawCache.vertex(x0, y1, z0).color(r, g, b, alpha);
            drawCache.vertex(x1, y1, z0).color(r, g, b, alpha);
            drawCache.vertex(x1, y0, z0).color(r, g, b, alpha);

            // South (+Z)
            drawCache.vertex(x0, y0, z1).color(r, g, b, alpha);
            drawCache.vertex(x1, y0, z1).color(r, g, b, alpha);
            drawCache.vertex(x1, y1, z1).color(r, g, b, alpha);
            drawCache.vertex(x0, y1, z1).color(r, g, b, alpha);

            // West (-X)
            drawCache.vertex(x0, y0, z0).color(r, g, b, alpha);
            drawCache.vertex(x0, y0, z1).color(r, g, b, alpha);
            drawCache.vertex(x0, y1, z1).color(r, g, b, alpha);
            drawCache.vertex(x0, y1, z0).color(r, g, b, alpha);

            // East (+X)
            drawCache.vertex(x1, y0, z0).color(r, g, b, alpha);
            drawCache.vertex(x1, y1, z0).color(r, g, b, alpha);
            drawCache.vertex(x1, y1, z1).color(r, g, b, alpha);
            drawCache.vertex(x1, y0, z1).color(r, g, b, alpha);
        }

        drawCache.draw(state);
    }
}
