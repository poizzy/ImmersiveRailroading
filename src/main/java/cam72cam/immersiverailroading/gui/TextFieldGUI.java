package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.components.ListSelector;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.rail.RailRender;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.gui.screen.TextField;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import org.luaj.vm2.ast.Str;
import util.Matrix4;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class TextFieldGUI implements IScreen {
    long frame;

    private TextField input;
    private TextRenderOptions settings;
    private ListSelector<Font.TextAlign> align;
    private Button alignButton;
    private double zoom = 1;
    private ListSelector<String> textFieldSelector;
    private Button textFieldButton;

    private EntityScriptableRollingStock stock;

    @Override
    public void init(IScreenBuilder screen) {
        int width = 200;
        int height = 20;
        int xtop = -GUIHelpers.getScreenWidth() / 2;
        int ytop = -GUIHelpers.getScreenHeight() / 4;

        Entity ent = MinecraftClient.getEntityMouseOver();
        this.stock = ent.as(EntityScriptableRollingStock.class);
        EntityRollingStockDefinition def = stock.getDefinition();

        if (settings == null) {
            settings = def.textFieldDef.entrySet().iterator().next().getValue();
        }

        textFieldSelector = new ListSelector<String>(screen, width, 250, height,
                settings.componentId,
                def.textFieldDef.entrySet().stream().collect(Collectors.toMap(entry -> entry.getValue().toString(), Map.Entry::getKey, (u, v) -> v, LinkedHashMap::new))) {
            @Override
            public void onClick(String option) {
                settings = def.textFieldDef.get(option);
            }
        };

        input = new TextField(screen, xtop, ytop, width-1, height);
        this.input.setText(settings.newText);
        this.input.setFocused(true);
        ytop += height;

        this.align = new ListSelector<Font.TextAlign>(screen, width, 100, height, settings.align,
                Arrays.stream(Font.TextAlign.values()).collect(Collectors.toMap(Font.TextAlign::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
        ) {
            @Override
            public void onClick(Font.TextAlign option) {
                settings.align = option;
            }
        };

        this.alignButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_ALIGNMENT.toString(settings.align)) {
            @Override
            public void onClick(Player.Hand hand) {
                showSelector(align);
            }
        };

        ytop += height;

        this.textFieldButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_TEXTFIELD.toString(settings.componentId)) {
            @Override
            public void onClick(Player.Hand hand) {
                showSelector(textFieldSelector);
            }
        };
    }

    private void showSelector(ListSelector<?> selector) {
        boolean isVisible = selector.isVisible();

        align.setVisible(false);
        textFieldSelector.setVisible(false);

        selector.setVisible(!isVisible);
    }


    @Override
    public void onEnterKey(IScreenBuilder builder) {
        builder.close();
    }

    @Override
    public void onClose() {
        if (!input.getText().isEmpty()) {
            RenderText renderText = RenderText.getInstance(String.valueOf(stock.getUUID()));

            settings.newText = input.getText();

            File file = new File(settings.id.getPath());
            String jsonPath = file.getName();

            Identifier jsonId = settings.id.getRelative(jsonPath.replaceAll(".png", ".json"));
            InputStream json = null;
            try {
                json = jsonId.getResourceStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            LinkedHashMap<String, OBJGroup> group = stock.getDefinition().getModel().groups;
            for (Map.Entry<String, OBJGroup> entry : group.entrySet()) {
                if (entry.getKey().contains(String.format("TEXTFIELD_%s", settings.componentId))) {
                    EntityRollingStockDefinition.Position getPosition = stock.getDefinition().normals.get(entry.getKey());
                    Vec3d vec3dmin = getVec3dmin(getPosition.vertices);
                    Vec3d vec3dmax = getVec3dmax(getPosition.vertices);
                    Vec3d vec3dNormal = getPosition.normal;
                    renderText.setText(settings.componentId, settings.newText, settings.id, vec3dmin, vec3dmax, json,
                            settings.resX, settings.resY, settings.align, settings.flipped, settings.fontSize, settings.fontX,
                            settings.fontGap, settings.overlay, vec3dNormal, settings.hexCode, settings.fullbright, settings.textureHeight, settings.useAlternative, settings.lineSpacingPixels, settings.offset, entry.getKey());
                }
            }
        }
    }

    private Vec3d getVec3dmin (List<Vec3d> vectors) {
        return vectors.stream().min(Comparator.comparingDouble(Vec3d::length)).orElse(null);
    }

    private Vec3d getVec3dmax (List<Vec3d> vectors) {
        return vectors.stream().max(Comparator.comparingDouble(Vec3d::length)).orElse(null);
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        frame ++;
        GUIHelpers.drawRect(200, 0, GUIHelpers.getScreenWidth() - 200, GUIHelpers.getScreenHeight(), 0xCC000000);
        GUIHelpers.drawRect(0, 0, 200, GUIHelpers.getScreenHeight(), 0xEE000000);

        if(align.isVisible()) {
            double textScale = 1.5;
            GUIHelpers.drawCenteredString(GuiText.SELECTOR_ALIGNMENT.toString(settings.align.toString()),(int) ((300 + (GUIHelpers.getScreenWidth()-300) / 2) / textScale), (int) (10 / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale) );
            double scale = GUIHelpers.getScreenWidth() / 12.0 * zoom;

            state.translate(300 + (GUIHelpers.getScreenWidth() - 300) / 2, builder.getHeight(), 100);
            state.rotate(90, 1, 0, 0);
            state.scale(-scale, scale, scale);
            state.translate(0, 0, 1);
            state.translate(-0.5, 0, -0.5);
            return;
        }

        if(textFieldSelector.isVisible()) {
            double textScale = 1.5;
            GUIHelpers.drawCenteredString(GuiText.SELECTOR_ALIGNMENT.toString(settings.align.toString()),(int) ((300 + (GUIHelpers.getScreenWidth()-300) / 2) / textScale), (int) (10 / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale) );
            double scale = GUIHelpers.getScreenWidth() / 12.0 * zoom;

            state.translate(300 + (GUIHelpers.getScreenWidth() - 300) / 2, builder.getHeight(), 100);
            state.rotate(90, 1, 0, 0);
            state.scale(-scale, scale, scale);
            state.translate(0, 0, 1);
            state.translate(-0.5, 0, -0.5);
            return;
        }
    }
}
