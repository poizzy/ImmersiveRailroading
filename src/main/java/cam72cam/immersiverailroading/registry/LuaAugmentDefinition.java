package cam72cam.immersiverailroading.registry;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.luaj.vm2.Globals;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.gui.RailAugmentGUI;
import cam72cam.immersiverailroading.util.CAML;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.util.JSON;
import cam72cam.mod.resource.Identifier;

public class LuaAugmentDefinition {

    public static List<RailAugmentGUI.ScriptDef> scriptDef = new ArrayList<>();

    public static Map<RailAugmentGUI.ScriptDef, Globals> globals = new HashMap<>();

    public static void loadJsonData() {
        try {
            List<DataBlock> blocks = new ArrayList<>();

            Identifier JsonIdent =
                    new Identifier(ImmersiveRailroading.MODID, "lua_scripts/scripts.json");
            List<InputStream> inputs = JsonIdent.getResourceStreamAll();
            for (InputStream stream : inputs) {
                blocks.add(JSON.parse(stream));
            }

            Identifier CamlIdent =
                    new Identifier(ImmersiveRailroading.MODID, "lua_scripts/scripts.caml");
            inputs = CamlIdent.getResourceStreamAll();
            for (InputStream stream : inputs) {
                blocks.add(CAML.parse(stream));
            }

            for (DataBlock block : blocks) {
                List<DataBlock> scripts = block.getBlocks("scripts");
                if (scripts != null) {
                    for (DataBlock entry : scripts) {
                        RailAugmentGUI.ScriptDef def =
                                new RailAugmentGUI.ScriptDef(entry.getValue("name").asString(),
                                        entry.getValue("script").asIdentifier());
                        if (entry.getValue("description").asString() != null) {
                            def.setDesc(entry.getValue("description").asString());
                        }
                        if (entry.getValues("add_scripts") != null) {
                            def.setAdditional(entry.getValues("add_scripts").stream()
                                    .map(DataBlock.Value::asString).collect(Collectors.toList()));
                        }
                        scriptDef.add(def);
                    }
                }
            }
            scriptDef = scriptDef.stream().distinct().collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
