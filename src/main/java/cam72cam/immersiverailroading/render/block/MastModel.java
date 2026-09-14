package cam72cam.immersiverailroading.render.block;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.library.MastConnector;
import cam72cam.immersiverailroading.registry.MastDefinition;
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

import javax.annotation.Nullable;
import java.util.*;

public class MastModel {

    public static StandardModel getModel(TileMast tile) {
        StandardModel model = new StandardModel();

        MastDefinition definition = tile.getDefinition();
        Vec3d blockOffset = new Vec3d(0.5, 0, 0.5);
        float rot = tile.getAngle();
        Vec3d offset = blockOffset.add(tile.getOffset());

        model.addCustom(((renderState, v) -> {
            renderMast(offset, renderState, definition, rot, tile);
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

    public static void renderMast(Vec3d offset, RenderState renderState, MastDefinition definition, float rotationYaw, @Nullable TileMast tm) {
        renderState.translate(offset);
        renderState.rotate(rotationYaw, 0, 1, 0);
        Model mast = definition.model;
        List<String> toBeRendered = mast.groups().stream().filter(g -> !g.contains("CONNECTOR_")).toList();
        try (ModelRenderer.Binding bound = ModelRenderer.getRendererFor(mast).bind(renderState)) {
            bound.enqueueOpaque(toBeRendered);
        }

        Player player = MinecraftClient.getPlayer();
        if (tm != null && player.getHeldItem(Player.Hand.PRIMARY).is(IRItems.ITEM_WIRE)) {
            Vec3d localEyes = player.getPositionEyes()
                    .subtract(new Vec3d(tm.getPos()))
                    .subtract(0.5, 0, 0.5)
                    .rotateYaw(-tm.getAngle());
            Vec3d localLook = player.getLookVector().rotateYaw(-tm.getAngle());
            for (MastConnector connector : definition.connectors.values()) {
                boolean hit = connector.getBoundingBox().intersectsSegment(localEyes, localEyes.add(localLook.scale(10)));
                RenderState previewState = renderState.clone();
                previewState.blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA)).lighting(false);

                if (hit) {
                    previewState.color(0, 1, 0, 0.4f);
                } else {
                    previewState.color(1, 0, 0, 0.4f);
                }

                connector.renderPreview(previewState);
            }
        }
    }
}
