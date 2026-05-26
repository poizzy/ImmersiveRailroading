package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.gui.helpers.MouseHelper;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.registry.LuaAugmentDefinition;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nullable;

import static cam72cam.immersiverailroading.gui.ClickListHelper.next;

public class RailAugmentGUI implements IScreen {
    private final Vec3i pos;
    private final TileRailBase tileRailBase;
    private final Augment augment;
    private final Augment.Properties properties;
    
    private TextField includeTags;
    private TextField excludeTags;
    private TextField doorActuatorFilter;
    private Button stockDetectorMode;
    private Button redstoneMode;
    private CheckBox pushpull;
    private Button couplerMode;
    private Button locoControlMode;
    private TextField luaSearchField;
    private Button luaDropdownToggle;
    
    private boolean isLuaDropdownOpen = false;
    private List<ScriptDef> filteredScripts;
    private int selectedLuaItemIndex = -1;
    private List<ScriptDef> scriptDef = new ArrayList<>();
    private ScriptDef selectedScript;
    private String lastLuaInput = "";
    
    private int guiLeft, guiTop;
    private static final int DROPDOWN_HEIGHT = 80;
    private static final int SCRIPT_LINE_HEIGHT = 12;

    public RailAugmentGUI(TileRailBase tileRailBase) {
        this.pos = tileRailBase.getPos();
        this.augment = tileRailBase.getAugment();
        this.properties = tileRailBase.getAugmentProperties() == null
                          ? Augment.Properties.empty()
                          : tileRailBase.getAugmentProperties();
        scriptDef = LuaAugmentDefinition.scriptDef;
        if (tileRailBase.selectedScript != null) {
            scriptDef.forEach(s -> {
                if (s.equals(tileRailBase.selectedScript)) {
                    this.selectedScript = s;
                }
            });
        }
        this.tileRailBase = tileRailBase;
        filteredScripts = new ArrayList<>(scriptDef);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void init(IScreenBuilder screen) {
        if (!this.augment.equals(Augment.LUA_SCRIPTER)) {
            int xtop = -GUIHelpers.getScreenWidth() / 2;
            int ytop = -GUIHelpers.getScreenHeight() / 4;

            int xOffset = 0;
            int yOffset = 40;

            int buttonWidth = 220;
            int buttonHeight = 20;

            doorActuatorFilter = new TextField(screen, xtop + xOffset + 240, ytop + yOffset, buttonWidth - 50, buttonHeight);
            doorActuatorFilter.setText(properties.doorActuatorFilter);
            doorActuatorFilter.setVisible(augment.equals(Augment.ACTUATOR));
            doorActuatorFilter.setValidator(s -> {
                properties.doorActuatorFilter = s;
                return true;
            });

            includeTags = new TextField(screen, xtop + xOffset, ytop + yOffset, buttonWidth-1, buttonHeight);
            includeTags.setText(properties.positiveFilter);
            includeTags.setValidator(s -> {
                properties.positiveFilter = s;
                return true;
            });
            yOffset += 40;

            excludeTags = new TextField(screen, xtop + xOffset, ytop + yOffset, buttonWidth-1, buttonHeight);
            excludeTags.setText(properties.negativeFilter);
            excludeTags.setValidator(s -> {
                properties.negativeFilter = s;
                return true;
            });
            yOffset += 25;

            Function<Enum<?>, String> translate = e -> TextUtil.translate(e.toString());

            stockDetectorMode = new Button(screen, xtop + xOffset, ytop + yOffset, buttonWidth, buttonHeight, GuiText.SELECTOR_AUGMENT_DETECT + translate.apply(properties.stockDetectorMode)) {
                @Override
                public void onClick(Player.Hand hand) {
                    properties.stockDetectorMode = next(properties.stockDetectorMode, Player.Hand.PRIMARY);
                    stockDetectorMode.setText(GuiText.SELECTOR_AUGMENT_DETECT + translate.apply(properties.stockDetectorMode));
                }
            };
            stockDetectorMode.setEnabled(this.augment == Augment.DETECTOR);
            yOffset += 25;

            redstoneMode = new Button(screen, xtop + xOffset, ytop + yOffset, buttonWidth, buttonHeight, GuiText.SELECTOR_AUGMENT_REDSTONE + translate.apply(properties.redstoneMode)) {
                @Override
                public void onClick(Player.Hand hand) {
                    properties.redstoneMode = next(properties.redstoneMode, Player.Hand.PRIMARY);
                    redstoneMode.setText(GuiText.SELECTOR_AUGMENT_REDSTONE + translate.apply(properties.redstoneMode));
                }
            };
            redstoneMode.setEnabled(this.augment == Augment.COUPLER
                                    || this.augment == Augment.ITEM_LOADER
                                    || this.augment == Augment.ITEM_UNLOADER
                                    || this.augment == Augment.FLUID_LOADER
                                    || this.augment == Augment.FLUID_UNLOADER);
            yOffset += 25;

            pushpull = new CheckBox(screen, xtop + xOffset, ytop + yOffset, GuiText.SELECTOR_AUGMENT_PUSHPULL.toString(), properties.pushpull) {
                @Override
                public void onClick(Player.Hand hand) {
                    properties.pushpull = !properties.pushpull;
                    pushpull.setChecked(properties.pushpull);
                }
            };
            pushpull.setEnabled(this.augment == Augment.COUPLER
                                || this.augment == Augment.ITEM_LOADER
                                || this.augment == Augment.ITEM_UNLOADER
                                || this.augment == Augment.FLUID_LOADER
                                || this.augment == Augment.FLUID_UNLOADER);
            yOffset += 15;

            couplerMode = new Button(screen, xtop + xOffset, ytop + yOffset, buttonWidth, buttonHeight, GuiText.SELECTOR_AUGMENT_COUPLER + translate.apply(properties.couplerAugmentMode)) {
                @Override
                public void onClick(Player.Hand hand) {
                    properties.couplerAugmentMode = next(properties.couplerAugmentMode, Player.Hand.PRIMARY);
                    couplerMode.setText(GuiText.SELECTOR_AUGMENT_COUPLER + translate.apply(properties.couplerAugmentMode));
                }
            };
            couplerMode.setEnabled(this.augment == Augment.COUPLER);
            yOffset += 25;

            locoControlMode = new Button(screen, xtop + xOffset, ytop + yOffset, buttonWidth, buttonHeight, GuiText.SELECTOR_AUGMENT_CONTROL + translate.apply(properties.locoControlMode)) {
                @Override
                public void onClick(Player.Hand hand) {
                    properties.locoControlMode = next(properties.locoControlMode, Player.Hand.PRIMARY);
                    locoControlMode.setText(GuiText.SELECTOR_AUGMENT_CONTROL + translate.apply(properties.locoControlMode));
                }
            };
            locoControlMode.setEnabled(this.augment == Augment.LOCO_CONTROL);
        } else {
            guiLeft = (int) (GUIHelpers.getScreenWidth() / 1.5);
            guiTop = (GUIHelpers.getScreenHeight() / 4);

            luaSearchField = new TextField(screen, guiLeft - (GUIHelpers.getScreenWidth() / 2), guiTop - (guiTop / 45), 120, 14);

            luaDropdownToggle = new Button(screen, guiLeft - (GUIHelpers.getScreenWidth() / 2) + (120 - 14), guiTop - (guiTop / 45), 14, 14, "▼") {
                @Override
                public void onClick(Player.Hand hand) {
                    isLuaDropdownOpen = !isLuaDropdownOpen;
                }
            };

            if (this.selectedScript != null) {
                luaSearchField.setText(this.selectedScript.name);
            }
        }
    }
    
    public void updateFilter() {
        String query = luaSearchField.getText().toLowerCase();
        if (query.isEmpty()) {
            filteredScripts = new ArrayList<>(scriptDef);
        } else {
            filteredScripts = new ArrayList<>();
            for (ScriptDef def : scriptDef) {
                if (def.name != null && def.name.toLowerCase().contains(query)) {
                    filteredScripts.add(def);
                }
            }
        }
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {
        builder.close();
    }

    @Override
    public void onClose() {
        if (!this.augment.equals(Augment.LUA_SCRIPTER)) {
            if(properties.positiveFilter == null) {
                properties.positiveFilter = "";
            }
            if (properties.negativeFilter == null) {
                properties.negativeFilter = "";
            }
            if (properties.doorActuatorFilter == null) {
                properties.doorActuatorFilter = "";
            }
            new AugmentFilterChangePacket(pos, properties).sendToServer();
        } else {
            if (selectedScript != null) {
                tileRailBase.setSelectedScript(selectedScript);
                new TileRailBase.AugmentPacket(tileRailBase, this.selectedScript).sendToServer();
            }
        }
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        IScreen.super.draw(builder, state);

        if (!this.augment.equals(Augment.LUA_SCRIPTER)) {
            GUIHelpers.drawRect(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0x88000000);

            GUIHelpers.drawRect(0, 0, 220, GUIHelpers.getScreenHeight(), 0xCC000000);

            @SuppressWarnings("unused")
            int xtop = -GUIHelpers.getScreenWidth() / 2;
            @SuppressWarnings("unused")
            int ytop = -GUIHelpers.getScreenHeight() / 4;

            int xOffset = 110;
            int yOffset = 30;
            GUIHelpers.drawCenteredString(GuiText.LABEL_CURRENT_AUGMENT + this.augment.toString(), xOffset,  10, 0xFFFFFFFF);

            GUIHelpers.drawCenteredString(GuiText.LABEL_INCLUDED_TAG.toString(), xOffset,  yOffset, 0xFFFFFFFF);
            includeTags.setText(properties.positiveFilter);
            if (augment == Augment.ACTUATOR) {
                GUIHelpers.drawCenteredString(GuiText.LABEL_ACTUATOR_FILTER.toString(), xOffset + 210, yOffset, 0xFFFFFFFF);
                doorActuatorFilter.setText(properties.doorActuatorFilter);
            }
            yOffset+=40;

            GUIHelpers.drawCenteredString(GuiText.LABEL_EXCLUDED_TAG.toString(), xOffset,  yOffset, 0xFFFFFFFF);
            excludeTags.setText(properties.negativeFilter);
        } else {
            if (!luaSearchField.getText().equals(lastLuaInput)) {
                updateFilter();
                lastLuaInput = luaSearchField.getText();
            }
            
            GUIHelpers.drawRect(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0xCC000000);
            state.depth_test(true);

            GUIHelpers.drawCenteredString("Selected lua Script for this Augment", guiLeft / 2, guiTop + (GUIHelpers.getScreenHeight() / 4), 0xFFD3D3D3);

            if (selectedScript != null) {
                String[] lines = selectedScript.desc.split("\n");
                int linePx = 10;
                for (String string : lines) {
                    GUIHelpers.drawCenteredString(string, (GUIHelpers.getScreenWidth() / 2), linePx, 0xFFD3D3D3);
                    linePx += 10;
                }
            }

            if (isLuaDropdownOpen) {
                luaDropdownToggle.setText("▲");

                int dropdownX = guiLeft;
                int dropdownY = guiTop + (GUIHelpers.getScreenHeight() / 4) + 14;
                int dropdownWidth = 120;

                GUIHelpers.drawRect(dropdownX, dropdownY, dropdownWidth, DROPDOWN_HEIGHT, 0xFF505050);

                int visibleScripts = DROPDOWN_HEIGHT / SCRIPT_LINE_HEIGHT;
                for (int i = 0; i < Math.min(filteredScripts.size(), visibleScripts); i++) {
                    String string = filteredScripts.get(i).name;
                    int lineY = dropdownY + i * SCRIPT_LINE_HEIGHT;

                    if (MouseHelper.mouseY >= lineY && MouseHelper.mouseY < lineY + SCRIPT_LINE_HEIGHT && MouseHelper.mouseX >= dropdownX && MouseHelper.mouseX < dropdownX + dropdownWidth) {
                        GUIHelpers.drawRect(dropdownX, lineY,  dropdownWidth, SCRIPT_LINE_HEIGHT, 0xFF808080);
                    }

                    GUIHelpers.drawCenteredString(string, (dropdownX + 2) + dropdownWidth / 2, lineY + 2, 0xFFFFFF);
                }
            } else {
                luaDropdownToggle.setText("▼");
            }

            if (MouseHelper.clicked) {
                this.mouseClicked(MouseHelper.mouseClickedX, MouseHelper.mouseClickedY, MouseHelper.button);
            }
        }
    }
    
    private void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isLuaDropdownOpen) {
            int dropdownX = guiLeft;
            int dropdownY = guiTop + (GUIHelpers.getScreenHeight() / 4) + 14;
            int dropdownWidth = 120;

            if (mouseX >= dropdownX && mouseX < dropdownX + dropdownWidth && mouseY >= dropdownY && mouseY < dropdownY + DROPDOWN_HEIGHT && mouseButton == 0) {
                int index = (mouseY - dropdownY) / SCRIPT_LINE_HEIGHT;

                if (index >=  0 && index < filteredScripts.size()) {
                    selectedLuaItemIndex = index;

                    selectedScript = filteredScripts.get(selectedLuaItemIndex);

                    luaSearchField.setText(selectedScript.name);

                    isLuaDropdownOpen = false;
                }
            }
        }
    }

    public static class AugmentFilterChangePacket extends Packet {
        @TagField
        Vec3i pos;

        @TagField
        Augment.Properties properties;

        public AugmentFilterChangePacket() {
        }

        public AugmentFilterChangePacket(Vec3i pos, Augment.Properties filter) {
            this.pos = pos;
            this.properties = filter;
        }

        @Override
        protected void handle() {
            TileRailBase railBase = this.getWorld().getBlockEntity(pos, TileRailBase.class);
            if (railBase != null && railBase.getAugment() != null) {
                railBase.setAugmentProperties(properties);
            }
        }
    }
    
    public static class ScriptDef {
        public String name;
        public Identifier script;
        public String desc;
        public List<String> additional;

        public ScriptDef(){}

        public ScriptDef(String name, Identifier script) {
            this.name = name;
            this.script = script;
        }

        public ScriptDef setAdditional(@Nullable List<String> additional) {
            this.additional = additional;
            return this;
        }

        public ScriptDef setDesc(@Nullable String desc) {
            this.desc = desc;
            return this;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            ScriptDef scriptDef = (ScriptDef) object;
            return Objects.equals(name, scriptDef.name) && Objects.equals(script, scriptDef.script) && Objects.equals(desc, scriptDef.desc) && Objects.equals(additional, scriptDef.additional);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, script, desc, additional);
        }
    }
}
