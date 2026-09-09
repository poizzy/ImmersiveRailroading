package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemWire;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.WireDefinition;
import cam72cam.immersiverailroading.util.WireBuilder;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;

import java.util.HashMap;
import java.util.Map;

// TODO maybe itemSprite
public class WireItemRenderer implements ItemRender.IItemModel {
    public static final Map<String, DirectDraw> cache = new HashMap<>();

    @Override
    public StandardModel getModel(World world, ItemStack itemStack) {
        return new StandardModel().addCustom(((renderState, _) -> render(renderState, itemStack)));
    }

    public static void render(RenderState state, ItemStack stack) {
        ItemWire.Data data = new ItemWire.Data(stack);
        DirectDraw model = cache.get(data.defID);
        if (model == null) {
            WireDefinition definition = DefinitionManager.getWire(data.defID);
            if (definition == null) return;
            model = WireBuilder.build(definition, new Vec3d(-30, 0, 0));
            // cache.put(data.defID, model);
        }

        state.scale(0.1);

        model.draw(state);
    }

/*
    public Identifier getSpriteKey(ItemStack itemStack) {
        ItemWire.Data data = new ItemWire.Data(itemStack);
        if (data.defID == null) return null;

        return new Identifier(ImmersiveRailroading.MODID, data.defID);
    }


    public StandardModel getSpriteModel(ItemStack itemStack) {
        ItemWire.Data data = new ItemWire.Data(itemStack);
        WireDefinition def = DefinitionManager.getWire(data.defID);

        return new StandardModel().addCustom((renderState, v) -> {
            renderState.translate(0, 0.5, -0.5);
            renderState.scale(0.5);
            renderState.rotate(90, 0, 1, 0);
            DirectDraw model = cache.computeIfAbsent(data.defID, _ -> WireBuilder.build(def, new Vec3d(-10, 0, 0)));
            model.draw(renderState);
        });
    }
*/
}
