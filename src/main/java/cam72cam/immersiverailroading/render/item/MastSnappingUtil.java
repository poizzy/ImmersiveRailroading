package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.track.*;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;

public class MastSnappingUtil {
    public static VecYPR getClosestPointOnTrack(World world, TileRail rail, Vec3d hit) {
        BuilderBase builder = rail.info.getBuilder(world);
        if (builder instanceof BuilderSwitch) {
            builder = rail.info.withSettings(mutable -> mutable.type = TrackItems.STRAIGHT).getBuilder(world);
        }

        if (!(builder instanceof BuilderCubicCurve curveBuilder)) {
            return null;
        }

        CubicCurve curve = curveBuilder.getCurve();

        Vec3d railOffset = rail.info.placementInfo.placementPosition.add(rail.getPos());
        Vec3d localHit = hit.subtract(railOffset);

        double t = closestT(curve, localHit);
        Vec3d worldPos = curve.position(t).add(railOffset);
        float yaw = VecUtil.toYaw(curve.derivative(t));
        return new VecYPR(worldPos, yaw);
    }

    private static double closestT(CubicCurve curve, Vec3d target) {
        int coarseSamples = 64;
        double bestT = 0;
        double bestDistSq = Double.MAX_VALUE;
        for (int i = 0; i <= coarseSamples; i++) {
            double t = i / (double) coarseSamples;
            double distSq = curve.position(t).distanceToSquared(target);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestT = t;
            }
        }

        double lo = Math.max(0, bestT - 1.0 / coarseSamples);
        double hi = Math.min(1, bestT + 1.0 / coarseSamples);
        for (int i = 0; i < 30; i++) {
            double m1 = lo + (hi - lo) / 3;
            double m2 = hi - (hi - lo) / 3;
            if (curve.position(m1).distanceToSquared(target) <= curve.position(m2).distanceToSquared(target)) {
                hi = m2;
            } else {
                lo = m1;
            }
        }
        return (lo + hi) / 2;
    }

    public static float rightSideYaw(float trackYaw, float playerYawHead) {
        Vec3d perpA = VecUtil.fromYaw(1, trackYaw + 90);
        Vec3d perpB = VecUtil.fromYaw(1, trackYaw - 90);
        Vec3d playerRight = VecUtil.fromWrongYaw(1, playerYawHead + 90);

        return perpA.dotProduct(playerRight) >= perpB.dotProduct(playerRight) ? trackYaw + 90 : trackYaw - 90;
    }
}
