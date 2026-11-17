package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.font.FontManager;
import cam72cam.immersiverailroading.gui.components.ArrowSelector;
import cam72cam.immersiverailroading.gui.components.DynamicListSelector;
import cam72cam.immersiverailroading.library.TextFieldConfig;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TextFieldGui implements IScreen {
    private TextField color;
    private DynamicListSelector<Identifier> fontSelector;
    private DynamicListSelector<String> objectSelector;
    private EntityRollingStock stock;
    private Button fontButton;
    private TextField input;
    private Identifier selectedFont;
    private double zoom = 1.0d;
    private List<Identifier> availableFonts;

    private String selectedTextField;
    private TextFieldConfig config;

    @Override
    public void init(IScreenBuilder screen) {
        int width = 200;
        int height = 20;
        int spacingHeight = 25;
        int xTop = -GUIHelpers.getScreenWidth() / 2;
        int yTop = -GUIHelpers.getScreenHeight() / 4;

        Entity ent = MinecraftClient.getEntityMouseOver();
        if (ent == null) {
            return;
        }
        stock = ent.as(EntityRollingStock.class);
        EntityRollingStockDefinition stockDefinition = stock.getDefinition();

        List<String> availableTextFields = stockDefinition.textFieldConfig.entrySet().stream().filter(e -> e.getValue().getDefinition().selectable).map(Map.Entry::getKey).toList();
        if (selectedTextField == null) {
            selectedTextField = availableTextFields.getFirst();
        }

        if (config == null) {
            config = stockDefinition.textFieldConfig.get(selectedTextField);
        }

        TextFieldConfig.State state = config.getState(stock);
        TextFieldConfig.Definition definition = config.getDefinition();

        availableFonts = new ArrayList<>(definition.availableFonts);
        if (availableFonts.isEmpty()) {
            availableFonts.add(FontManager.DEFAULT);
        }

        selectedFont = state.font;

        Button objectButton = new Button(screen, xTop + 145 / 4, yTop, width - 55, height, selectedTextField) {
            @Override
            public void onClick(Player.Hand hand) {
                objectSelector.setVisible(!objectSelector.isVisible());
            }
        };

        yTop += spacingHeight;

        input = new TextField(screen, xTop + 62, yTop, width - 54, height);
        input.setText(state.text);
        input.setFocused(true);
        // TODO
        Predicate<String> validator = s -> true;
        input.setValidator(validator);

        yTop += spacingHeight;

        fontButton = new Button(screen, xTop + 60, yTop, width - 50, height, new File(selectedFont.getPath()).getName()) {
            @Override
            public void onClick(Player.Hand hand) {
                if (availableFonts.size() > 1) {
                    fontSelector.setVisible(!fontSelector.isVisible());
                }
            }
        };


        fontButton.setEnabled(availableFonts.size() > 1);

        yTop += spacingHeight;

        color = new TextField(screen, xTop + 62, yTop, width - 149, height);
        color.setText(state.color);

        yTop += spacingHeight;

        Button alignButton = new Button(screen, xTop + 60, yTop, width - 145, height, state.align.name()) {
            @Override
            public void onClick(Player.Hand hand) {
                TextFieldConfig.Align next = state.align.next();
                state.setAlign(next);
                this.setText(next.toString());
            }
        };

        yTop += spacingHeight;

        Button verticalAlign = new Button(screen, xTop + 60, yTop, width - 145, height, state.verticalAlign.name()) {
            @Override
            public void onClick(Player.Hand hand) {
                TextFieldConfig.VerticalAlign next = state.verticalAlign.next();
                state.setVerticalAlign(next);
                this.setText(next.toString());
            }
        };

        yTop += spacingHeight + 1;

        ArrowSelector gap = new ArrowSelector(screen, xTop + 60, yTop, width - 145, height, state.gap, 0, 15) {
            @Override
            public void onUpdate(float val) {
                state.setGap((int) val);
            }
        };

        yTop += spacingHeight + 1;

        ArrowSelector offset = new ArrowSelector(screen, xTop + 60, yTop, width - 145, height, state.offset, 0, 15) {
            @Override
            public void onUpdate(float val) {
                state.setOffset((int) val);
            }
        };

        yTop += spacingHeight + 1;

        ArrowSelector scale = new ArrowSelector(screen, xTop + 60, yTop, width - 145, height, state.scale, 0, 8, 0.5f) {
            @Override
            public void onUpdate(float val) {
                state.setScale(val);
            }
        };


        Slider zoom_slider = new Slider(screen, xTop + width + 15, (int) (GUIHelpers.getScreenHeight() * 0.75 - height), "Zoom: ", 0.1, 3, 1, true) {
            @Override
            public void onSlider() {
                zoom = this.getValue();
            }
        };


        /*
         * Selectors
         */

        fontSelector = new DynamicListSelector<Identifier>(screen, width + 15, 100, height, selectedFont,
                availableFonts.stream().collect(Collectors.toMap(i -> new File(i.getPath()).getName(), g -> g, (u, v) -> u, LinkedHashMap::new))) {
            @Override
            public void onClick(Identifier option) {
                selectedFont = option;
                fontButton.setText(new File(option.getPath()).getName());
                this.setVisible(!this.isVisible());
            }
        };

        objectSelector = new DynamicListSelector<String>(screen,
                width + 15,
                250,
                height,
                selectedTextField,
                availableTextFields.stream().collect(Collectors.toMap(Function.identity(), Function.identity()))) {
            @Override
            public void onClick(String option) {
                selectedTextField = option;
                TextFieldGui.this.init(screen);
            }
        };
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {
        builder.close();
    }

    @Override
    public void onClose() {
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {

        GUIHelpers.drawRect(215, 0, GUIHelpers.getScreenWidth() - 200, GUIHelpers.getScreenHeight(), 0xCC000000);
        GUIHelpers.drawRect(0, 0, 215, GUIHelpers.getScreenHeight(), 0xEE000000);

        int xTop = 5;
        int yTop = 6;
        int height = 25;

        yTop += height;
        GUIHelpers.drawString("Text:", xTop, yTop, 0xFFFFFF);
        yTop += height;
        GUIHelpers.drawString("Font:", xTop, yTop, 0xFFFFFF);
        yTop += height;
        GUIHelpers.drawString("Color:", xTop, yTop, 0xFFFFFF);
        yTop += height;
        GUIHelpers.drawString("Align:", xTop, yTop, 0xFFFFFF);
        yTop += height;
        GUIHelpers.drawString("Vert Align:", xTop, yTop, 0xFFFFFF);
        yTop += height + 1;
        GUIHelpers.drawString("Spacing:", xTop, yTop, 0xFFFFFF);
        yTop += height + 1;
        GUIHelpers.drawString("Offset:", xTop, yTop, 0xFFFFFF);
        yTop += height + 1;
        GUIHelpers.drawString("Scale:", xTop, yTop, 0xFFFFFF);

        TextFieldConfig.State configState = config.getState(stock);

        if (color.getText().matches("^#([A-Fa-f0-9]{6})$") && !color.getText().equals(configState.color)) {
            configState.setColor(color.getText());
        }

        if (!input.getText().equals(configState.text)) {
            configState.setText(input.getText());
        }

        new EntityRollingStock.TextFieldServerSync(stock, selectedTextField, configState).sendToServer();

        StockModel<?, ?> model = stock.getDefinition().getModel();


        int scale = (int) ((double) GUIHelpers.getScreenWidth() / 40 * zoom);
        state.translate(200 + (double) (GUIHelpers.getScreenWidth() - 200) / 2, (double) builder.getHeight() / 2 + 10, 400);
        state.rotate(getRotationFromNormal(config.getDefinition().renderData.normal()), 0, 1, 0);
        state.scale(-scale, -scale, -scale);
        state.lightmap(1, 1);
        state.depth_test(true);

        model.renderEntity((EntityMoveableRollingStock) stock, state, 0);
        model.postRenderEntity((EntityMoveableRollingStock) stock, state, 0);
    }

    private static double getRotationFromNormal(Vec3d normal) {
        Vec3d normalized = normal.normalize().scale(-1);

        double angleRadians = Math.atan2(-normalized.x, normalized.z);

        double angleDegrees = Math.toDegrees(angleRadians);

        if (angleDegrees < 0) {
            angleDegrees += 360;
        }

        return angleDegrees;
    }
}
