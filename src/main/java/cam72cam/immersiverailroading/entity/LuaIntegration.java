package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.mod.ModCore;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class LuaIntegration extends EntityCoupleableRollingStock {

    private Globals globals;
    private LuaValue controlPositionEvent;
    private boolean isLuaLoaded = false;

    public boolean LoadLuaFile() throws IOException {
        if (!isLuaLoaded) {
            globals = JsePlatform.standardGlobals();

            ModCore.info("Definition SubClass: " + getDefinition().script.toString());


            // Get Lua file from Json
            Identifier script = getDefinition().script;
            Identifier identifier = new Identifier(script.getDomain(), script.getPath());
            InputStream inputStream = identifier.getResourceStream();

            if (inputStream == null) {
                ModCore.error(String.format("File %s does not exist", script.getDomain() + ":" + script.getPath()));
                return true;
            }

            String luaScript = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if (luaScript == null || luaScript.isEmpty()) {
                ModCore.error("Lua script content is empty | file not found");
                return true;
            }

            LuaValue chunk = globals.load(luaScript);
            chunk.call();

            controlPositionEvent = globals.get("controlPositionEvent");
            if (controlPositionEvent.isnil()) {
                ModCore.error("Lua function 'controlPositionEvent' is not defined");
                return true;
            }

            isLuaLoaded = true;
            ModCore.info("Lua environment initialized and script loaded successfully");
        }
        return false;
    }

    public void getControlGroupLua(Control<?> control, float val, Map<String, Pair<Boolean, Float>> controlPositions) {
        LuaValue result = controlPositionEvent.call(LuaValue.valueOf(control.controlGroup), LuaValue.valueOf(val));
        String result_debug = String.valueOf(controlPositionEvent.call(LuaValue.valueOf(control.controlGroup), LuaValue.valueOf(val)));
        ModCore.info("results: " + result_debug);
        if (result.istable()) {
            LuaTable table = result.checktable();
            ModCore.info("Lua return is a table");

            for (LuaValue key : table.keys()) {
                LuaValue value = table.get(key);

                String controlName = key.toString();
                Float newVal = value.tofloat();


                ModCore.info("Key: " + controlName + ", Value: " + newVal);

                // Add to the Java map
                controlPositions.put(controlName, Pair.of(false, newVal));

                ModCore.info(controlPositions.toString());
            }
        } else {
            ModCore.error("Result is not a table. Type: " + result.typename());
        }
    }
}
