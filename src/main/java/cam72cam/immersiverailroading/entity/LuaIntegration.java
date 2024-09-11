package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.gui.overlay.ReadoutsEventHandler;
import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.util.DataBlock;
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
    private boolean controlPositionEventLoaded = false;
    private boolean readoutEventHandlerLoaded = false;
    private boolean textureEventHandlerLoaded = false;
    private boolean isSleeping;
    private long lastExecutionTime;
    private boolean wakeLuaScriptCalled = false;
    private boolean couplerBoolean;
    private LuaValue changePerformance;
    private boolean changePerformanceLoded;
    private LuaValue changeSound;
    private boolean changeSoundLoaded;

    @Override
    public void onTick() {
        if (getDefinition().script != null) {
            if (!ConfigPerformance.disableLuaScript) {
                if (ConfigPerformance.luaScriptSleep > 0) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - lastExecutionTime;
                    
                    if (!isSleeping && elapsedTime >= ConfigPerformance.luaScriptSleep * 1000L) {
                        isSleeping = true;
                    }

                    if (isSleeping) {
                        if (wakeLuaScriptCalled || getPassengerCount() > 0) {
                            wakeLuaScriptCalled = false;
                            isSleeping = false;
                            lastExecutionTime = currentTime;
                        } else {
                            return;
                        }
                    }
                }

                if (ConfigPerformance.luaScriptSleep == 0) {
                    isSleeping = false;
                    lastExecutionTime = System.currentTimeMillis();
                }


                if (!isSleeping || ConfigPerformance.luaScriptSleep == 0) {

                    try {
                        if (LoadLuaFile()) return;
                        if (controlPositionEventLoaded) {
                            getControlGroup();
                        }
                        if (readoutEventHandlerLoaded) {
                            getReadout();
                        }
                        if (textureEventHandlerLoaded) {
                            textureEvent();
                        }
                        if (changePerformanceLoded) {
                            setChangePerformance();
                        }
                        if (changeSoundLoaded) {
                            setChangeSounds();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        super.onTick();
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
            } else {
                controlPositionEventLoaded = true;
            }

            readoutEventHandler = globals.get("readoutEventHandler");
            if (controlPositionEvent.isnil()) {
                ModCore.error("Lua function 'readoutEvent' is not defined");
            } else {
                readoutEventHandlerLoaded = true;
            }

            textureEventHandler = globals.get("textureEventHandler");
            if (controlPositionEvent.isnil()) {
                ModCore.error("Lua function 'readoutEvent' is not defined");
            } else {
                textureEventHandlerLoaded = true;
            }

            changePerformance = globals.get("changePerformance");
            if (changePerformance.isnil()) {
                ModCore.error("Lua function 'changePerformance' is not defined");
            } else {
                changePerformanceLoded = true;
            }

            changeSound = globals.get("changeSound");
            if (changeSound.isnil()) {
                ModCore.error("Lua function 'changeSounds' is not defined");
            } else {
                changeSoundLoaded = true;
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

                switch (controlName) {
                    case "THROTTLE":
                        setThrottleLua(newValControl);
                        break;
                    case "REVERSER":
                        setReverserLua(newValControl);
                        break;
                    case "TRAIN_BRAKE":
                        setBrakeLua(newValControl);
                        break;
                    case "INDEPENDENT_BRAKE":
                        setIndependentBrakeLua(newValControl);
                        break;
                    case "COUPLER_ENGAGED_FRONT":
                        couplerBoolean = newValControl != 1;
                        setCouplerEngaged(CouplerType.FRONT, couplerBoolean);
                        break;
                    case "COUPLER_ENGAGED_BACK":
                        couplerBoolean = newValControl != 1;
                        setCouplerEngaged(CouplerType.BACK, couplerBoolean);
                        break;
                    default:
                        controlPositions.put(controlName, Pair.of(false, newValControl));
                        break;
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
        Map<String, Float> controls = new HashMap<>();

        controls.put("THROTTLE", getThrottleLua());
        controls.put("REVERSER", getReverserLua());
        controls.put("TRAIN_BRAKE", getTrainBrakeLua());

        for (String control : controlGroup) {
            Float position = getControlPosition(control);
            controlMap.put(control, position);
        }

        resultList.add(controlMap);
        resultList.add(controls);


        LuaTable luaTable = convertToLuaTable(resultList);
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
    
    public void setChangePerformance() {
        LuaValue result = changePerformance.call();
        if (result.istable()) {
            LuaTable table = result.checktable();

            for (LuaValue key : table.keys()) {
                LuaValue value = table.get(key);

                String scriptName = key.toString();
                Double scripValDouble = value.todouble();

                switch (scriptName) {
                    case "max_speed_kmh":
                        getDefinition().setMaxSpeed(scripValDouble);
                        break;
                    case "tractive_effort_lbf":
                        getDefinition().setTraction(scripValDouble);
                        break;
                    case "horsepower":
                        getDefinition().setHorsepower(scripValDouble);
                        break;
                }
            }
        }
    }

    public void setChangeSounds() {
        LuaValue result = changeSound.call();
        List<Map<String, DataBlock.Value>> newSound = new ArrayList<>();

        for (LuaValue key : result.checktable().keys()) {

            LuaValue entry = result.get(key);
            Map<String, DataBlock.Value> soundDefinition = new HashMap<>();

            for(LuaValue entrykey : entry.checktable().keys()) {
                LuaValue value = entry.get(entrykey);
                DataBlock.Value soundDef = new ObjectValue(convertLuaValue(value));
                soundDefinition.put(entrykey.tojstring(), soundDef);
            }
            newSound.add(soundDefinition);
        }
        getDefinition().setSounds(newSound);
    }

    private static Object convertLuaValue(LuaValue value) {
        if (value.isboolean()) {
            return value.toboolean();
        } else if (value.isnumber()) {
            return value.todouble();
        } else if (value.isstring()) {
            return value.tojstring();
        } else if (value.istable()) {
            Map<String, Object> nestedMap = new HashMap<>();
            for (LuaValue key : value.checktable().keys()) {
                LuaValue val = value.get(key);
                nestedMap.put(key.tojstring(), convertLuaValue(val));
            }
            return nestedMap;
        } else {
            return value; // Return raw LuaValue if type is unknown
        }
    }

    public void setThrottleLua(float val) {

    }

    public void setReverserLua(float val) {

    }

    public void setBrakeLua(float val) {

    }

    public void setIndependentBrakeLua(float val) {

    }

    public float getThrottleLua() {
        return 0;
    }

    public float getReverserLua() {
        return 0;
    }

    public float getTrainBrakeLua() {
        return 0;
    }

    @Override
    public void wakeLuaScript() {
        super.wakeLuaScript();
        wakeLuaScriptCalled = true;
    }
}
