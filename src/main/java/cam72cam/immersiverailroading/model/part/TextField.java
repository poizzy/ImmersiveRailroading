package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.TextFieldConfig;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextField<ENTITY extends EntityMoveableRollingStock> {
    private final static Pattern regex = Pattern.compile("^TEXTFIELD_([^_]+)");
    public final String name;
    public final TextFieldConfig.Immutable config;
    public Map<UUID, VBO> model;

    public static <T extends EntityMoveableRollingStock> List<TextField<T>> get(EntityRollingStockDefinition def, ComponentProvider provider, ModelComponentType type, Model model) {
        return provider.parseAll(type).stream().map(c -> new TextField<T>(def, c, model)).toList();
    }

    public static <T extends EntityMoveableRollingStock> List<TextField<T>> get(EntityRollingStockDefinition def, ComponentProvider provider, ModelComponentType type, Model model, ModelComponentType.ModelPosition pos) {
        return provider.parseAll(type, pos).stream().map(c -> new TextField<T>(def, c, model)).toList();
    }

    public TextField(EntityRollingStockDefinition definition, ModelComponent component, Model model) {
        String object = component.modelIDs.stream().findFirst().orElseThrow();
        Matcher matcher = regex.matcher(object);
        if (!matcher.find()) {
            throw new RuntimeException();
        }
        this.name = matcher.group(1);

        this.config = definition.textFieldConfig.get(name).immutable.get();
        config.initRenderData(component.modelIDs.stream().findFirst().get(), model);

    }

    public void render(ENTITY stock, RenderState state, float partialTicks) {

    }
}
