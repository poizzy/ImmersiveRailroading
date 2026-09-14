package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.registry.WireDefinition;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.resource.Identifier;

import java.util.*;

public class WireBuilder {

    public static Model build(WireDefinition def, Vec3d delta, float multiplier) {
        Vec3d parentA = new Vec3d(0, 1, 0);
        Vec3d targetA = parentA.add(delta);
        Vec3d parentB = Vec3d.ZERO;
        Vec3d targetB = parentB.add(delta);
        return build(def, targetA, targetB, parentA, parentB, multiplier);
    }


    public static Model build(WireDefinition def, Vec3d targetA, Vec3d targetB, Vec3d parentA, Vec3d parentB) {
        return build(def, targetA, targetB, parentA, parentB, 1);
    }

    public static Model build(WireDefinition def, Vec3d targetA, Vec3d targetB, Vec3d parentA, Vec3d parentB, float multiplier) {

        VertexBuilder builder = new VertexBuilder();

        // Transform to local space with parentA as (0, 0, 0)
        Vec3d localA1 = targetA.subtract(parentA);
        Vec3d localB1 = targetB.subtract(parentA);
        Vec3d localA2 = parentA.subtract(parentA);
        Vec3d localB2 = parentB.subtract(parentA);

        Map<String, Vec3d> startByStrand = new HashMap<>();
        Map<String, Vec3d> endByStrand = new HashMap<>();
        startByStrand.put("A", localA1);
        endByStrand.put("A", localA2);
        startByStrand.put("B", localB1);
        endByStrand.put("B", localB2);

        double length = localA2.subtract(localA1).length();
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d tangent = localA2.subtract(localA1).normalize();
        Vec3d planeNormal = tangent.crossProduct(up).normalize();

        Map<String, Double> sagByStrand = new HashMap<>();
        Map<String, Double> yOffsetByStrand = new HashMap<>();
        Map<String, Vec3d> strandStart = new HashMap<>();
        Map<String, Vec3d> strandEnd = new HashMap<>();

        for (WireDefinition.Wire strand : def.wires) {
            Vec3d start = startByStrand.get(strand.name);
            Vec3d end = endByStrand.get(strand.name);
            if (start == null || end == null) {
                throw new IllegalArgumentException("Unknown wire strand name: " + strand.name + " (expected \"A\" or \"B\")");
            }

            double strandLength = end.subtract(start).length();
            double sag = strand.sagRatio * strandLength;
            sagByStrand.put(strand.name, sag);
            yOffsetByStrand.put(strand.name, (double) strand.yOffset);
            strandStart.put(strand.name, start);
            strandEnd.put(strand.name, end);

            Vec3d[] centerline = new Vec3d[strand.segments + 1];
            for (int i = 0; i <= strand.segments; i++) {
                double t = (double) i / strand.segments;
                Vec3d base = start.add(end.subtract(start).scale(t));
                centerline[i] = base.add(0, strand.yOffset + sagY(t, sag), 0);
            }
            emitRibbon(builder, centerline, strand.width * multiplier, strand.color, planeNormal);
        }

        for (WireDefinition.Connector conn : def.connectors) {
            Vec3d fromStart = strandStart.get(conn.from);
            Vec3d fromEnd = strandEnd.get(conn.from);
            Vec3d toStart = strandStart.get(conn.to);
            Vec3d toEnd = strandEnd.get(conn.to);
            if (fromStart == null || toStart == null) continue;

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

                Vec3d fromBase = fromStart.add(fromEnd.subtract(fromStart).scale(t));
                Vec3d toBase = toStart.add(toEnd.subtract(toStart).scale(t));

                Vec3d from = fromBase.add(0, yFrom + sagY(t, sagFrom), 0);
                Vec3d to = toBase.add(0, yTo + sagY(t, sagTo), 0);
                emitRibbon(builder, new Vec3d[]{from, to}, conn.width * multiplier, conn.color, planeNormal);
            }
        }
        return builder.build();
    }

    private static double sagY(double t, double sag) {
        return -4.0 * sag * t * (1.0 - t);
    }

    private static void emitRibbon(VertexBuilder builder, Vec3d[] centerline, double width, String color, Vec3d planeNormal) {
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

        float r = Integer.parseInt(color.substring(0, 2)) / 255f;
        float g = Integer.parseInt(color.substring(2, 4)) / 255f;
        float b = Integer.parseInt(color.substring(4, 6)) / 255f;
        float a = 1;

        for (int i = 0; i < centerline.length - 1; i++) {
            builder.addVertex(left[i], r, g, b, a);
            builder.addVertex(right[i], r, g, b, a);
            builder.addVertex(right[i + 1], r, g, b, a);

            builder.addVertex(left[i], r, g, b, a);
            builder.addVertex(right[i + 1], r, g, b, a);
            builder.addVertex(left[i + 1], r, g, b, a);
        }
    }

    public static class VertexBuilder {
        private static final Identifier LOCATION = new Identifier("WIRE_BUILDER");
        private static final VAOLayout layout = VAOLayout.POS_TEX_COLOR;
        private final List<Vertex> vertices = new ArrayList<>();

        public void addVertex(Vec3d pos, float r, float g, float b, float a) {
            addVertex((float) pos.x, (float) pos.y, (float) pos.z, r, g, b, a);
        }

        private void addVertex(float x, float y, float z, float r, float g, float b, float a) {
            vertices.add(new Vertex(x, y, z, (float) 0.0, (float) 0.0, r, g, b, a));
        }

        public Model build() {
            int strideF = layout.getStride();
            int triCount = vertices.size();
            float[] data = new float[triCount * strideF];

            int posOff = layout.getOffset(VAOLayout.Usage.POSITION);
            int uvOff = layout.getOffset(VAOLayout.Usage.UV);
            int colorOff = layout.getOffset(VAOLayout.Usage.COLOR);

            for (int i = 0; i < triCount; i++) {
                Vertex vertex = vertices.get(i);

                int base = i * strideF;

                if (posOff != -1) {
                    data[base + posOff] = vertex.x();
                    data[base + posOff + 1] = vertex.y();
                    data[base + posOff + 2] = vertex.z();
                }

                if (uvOff != -1) {
                    data[base + uvOff] = vertex.u();
                    data[base + uvOff] = vertex.v();
                }

                if (colorOff != -1) {
                    data[base + colorOff] = vertex.r();
                    data[base + colorOff + 1] = vertex.g();
                    data[base + colorOff + 2] = vertex.b();
                    data[base + colorOff + 3] = vertex.a();
                }
            }

            return new Model(LOCATION, layout, () -> data, new LinkedHashMap<>(), false, false, false, 0, 0);
        }

        public record Vertex(float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        }
    }
}
