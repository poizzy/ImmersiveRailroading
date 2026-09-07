package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.ItemMast;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.MastDefinition;
import cam72cam.immersiverailroading.render.block.MastModel;
import cam72cam.immersiverailroading.thirdparty.trackapi.IRPathingData;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.immersiverailroading.tile.TileRailBase;
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

    public static void renderMouseover(Player player, ItemStack stack, Vec3i pos, Vec3d ignoredVec, RenderState state, float ignoredPartialTicks) {
        World world = player.getWorld();
        ItemMast.Data data = new ItemMast.Data(stack);
        float localOff = data.getDistance();
        Vec3d renderOff = new Vec3d(pos);
        float rotation = (-(Math.round(player.getRotationYawHead() / 15) * 15) - 90);

        if (BlockUtil.isIRRail(world, pos)) {
            float yaw = player.getRotationYawHead();

            TileRailBase rail = world.getBlockEntity(pos, TileRailBase.class);
            Vec3d vec = new Vec3d(0.5, 0, 0).rotateYaw(yaw);
            IRPathingData next = new IRPathingData(renderOff.add(vec), 0);
            rail.getNextPosition(next, VecUtil.fromWrongYaw(0.1, yaw), rail.getRenderGauge());
            Vec3d firstPos = next.getUMCPos();
            rail.getNextPosition(next, VecUtil.fromWrongYaw(-0.1, yaw), rail.getRenderGauge());

            int offset = 2;
            Vec3d umcPos = next.getUMCPos();

            rotation = VecUtil.toYaw(umcPos.subtract(firstPos));

            Vec3d off = new Vec3d(offset + localOff, 0, 0).rotateYaw(rotation);

            renderOff = new Vec3d(new Vec3i(umcPos)).add(off);
        }


        Model model = DefinitionManager.getMast(data.defID).model;

        Vec3d cameraPos = GlobalRender.getCameraPos(ignoredPartialTicks);
        renderOff = renderOff.add(0.5, 0, 0.5).subtract(cameraPos);

        MastModel.renderMast(renderOff, state, model, rotation);
    }
}
