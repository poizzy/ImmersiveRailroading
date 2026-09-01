package cam72cam.immersiverailroading.render.block;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.registry.MastDefinition;
import cam72cam.immersiverailroading.tile.OverheadWire;
import cam72cam.immersiverailroading.tile.TileMast;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;

import java.util.*;

public class MastModel {
    public static StandardModel getModel(TileMast tile) {
        StandardModel model = new StandardModel();
        Texture connector = Texture.wrap(new Identifier(ImmersiveRailroading.MODID, "textures/connector.png"));
        List<Vec3i> placedBlocks = new ArrayList<>();

        Map<String, IBoundingBox> boundingBoxes = new HashMap<>();
        MastDefinition def = tile.getDefinition();

        for (String con : def.connectorPos.keySet()) {
            List<String> singleton = Collections.singletonList(con);
            boundingBoxes.put(con, IBoundingBox.from(def.model.minOfGroups(singleton), def.model.maxOfGroups(singleton)));
        }

        model.addCustom(((renderState, v) -> {
            renderState.translate(0.5, 0, 0.5);
            renderState.rotate(tile.getAngle(), 0, 1, 0);
            List<String> toBeRendered = def.model.groups().stream().filter(g -> !g.contains("CONNECTOR_")).toList();
            List<String> connectors = def.model.groups().stream().filter(g -> g.contains("CONNECTOR_")).toList();
            try (ModelRenderer.Binding bound = ModelRenderer.getRendererFor(def.model).bind(renderState)) {
                bound.enqueueOpaque(toBeRendered);

                Player player = MinecraftClient.getPlayer();
                if (player.getHeldItem(Player.Hand.PRIMARY).is(IRItems.ITEM_WIRE)) {
                    bound.enqueueTransparent(connectors, state -> state.texture(connector).blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA)).lighting(false));
                }

                List<Vec3i> removed = new ArrayList<>();

                for (Vec3i placed : placedBlocks) {
                    tile.getWorld().breakBlock(placed, false);
                    removed.add(placed);
                }
                placedBlocks.removeAll(removed);

            }
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
}
