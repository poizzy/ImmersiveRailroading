package cam72cam.immersiverailroading.render.block;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.OverheadWire;
import cam72cam.immersiverailroading.tile.TileMast;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;

import java.util.*;

public class MastModel {
    private static final Texture CONNECTOR = Texture.wrap(new Identifier(ImmersiveRailroading.MODID, "textures/connector.png"));

    public static StandardModel getModel(TileMast tile) {
        StandardModel model = new StandardModel();

        Model mast = tile.getDefinition().model;
        Vec3d blockOffset = new Vec3d(0.5, 0, 0.5);
        float rot = tile.getAngle();
        Vec3d offset = blockOffset.add(tile.getOffset());

        model.addCustom(((renderState, v) -> {
            renderMast(offset, renderState, mast, rot);
        }));

        model.addCustom(((state, _) -> {
            state.cull_face(false);
            state.texture(Texture.NO_TEXTURE);
            for (OverheadWire wire : tile.getWires()) {
                wire.render(state.clone());
            }
        }));
        return model;
    }

    public static void renderMast(Vec3d offset, RenderState renderState, Model mast, float rotationYaw) {
        renderState.translate(offset);
        renderState.rotate(rotationYaw, 0, 1, 0);
        List<String> toBeRendered = mast.groups().stream().filter(g -> !g.contains("CONNECTOR_")).toList();
        List<String> connectors = mast.groups().stream().filter(g -> g.contains("CONNECTOR_")).toList();
        try (ModelRenderer.Binding bound = ModelRenderer.getRendererFor(mast).bind(renderState)) {
            bound.enqueueOpaque(toBeRendered);

            Player player = MinecraftClient.getPlayer();
            if (player.getHeldItem(Player.Hand.PRIMARY).is(IRItems.ITEM_WIRE)) {
                bound.enqueueTransparent(connectors, state -> state.texture(CONNECTOR).blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA)).lighting(false));
            }

        }
    }
}
