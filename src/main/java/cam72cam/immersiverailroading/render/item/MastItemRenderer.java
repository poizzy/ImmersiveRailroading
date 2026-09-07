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

            VecYPR onTrack = parent != null && parent.info != null ? MastSnappingUtil.getClosestPointOnTrack(world, parent, hit) : null;
            if (onTrack != null) {
                int offset = 2;

                rotation = MastSnappingUtil.rightSideYaw(onTrack.getYaw(), player.getRotationYawHead());
                Vec3d off = VecUtil.fromYaw(offset + localOff, rotation);
                renderOff = new Vec3d(new Vec3i(onTrack.x, onTrack.y, onTrack.z)).add(off);
            }
        }


        Model model = DefinitionManager.getMast(data.defID).model;

        Vec3d cameraPos = GlobalRender.getCameraPos(ignoredPartialTicks);
        renderOff = renderOff.add(0.5, 0, 0.5).subtract(cameraPos);

        MastModel.renderMast(renderOff, state, model, rotation);
    }
}
