package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.*;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import org.apache.commons.lang3.tuple.Pair;

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

    public record SnapInfo(Vec3i blockPos, Vec3d offset, float rotation) {
        public Vec3d getPosition() {
            return offset().add(blockPos());
        }
    }

    public static SnapInfo getPlacement(World world, Player player, Vec3i posIn, Vec3d localOff) {
        TileRailBase rail = world.getBlockEntity(posIn, TileRailBase.class);
        TileRail parent = rail instanceof TileRail tr ? tr : rail.getParentTile();

        // Always use middle
        Vec3d hit = new Vec3d(0.5, 0, 0.5);

        VecYPR onTrack = parent != null && parent.info != null ? MastSnappingUtil.getClosestPointOnTrack(world, parent, new Vec3d(posIn).add(hit)) : null;
        if (onTrack == null) return null;

        int offset = 2;

        float rotation = MastSnappingUtil.rightSideYaw(onTrack.getYaw(), player.getRotationYawHead());
        Vec3d off = VecUtil.fromYaw(offset, rotation).add(localOff.rotateYaw(rotation));
        Vec3d blockPos = new Vec3d(onTrack.x, onTrack.y, onTrack.z).add(off);

        Vec3d placePosD = new Vec3d(new Vec3i(blockPos));
        double trackYawRad = Math.toRadians(onTrack.getYaw());
        boolean alongIsX = Math.abs(Math.sin(trackYawRad)) >= Math.abs(Math.cos(trackYawRad));
        double snappedX = alongIsX ? placePosD.x + 0.5 : blockPos.x;
        double snappedZ = alongIsX ? blockPos.z : placePosD.z + 0.5;
        Vec3d renderOff = new Vec3d(snappedX, blockPos.y, snappedZ);

        rotation -= 90;
        return computeBlockPos(world, renderOff, rotation, localOff);
    }

    private static SnapInfo computeBlockPos(World world, Vec3d in, float rotation, Vec3d localOff) {
        Vec3i blockPos = new Vec3i(in);
        if (world.isReplaceable(blockPos)) {
            Vec3d offset = in.subtract(blockPos).add(localOff);
            return new SnapInfo(blockPos, offset, rotation);
        }

        float lengthSquared = Float.POSITIVE_INFINITY;
        // Find nearest placeable block
        Vec3i nearest = null;
        for (Facing f : new Facing[]{Facing.NORTH, Facing.EAST, Facing.SOUTH, Facing.WEST}) {
            Vec3i newPos = blockPos.offset(f);
            if (!world.isReplaceable(newPos)) continue;
            float len = VecUtil.distanceSquared(newPos, blockPos);
            if (len < lengthSquared) {
                lengthSquared = len;
                nearest = newPos;
            }
        }

        if (nearest == null) return new SnapInfo(blockPos, localOff, rotation);

        Vec3d offset = in.subtract(nearest).add(localOff);
        return new SnapInfo(nearest, offset, rotation);
    }
}
