package cam72cam.immersiverailroading.floor;

import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public class NavMesh {
    public BVHNode root;
    public BVHNode collisionRoot;
    private boolean hasNavMesh = false;

    public NavMesh(Mesh mesh) {
        hasNavMesh = !mesh.getGroupContains("FLOOR").isEmpty();
        this.root = buildBVH(mesh.getGroupContains("FLOOR").stream().flatMap(m -> m.faces.stream()).collect(Collectors.toList()), 18);
        this.collisionRoot = buildBVH(mesh.getGroupContains("COLLISION").stream().flatMap(m -> m.faces.stream()).collect(Collectors.toList()), 20);
    }

    public boolean hasNavMesh() {
        return hasNavMesh;
    }

    public static class BVHNode {
        CollisionBox bounds;
        BVHNode left;
        BVHNode right;
        List<Mesh.Face> triangles;

        boolean isLeaf() {
            return triangles != null;
        }
    }

    public BVHNode buildBVH(List<Mesh.Face> triangles, int depth) {
        if (triangles.size() <= 5 || depth > 20) {
            CollisionBox bounds = new CollisionBox();
            triangles.forEach(t -> bounds.expandToFit(t.getCollisionBox()));
            BVHNode node = new BVHNode();
            node.bounds = bounds;
            node.triangles = triangles;
            return node;
        }

        CollisionBox bounds = new CollisionBox();
        triangles.forEach(t -> bounds.expandToFit(t.getCollisionBox()));

        Vec3d size = bounds.max.subtract(bounds.min);
        int axis = (size.x > size.y && size.x > size.z) ? 0 : (size.y > size.z ? 1 : 2);

        triangles.sort((a, b) -> Double.compare(getCentroid(a, axis), getCentroid(b, axis)));
        int mid = triangles.size() / 2;

        BVHNode node = new BVHNode();
        node.left = buildBVH(triangles.subList(0, mid), depth + 1);
        node.right = buildBVH(triangles.subList(mid, triangles.size()), depth + 1);
        node.bounds = bounds;
        return node;
    }

    public void queryBVH(BVHNode node, CollisionBox query, List<Mesh.Face> result, double scale) {
        query = unscaleBoxUniform(query, scale);
        queryBVHInternal(node, query, result, scale);
    }



    public void queryBVHInternal(BVHNode node, CollisionBox query, List<Mesh.Face> result, double scale) {
//        if (!node.bounds.intersects(query)) return;

//        query = unscaleBoxUniform(query, scale);

        if (node.isLeaf()) {
            for (Mesh.Face tri : node.triangles) {
                if (tri.getCollisionBox().intersects(query)) {
                    result.add(tri.scale(scale));
                }
            }
        } else {
            queryBVHInternal(node.left, query, result, scale);
            queryBVHInternal(node.right, query, result, scale);
        }
    }

    private static CollisionBox unscaleBoxUniform(CollisionBox box, double s) {
        Vec3d a = box.min.scale(1.0 / s);
        Vec3d b = box.max.scale(1.0 / s);

        CollisionBox out = new CollisionBox(
                new Vec3d(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z)),
                new Vec3d(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z))
        );
        return out;
    }

    private double getCentroid(Mesh.Face tri, int axis) {
        return (VecUtil.getAxis(tri.vertices.get(0), axis) + VecUtil.getAxis(tri.vertices.get(1), axis) + VecUtil.getAxis(tri.vertices.get(2), axis)) / 3f;
    }
}
