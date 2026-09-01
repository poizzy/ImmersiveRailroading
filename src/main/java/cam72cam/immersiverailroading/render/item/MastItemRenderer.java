package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.ItemMast;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.MastDefinition;
import cam72cam.immersiverailroading.registry.WireDefinition;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.world.World;
import cam72cam.immersiverailroading.items.ItemWire.Data;

public class MastItemRenderer implements ItemRender.IItemModel {
    @Override
    public StandardModel getModel(World world, ItemStack itemStack) {
        MastDefinition def = DefinitionManager.getMast(new ItemMast.Data(itemStack).defID);
        return new StandardModel().addCustom((renderState, v) -> {
            renderState.scale(0.5, 0.5, 0.5);
            try (ModelRenderer.Binding vbo = ModelRenderer.getRendererFor(def.model).bind(renderState)) {
                vbo.enqueueOpaque();
            }
        });
    }
}
