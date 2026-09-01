package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.registry.WireDefinition;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.DirectDraw;

import java.util.HashMap;
import java.util.Map;

public class WireBuilder {
    public static DirectDraw build(WireDefinition def, Vec3d end) {
        // This isn't ideal;
        DirectDraw model = new DirectDraw();
        double length = end.length();
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d tangent = end.normalize();
        Vec3d planeNormal = tangent.crossProduct(up).normalize();

        Map<String, Double> sagByStrand = new HashMap<>();
        Map<String, Double> yOffsetByStrand = new HashMap<>();

        for (WireDefinition.Wire strand : def.wires) {
            double sag = strand.sagRatio * length;
            sagByStrand.put(strand.name, sag);
            yOffsetByStrand.put(strand.name, (double) strand.yOffset);

            Vec3d[] centerline = new Vec3d[strand.segments + 1];
            for (int i = 0; i <= strand.segments; i++) {
                double t = (double) i / strand.segments;
                centerline[i] = end.scale(t).add(0, strand.yOffset + sagY(t, sag), 0);
            }
            emitRibbon(model, centerline, strand.width, strand.color, planeNormal);
        }

        for (WireDefinition.Connector conn : def.connectors) {
            double sagFrom = sagByStrand.getOrDefault(conn.from, 0.0);
            double sagTo = sagByStrand.getOrDefault(conn.to, 0.0);
            double yFrom = yOffsetByStrand.getOrDefault(conn.from, 0.0);
            double yTo = yOffsetByStrand.getOrDefault(conn.to, 0.0);

            int n = (int) Math.round(length / conn.spacing);
            if (n <= 0) continue;

            int lo = conn.excludeEnds ? 1 : 0;
            int hi = conn.excludeEnds ? n - 1 : n;

            for (int i = lo; i <= hi; i++) {
                double t = (double) i / n;
                double x = t * length;
                Vec3d base = end.scale(t);
                Vec3d from = base.add(0, yFrom + sagY(t, sagFrom), 0);
                Vec3d to = base.add(0, yTo + sagY(t, sagTo), 0);
                emitRibbon(model, new Vec3d[]{from, to}, conn.width, conn.color, planeNormal);
            }
        }
        return model;
    }

    private static double sagY(double t, double sag) {
        return -4.0 * sag * t * (1.0 - t);
    }

    private static void emitRibbon(DirectDraw draw, Vec3d[] centerline, double width, String color, Vec3d planeNormal) {
        double half = width / 2;
        Vec3d[] left = new Vec3d[centerline.length];
        Vec3d[] right = new Vec3d[centerline.length];

        for (int i = 0; i < centerline.length; i++) {
            Vec3d localTangent;
            if (i == 0) localTangent = centerline[1].subtract(centerline[0]).normalize();
            else if (i == centerline.length - 1) localTangent = centerline[i].subtract(centerline[i - 1]).normalize();
            else localTangent = centerline[i + 1].subtract(centerline[i - 1]).normalize();

            Vec3d offsetAxis = planeNormal.crossProduct(localTangent).normalize();
            left[i] = centerline[i].add(offsetAxis.scale(half));
            right[i] = centerline[i].subtract(offsetAxis.scale(half));
        }

        for (int i = 0; i < centerline.length - 1; i++) {
            addVert(draw, left[i], color);
            addVert(draw, right[i], color);
            addVert(draw, right[i + 1], color);
            addVert(draw, left[i + 1], color);
        }
    }

    private static void addVert(DirectDraw draw, Vec3d pos, String color) {
        draw.vertex(pos).normal(0, 0, 1).color(Integer.parseInt(color.substring(0, 2)) / 255f, Integer.parseInt(color.substring(2, 4)) / 255f, Integer.parseInt(color.substring(4, 6)) / 255f, 1);
    }
}
