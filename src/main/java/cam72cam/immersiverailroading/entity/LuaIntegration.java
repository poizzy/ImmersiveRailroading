package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.gui.overlay.ReadoutsEventHandler;
import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.LuaValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.luaj.vm2.LuaFunction;

public abstract class LuaIntegration extends EntityCoupleableRollingStock implements ReadoutsEventHandler {

    private Globals globals;
    private LuaValue tickEvent;
    private boolean isLuaLoaded = false;
    private boolean isSleeping;
    private long lastExecutionTime;
    private boolean wakeLuaScriptCalled = false;
    LuaTable luaFunction = new LuaTable();
    private Map<String, Float> readoutsMap = new HashMap<>();
    private Map<String, InputStream> moduleMap = new HashMap<>();
    private String oldControl = null;
    private float oldValue;
    private Vec3d vec3d;
    private String oldText;

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
                        callFuction();
                        getReadout();
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

            if (getDefinition().addScripts != null) {
                for (String modules : getDefinition().addScripts) {
                    Identifier newModule = identifier.getRelative(modules);
                    moduleMap.put(modules.replace(".lua", ""), newModule.getResourceStream());
                }
                preloadModules(globals, moduleMap);
            }

            LuaValue chunk = globals.load(luaScript);
            chunk.call();

            setLuaFunctions();

            globals.set("IR", luaFunction);

            tickEvent = globals.get("tickEvent");
            if (tickEvent.isnil()) {
                ModCore.error("Function 'tickEvent' is not Defined!");
            }

            isLuaLoaded = true;
            ModCore.info("Lua environment initialized and script loaded successfully");
        }
        return false;
    }

    private void preloadModules(Globals globals, Map<String, InputStream> moduleStreams) {
        LuaValue packageLib = globals.get("package");
        LuaValue preloadTable = packageLib.get("preload");

        for (Map.Entry<String, InputStream> entry : moduleStreams.entrySet()) {
            String moduleName = entry.getKey();
            InputStream moduleStream = entry.getValue();

            try {
                // Step 5: Load the Lua chunk from the InputStream
                LuaValue chunk = globals.load(moduleStream, moduleName, "bt", globals);

                // Step 6: Add the chunk to package.preload with the module name
                preloadTable.set(moduleName, chunk);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setLuaFunctions() {
        luaFunction.set("getCG", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue control) {
                float result = getControlGroup(control.tojstring());
                return LuaValue.valueOf(result);
            }
        });
        luaFunction.set("setCG", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue control, LuaValue val) {
                setControlGroup(control.tojstring(), val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getPaint", new LuaFunction() {
            @Override
            public LuaValue call() {
                String result = getCurrentTexture();
                return LuaValue.valueOf(result);
            }
        });
        luaFunction.set("setPaint", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue newTexture) {
                setNewTexture(newTexture.tojstring());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getReadout", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue readout) {
                float result = getReadout(readout.tojstring());
                return LuaValue.valueOf(result);
            }
        });
        luaFunction.set("setPerformance", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue performanceType, LuaValue newVal) {
                setPerformance(performanceType.tojstring(), newVal.todouble());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("couplerEngaged", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue position, LuaValue newState) {
                setCouplerEngaged(position.tojstring(), newState.toboolean());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setThrottle", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue val) {
                setThrottleLua(val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setReverser", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue val) {
                setReverserLua(val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setTrainBrake", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue val) {
                setBrakeLua(val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setIndependentBrake", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue val) {
                setIndependentBrakeLua(val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getThrottle", new LuaFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(getThrottleLua());
            }
        });
        luaFunction.set("getReverser", new LuaFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(getReverserLua());
            }
        });
        luaFunction.set("getTrainBrake", new LuaFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(getTrainBrakeLua());
            }
        });
        luaFunction.set("setSound", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue val) {
                setNewSound(val);
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setGlobal", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue control, LuaValue val) {
                setGlobalControlGroup(control.tojstring(), val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setUnit", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue control, LuaValue val) {
                setUnitControlGroup(control.tojstring(), val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setText", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue identifier, LuaValue text) {
                Identifier id = new Identifier(identifier.tojstring());
                setText(id, text.tojstring());
                return LuaValue.NIL;
            }
        });
    }

    public float getControlGroup(String control) {
        return getControlPosition(control);
    }

    public void setControlGroup(String control, float val) {
        controlPositions.put(control, Pair.of(false, val));
    }

    public void setGlobalControlGroup(String controlName, float newValControl) {
        this.mapTrain(this, false, stock -> {
            stock.controlPositions.put(controlName, Pair.of(false, newValControl));
        });
    }

    @SideOnly(Side.CLIENT)
    public void setUnitControlGroup(String controlName, float newValControl) {
        List<List<EntityCoupleableRollingStock>> units = this.getUnit(false);
        for (List<EntityCoupleableRollingStock> unit : units) {
            if (unit.contains(this)) {
                for (EntityCoupleableRollingStock stock : unit) {
                    stock.controlPositions.put(controlName, Pair.of(false, newValControl));
                    oldControl = controlName;
                    oldValue = newValControl;
                }
                break;
            }
        }
    }

    public String getCurrentTexture() {
        String currentTexture = getTexture();
        if (currentTexture == null) {
            currentTexture = "Default";
        }
        return currentTexture;
    }

    public void setNewTexture(String texture) {
        setTexture(texture);
    }

    public float getReadout(String readout) {
        getReadout();
        return readoutState.get(readout);
    }

    public void setPerformance(String performanceType, double val) {
        switch (performanceType) {
            case "max_speed_kmh":
                getDefinition().setMaxSpeed(val);
                break;
            case "tractive_effort_lbf":
                getDefinition().setTraction(val);
                break;
            case "horsepower":
                getDefinition().setHorsepower(val);
                break;
        }
    }

    public void setCouplerEngaged(String position, Boolean engaged) {
        switch (position) {
            case "FRONT":
                setCouplerEngaged(CouplerType.FRONT, engaged);
                break;
            case "BACK":
                setCouplerEngaged(CouplerType.BACK, engaged);
                break;
        }
    }

    public void setText(Identifier id, String newText) {
        List<ModelComponent> test = getDefinition().getModel().allComponents;
        for (ModelComponent component : getDefinition().getModel().allComponents) {
            if (component.type == ModelComponentType.TEXTFIELD_X) {
                if (!newText.equals(oldText)) {
                    vec3d = component.center;
                    RenderText renderText = RenderText.getInstance(defID);
                    renderText.setText(newText, id, vec3d);
                    oldText = newText;
                }
            }
        }
    }

    public void callFuction() {
        tickEvent.call();
    }

    public void setNewSound(LuaValue result) {
        List<Map<String, DataBlock.Value>> newSound = new ArrayList<>();

        if (result.istable()) {

            for (LuaValue key : result.checktable().keys()) {

                LuaValue entry = result.get(key);
                Map<String, DataBlock.Value> soundDefinition = new HashMap<>();

                for (LuaValue entrykey : entry.checktable().keys()) {
                    LuaValue value = entry.get(entrykey);
                    DataBlock.Value soundDef = new ObjectValue(convertLuaValue(value));
                    soundDefinition.put(entrykey.tojstring(), soundDef);
                }
                newSound.add(soundDefinition);
            }
            getDefinition().setSounds(newSound);
        }
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
