package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.gui.overlay.ReadoutsEventHandler;
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
import java.util.*;

public abstract class LuaIntegration extends EntityCoupleableRollingStock implements ReadoutsEventHandler {

    private Globals globals;
    private LuaValue controlPositionEvent;
    private boolean isLuaLoaded = false;
    private LuaValue readoutEventHandler;
    private LuaValue textureEventHandler;

    @Override
    public void onTick() {
        if (getDefinition().script != null) {
            try {
                if (LoadLuaFile()) return;
                getControlGroup();
                getReadout();
                textureEvent();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            super.onTick();
        }
    }

    public boolean LoadLuaFile() throws IOException {
        if (!isLuaLoaded) {
            globals = JsePlatform.standardGlobals();

//            ModCore.info("Definition SubClass: " + getDefinition().script.toString());


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

            readoutEventHandler = globals.get("readoutEventHandler");
            if (controlPositionEvent.isnil()) {
                ModCore.error("Lua function 'readoutEvent' is not defined");
                return true;
            }

            textureEventHandler = globals.get("textureEventHandler");
            if (controlPositionEvent.isnil()) {
                ModCore.error("Lua function 'readoutEvent' is not defined");
                return true;
            }

            isLuaLoaded = true;
            ModCore.info("Lua environment initialized and script loaded successfully");
        }
        return false;
    }

    @Override
    public void readoutEvent(Readouts readout, float oldVal, float newVal) {
        try {
//            ModCore.info("readoutEvent" + readout + " | " + oldVal + " | " + newVal);
            readoutEventHandler.call(LuaValue.valueOf(readout.toString()), LuaValue.valueOf(newVal));
//            LuaValue result = controlPositionEvent.call();
//            putLuaValue(result);
        }catch (Exception e) {
        }
    }

    private void putLuaValue(LuaValue result) {
        if (result.istable()) {
            LuaTable table = result.checktable();

            for (LuaValue key : table.keys()) {
                LuaValue value = table.get(key);

                String controlName = key.toString();
                Float newValControl = value.tofloat();

                if (controlName.equals("THROTTLE")) {
                    setThrottleLua(newValControl);
                }else {
//                ModCore.info("Key: " + controlName + ", Value: " + newValControl);
                    controlPositions.put(controlName, Pair.of(false, newValControl));
                }
            }
        } else {
//            ModCore.error("Result is not a table. Type: " + result.typename());
        }
    }

    public void textureEvent() {
        if (textureEventHandler == null) {
            ModCore.error("textureEventHandler is null");
            return;
        }

        String currentTexture = getTexture();
        if (currentTexture == null) {
            currentTexture = "Default";
        }

        LuaValue texture = textureEventHandler.call(LuaValue.valueOf(currentTexture));

        if (texture == null) {
            return;
        }

        try {
            setTexture(texture.tojstring());
        } catch (Exception e) {
            ModCore.error(String.format("Failed to set texture '%s'. Error: %s", texture.tojstring(), e.getMessage()));
        }
    }

    private void getControlGroup() {
        List<String> controlGroup = getDefinition().controlGroup;
        List<Map<String, Float>> resultList = new ArrayList<>();
        Map<String, Float> controlMap = new HashMap<>();

        for (String control : controlGroup) {
            Float position = getControlPosition(control);
            controlMap.put(control, position);
        }
        resultList.add(controlMap);
//        ModCore.info(controlMap.toString());
        LuaTable luaTable = convertToLuaTable(resultList);
//        ModCore.info(resultList.toString());
        LuaValue result = controlPositionEvent.call(luaTable);
        putLuaValue(result);
    }

    public LuaTable convertToLuaTable(List<Map<String, Float>> resultList) {
        LuaTable luaResultList = new LuaTable();

        for (int i = 0; i < resultList.size(); i++) {
            LuaTable luaMap = new LuaTable();
            for (Map.Entry<String, Float> entry : resultList.get(i).entrySet()) {
                luaMap.set(entry.getKey(), LuaValue.valueOf(entry.getValue()));
            }
            luaResultList.set(i + 1, luaMap);
        }

        return luaResultList;
    }

    public void setThrottleLua(float val) {

    }

}
