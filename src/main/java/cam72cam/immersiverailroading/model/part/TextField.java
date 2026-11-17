package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.font.FontManager;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.TextFieldConfig;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.TextBuilder;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.net.Packet;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.serialization.TagField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextField<ENTITY extends EntityMoveableRollingStock> {
    private final static Pattern regex = Pattern.compile("^TEXTFIELD_([^_]+)");
    public final String name;
    public final TextFieldConfig config;
    public Map<UUID, VBO> model;
    private final TextBuilder builder;

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

        config = definition.textFieldConfig.get(name);

        if (config == null) {
            throw new RuntimeException(String.format("Textfield \"%s\" couldn't be initiated, maybe config/model name mismatch?", name));
        }

        TextFieldConfig.Definition fieldDef = config.getDefinition();

        fieldDef.initRenderData(component.modelIDs.stream().findFirst().get(), model);
        this.builder = new TextBuilder(fieldDef);
        this.model = new HashMap<>();
    }

    public void render(ENTITY stock, RenderState state, float partialTicks) {
        TextFieldConfig.State content = stock.textfieldData.get(name);
        VBO vbo = model.get(stock.getUUID());
        if (content.dirty || vbo == null) {
            update(stock);
            vbo = builder.build(content);
            model.put(stock.getUUID(), vbo);
            content.setDirty(false);
            new DirtyPackage(stock, name, false).sendToServer();
        }

        try (VBO.Binding binding = vbo.bind(state.texture(FontManager.getTexture()))) {
            binding.draw();
        }
    }

    private void update(ENTITY stock) {
        EntityRollingStockDefinition stockDefinition = stock.getDefinition();
        TextFieldConfig.Definition definition = config.getDefinition();
        TextFieldConfig.State state  = config.getState(stock);

        for (String textField : definition.linked) {
            stockDefinition.textFieldConfig.get(textField).getState(stock).copyState(state);
        }
    }

    public static class DirtyPackage extends Packet {
        @TagField
        public boolean dirty;
        @TagField
        public EntityRollingStock stock;
        @TagField
        public String name;

        public DirtyPackage() {}

        public DirtyPackage(EntityRollingStock stock, String name, boolean dirty) {
            this.dirty = dirty;
            this.stock = stock;
            this.name = name;
        }

        @Override
        protected void handle() {
            stock.textfieldData.get(name).setDirty(dirty);
        }
    }
}
