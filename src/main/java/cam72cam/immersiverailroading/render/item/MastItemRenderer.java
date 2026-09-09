package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.ItemMast;
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
        double scale = 1.0f / model.maxOfGroups(toBeRendered).y;
        return new StandardModel().addCustom((renderState, v) -> {
            renderState.translate(0.5, 0, 0.5);
            renderState.scale(scale);
            try (ModelRenderer.Binding vbo = ModelRenderer.getRendererFor(model).bind(renderState)) {
                vbo.enqueueOpaque(toBeRendered);
            }
        });
    }

    public static void renderMouseover(Player player, ItemStack stack, Vec3i pos, Vec3d hit, RenderState state, float ignoredPartialTicks) {
        World world = player.getWorld();
        ItemMast.Data data = new ItemMast.Data(stack);
        Vec3d localOff = data.getOffset();
        Vec3d renderOff = new Vec3d(pos).add(0.5, 0, 0.5);
        float rotation = (-(Math.round(player.getRotationYawHead() / 15) * 15) - 90);

        MastSnappingUtil.SnapInfo snapInfo;
        if (BlockUtil.isIRRail(world, pos) && (snapInfo = MastSnappingUtil.getPlacement(world, player, pos, localOff)) != null) {
            renderOff = snapInfo.getPosition();
            rotation = snapInfo.rotation();
        } else {
            renderOff = renderOff.add(localOff.rotateYaw(rotation));
        }


        Model model = DefinitionManager.getMast(data.defID).model;

        Vec3d cameraPos = GlobalRender.getCameraPos(ignoredPartialTicks);
        renderOff = renderOff.subtract(cameraPos);

        MastModel.renderMast(renderOff, state, model, rotation);
    }
}
