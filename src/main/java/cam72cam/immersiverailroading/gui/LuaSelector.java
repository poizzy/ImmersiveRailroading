package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.registry.LuaAugmentDefinition;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import javax.annotation.Nullable;
import java.util.*;

public class LuaSelector extends AbstractSearchGui<LuaSelector.ScriptDef> {
    private final TileRailBase tileRailBase;

    public LuaSelector(TileRailBase tile) {
        super();
        this.candidate = new ArrayList<>(LuaAugmentDefinition.scriptDef);
        if (tile.selectedScript != null) {
            candidate.forEach(s -> {
                if (s.equals(tile.selectedScript)) {
                    this.current = s;
                }
            });
        }
        this.tooltip = "Selected lua Script for this Augment";

        this.tileRailBase = tile;
        this.filtered = new ArrayList<>(this.candidate);
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {
        builder.close();
    }

    @Override
    public void onClose() {
        if (current != null) {
            tileRailBase.setSelectedScript(current);
            new TileRailBase.AugmentPacket(tileRailBase, this.current).sendToServer();
        }
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        super.draw(builder, state);

        if (current != null) {
            String[] lines = current.desc.split("\n");
            int linePx = 10;
            for (String string : lines) {
                GUIHelpers.drawCenteredString(string, (GUIHelpers.getScreenWidth() / 2), linePx, 0xFFD3D3D3);
                linePx += 10;
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
        public String toString() {
            return this.name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, script, desc, additional);
        }
    }
}
