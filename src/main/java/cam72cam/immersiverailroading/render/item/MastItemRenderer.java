package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.ItemMast;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.MastDefinition;
import cam72cam.immersiverailroading.render.block.MastModel;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.*;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.world.World;

import java.util.List;

public class MastItemRenderer implements ItemRender.IItemModel {
    @Override
    public StandardModel getModel(World world, ItemStack itemStack) {
        MastDefinition def = DefinitionManager.getMast(new ItemMast.Data(itemStack).defID);
        Model model = def.model;
        List<String> toBeRendered = model.groups().stream().filter(s -> !s.contains("CONNECTOR")).toList();
        return new StandardModel().addCustom((renderState, v) -> {
            renderState.scale(0.5, 0.5, 0.5);
            try (ModelRenderer.Binding vbo = ModelRenderer.getRendererFor(model).bind(renderState)) {
                vbo.enqueueOpaque(toBeRendered);
            }
        });
    }

    public static void renderMouseover(Player player, ItemStack stack, Vec3i pos, Vec3d hit, RenderState state, float ignoredPartialTicks) {
        World world = player.getWorld();
        ItemMast.Data data = new ItemMast.Data(stack);
        float localOff = data.getDistance();
        Vec3d renderOff = new Vec3d(pos);
        float rotation = (-(Math.round(player.getRotationYawHead() / 15) * 15) - 90);

        if (BlockUtil.isIRRail(world, pos)) {
            TileRailBase rail = world.getBlockEntity(pos, TileRailBase.class);
            TileRail parent = rail instanceof TileRail tr ? tr : rail.getParentTile();

            VecYPR onTrack = parent != null && parent.info != null ? getClosestPointOnTrack(world, parent, hit) : null;
            if (onTrack != null) {
                int offset = 2;

                rotation = rightSideYaw(onTrack.getYaw(), player.getRotationYawHead());
                Vec3d off = VecUtil.fromYaw(offset + localOff, rotation);
                renderOff = new Vec3d(new Vec3i(onTrack.x, onTrack.y, onTrack.z)).add(off);
            }
        }


        Model model = DefinitionManager.getMast(data.defID).model;

        Vec3d cameraPos = GlobalRender.getCameraPos(ignoredPartialTicks);
        renderOff = renderOff.add(0.5, 0, 0.5).subtract(cameraPos);

        MastModel.renderMast(renderOff, state, model, rotation);
    }

    private static VecYPR getClosestPointOnTrack(World world, TileRail rail, Vec3d hit) {
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

    private static float rightSideYaw(float trackYaw, float playerYawHead) {
        Vec3d perpA = VecUtil.fromYaw(1, trackYaw + 90);
        Vec3d perpB = VecUtil.fromYaw(1, trackYaw - 90);
        Vec3d playerRight = VecUtil.fromWrongYaw(1, playerYawHead + 90);

        return perpA.dotProduct(playerRight) >= perpB.dotProduct(playerRight) ? trackYaw + 90 : trackYaw - 90;
    }
}
