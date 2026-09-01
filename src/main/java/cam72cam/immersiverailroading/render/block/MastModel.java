package cam72cam.immersiverailroading.render.block;

import cam72cam.immersiverailroading.registry.MastDefinition;
import cam72cam.immersiverailroading.tile.OverheadWire;
import cam72cam.immersiverailroading.tile.TileMast;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.Texture;

import java.util.List;

public class MastModel {
    public static StandardModel getModel(TileMast tile) {
        StandardModel model = new StandardModel();

        model.addCustom(((renderState, v) -> {
            renderState.translate(0.5, 0, 0.5);
            renderState.rotate(tile.getAngle(), 0, 1, 0);
            MastDefinition def = tile.getDefinition();
            List<String> toBeRendered = def.model.groups().stream().filter(g -> !g.contains("CONNECTION_")).toList();
            try (ModelRenderer.Binding bound = ModelRenderer.getRendererFor(def.model).bind(renderState)) {
                bound.enqueueOpaque(toBeRendered);
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
