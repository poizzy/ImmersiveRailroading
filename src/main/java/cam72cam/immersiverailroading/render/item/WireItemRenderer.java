package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.ItemWire;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.WireDefinition;
import cam72cam.immersiverailroading.util.WireBuilder;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.world.World;

import java.util.HashMap;
import java.util.Map;

public class WireItemRenderer implements ItemRender.IItemModel {
    public static final Map<String, Model> cache = new HashMap<>();

    @Override
    public StandardModel getModel(World world, ItemStack itemStack) {
        return new StandardModel().addCustom(((renderState, _) -> render(renderState, itemStack)));
    }

    public static void render(RenderState state, ItemStack stack) {
        ItemWire.Data data = new ItemWire.Data(stack);
        Model model = cache.computeIfAbsent(data.defID, def -> {
            WireDefinition definition = DefinitionManager.getWire(def);
            if (definition == null) return null;
            return WireBuilder.build(definition, new Vec3d(-15, 0, 0), 3);
        });

        if (model == null) return;

        state.scale(1.0f / 15.0f);
        state.cull_face(false);
        state.texture(Texture.NO_TEXTURE);

        try (ModelRenderer.Binding binding = ModelRenderer.getRendererFor(model).bind(state)) {
            binding.enqueueOpaque();
        }
    }

    @Override
    public void applyTransform(ItemStack stack, ItemRender.ItemRenderType type, RenderState state) {
        ItemRender.IItemModel.defaultTransform(type, state);

        state.translate(1, 0.45, 0);
        state.scale(1, 5, 0);
    }
}
