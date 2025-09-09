package cam72cam.immersiverailroading.floor;

import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.obj.OBJFace;

import java.util.List;
import java.util.stream.Collectors;

public class NavMesh {
    public BVHNode root;
    public BVHNode collisionRoot;
    private final boolean hasNavMesh;
    // Theoretically this could be much lower. IR floor meshes probably won't use the whole depth, but who knows
    private static final int MAX_DEPTH = 20;
    private static final int LEAF_SIZE = 8;

    public NavMesh(StockModel<?, ?> model) {
        hasNavMesh = model.groups().stream().anyMatch(s -> s.contains("FLOOR"));
        if (!hasNavMesh) return;
        // I could hook FLOOR and COLLISION to the Model Component system, but that would mean you'll have to add a separate FLOOR object to your actual floor
        List<OBJFace> floor = model.getFaces(model.groups.values().stream().filter(n -> n.name.contains("FLOOR")).collect(Collectors.toList()));
        this.root = buildBVH(floor, 0);

        List<OBJFace> collision = model.getFaces(model.groups.values().stream().filter(n -> n.name.contains("COLLISION")).collect(Collectors.toList()));
        this.collisionRoot = buildBVH(collision, 0);
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
        int axis = (size.x > size.y && size.x > size.z) ? 0 : (size.y > size.z ? 1 : 2);

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

    private double getCentroid(OBJFace tri, int axis) {
        return (VecUtil.getAxis(tri.vertices.get(0), axis) + VecUtil.getAxis(tri.vertices.get(1), axis) + VecUtil.getAxis(tri.vertices.get(2), axis)) / 3f;
    }
}
