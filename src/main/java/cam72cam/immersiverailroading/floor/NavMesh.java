package cam72cam.immersiverailroading.floor;

import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.obj.FaceAccessor;
import cam72cam.mod.model.obj.OBJFace;
import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.util.Axis;

import java.util.*;

public class NavMesh {
    public BVHNode root;
    public BVHNode collisionRoot;
    private final boolean hasNavMesh;
    // Theoretically this could be much lower. IR floor meshes probably won't use the whole depth, but who knows
    private static final int MAX_DEPTH = 20;
    private static final int LEAF_SIZE = 8;

    public NavMesh(EntityRollingStockDefinition definition) {
        hasNavMesh = definition.getModel().floor != null;

        if (hasNavMesh) {
            initNavMesh(definition.getModel());
        } else {
            try {
                initLegacy(definition);
            } catch (IllegalArgumentException e) {
                ModCore.catching(e);
            }
        }
    }

    private void initNavMesh(StockModel<?, ?> model) {
        FaceAccessor accessor = model.getFaceAccessor();

        List<OBJFace> floor = new ArrayList<>();
        if (model.floor != null) {
            model.floor.groups().forEach(group -> {
                FaceAccessor sub = accessor.getSubByGroup(group.name);
                sub.forEach(a -> floor.add(a.asOBJFace()));
            });
        }
        this.root = buildBVH(floor, 0);

        List<OBJFace> collision = new ArrayList<>();
        if (model.collision != null) {
            model.collision.groups().forEach(group -> {
                FaceAccessor sub = accessor.getSubByGroup(group.name);
                sub.forEach(a -> collision.add(a.asOBJFace()));
            });
        }
        this.collisionRoot = buildBVH(collision, 0);
    }

    private void initLegacy(EntityRollingStockDefinition def) throws IllegalArgumentException {
        Vec3d center = def.passengerCenter;
        Double length = def.passengerCompartmentLength;
        Double width = def.passengerCompartmentWidth;

        if (length == null || width == null) {
            throw new IllegalArgumentException(String.format("Rolling stock %s needs to have either a FLOOR object or have \"length\" and \"width\" defined in the \"passenger\" section of the stocks json", def.name()));
        }

        if (center == null) {
            center = Vec3d.ZERO;
        }

        OBJFace face1 = new OBJFace();
        OBJFace face2 = new OBJFace();

        Vec2f uv = new Vec2f(0, 0);
        Vec3d normal = new Vec3d(0, 1, 0);

        Vec3d vertex1 = center.add(-length, 0, width / 2);
        Vec3d vertex2 = center.add(length, 0, width / 2);
        Vec3d vertex3 = center.add(length,  0, -width / 2);
        Vec3d vertex4 = center.add(-length, 0, -width / 2);

        face1.vertex0 = new OBJFace.Vertex(vertex1, uv);
        face1.vertex1 = new OBJFace.Vertex(vertex2, uv);
        face1.vertex2 = new OBJFace.Vertex(vertex3, uv);

        face2.vertex0 = new OBJFace.Vertex(vertex1, uv);
        face2.vertex1 = new OBJFace.Vertex(vertex3, uv);
        face2.vertex2 = new OBJFace.Vertex(vertex4, uv);

        face1.normal = normal;
        face2.normal = normal;

        this.root = buildBVH(Arrays.asList(face1, face2), 0);
        this.collisionRoot = buildBVH(Collections.emptyList(), 0);
    }

    public boolean hasNavMesh() {
        return hasNavMesh;
    }

    public static class BVHNode {
        IBoundingBox bounds;
        BVHNode left;
        BVHNode right;
        List<OBJFace> triangles;

        boolean isLeaf() {
            return triangles != null;
        }
    }

    public BVHNode buildBVH(List<OBJFace> triangles, int depth) {
        if (triangles.size() <= LEAF_SIZE || depth > MAX_DEPTH) {
            IBoundingBox bounds = IBoundingBox.from(Vec3i.ZERO);
            for (OBJFace face : triangles) {
                bounds = bounds.expandToFit(face.getBoundingBox());
            }
            BVHNode node = new BVHNode();
            node.bounds = bounds;
            node.triangles = triangles;
            return node;
        }

        IBoundingBox bounds = IBoundingBox.from(Vec3i.ZERO);
        for (OBJFace face : triangles) {
            bounds = bounds.expandToFit(face.getBoundingBox());
        }

        Vec3d size = bounds.max().subtract(bounds.min());
        Axis axis = (size.x > size.y && size.x > size.z) ? Axis.X : (size.y > size.z ? Axis.Y : Axis.Z);

        triangles.sort((a, b) -> Double.compare(getCentroid(a, axis), getCentroid(b, axis)));
        int mid = triangles.size() / 2;

        BVHNode node = new BVHNode();
        node.left = buildBVH(triangles.subList(0, mid), depth + 1);
        node.right = buildBVH(triangles.subList(mid, triangles.size()), depth + 1);
        node.bounds = bounds;
        return node;
    }

    public void queryBVH(BVHNode node, IBoundingBox query, List<OBJFace> result, double scale) {
        query = unscaleBoxUniform(query, scale);
        queryBVHInternal(node, query, result, scale);
    }



    public void queryBVHInternal(BVHNode node, IBoundingBox query, List<OBJFace> result, double scale) {
        if (node == null) return;
        if (!node.bounds.intersects(query)) return;

//        query = unscaleBoxUniform(query, scale);

        if (node.isLeaf()) {
            for (OBJFace tri : node.triangles) {
                if (tri.getBoundingBox().intersects(query)) {
                    result.add(tri.scale(scale));
                }
            }
        } else {
            queryBVHInternal(node.left, query, result, scale);
            queryBVHInternal(node.right, query, result, scale);
        }
    }

    private static IBoundingBox unscaleBoxUniform(IBoundingBox box, double s) {
        Vec3d a = box.min().scale(1.0 / s);
        Vec3d b = box.max().scale(1.0 / s);

        IBoundingBox out = IBoundingBox.from(
                new Vec3d(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z)),
                new Vec3d(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z))
        );
        return out;
    }

    private double getCentroid(OBJFace tri, Axis axis) {
        return (VecUtil.getByAxis(tri.vertex0.pos, axis) + VecUtil.getByAxis(tri.vertex1.pos, axis) + VecUtil.getByAxis(tri.vertex2.pos, axis)) / 3f;
    }
}