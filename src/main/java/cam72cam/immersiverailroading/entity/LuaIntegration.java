package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.gui.overlay.ReadoutsEventHandler;
import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.LuaValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.luaj.vm2.LuaFunction;

public abstract class LuaIntegration extends EntityCoupleableRollingStock implements ReadoutsEventHandler {

    private LuaValue tickEvent;
    public boolean isLuaLoaded = false;
    private boolean isSleeping;
    private long lastExecutionTime;
    private boolean wakeLuaScriptCalled = false;
    LuaTable luaFunction = new LuaTable();
    private final Map<String, InputStream> moduleMap = new HashMap<>();
    private Map<String, String> componentTextMap = new HashMap<>();

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
            Globals globals = JsePlatform.standardGlobals();

            LuaValue safeOs = LuaValue.tableOf();
            safeOs.set("time", globals.get("os").get("time"));
            safeOs.set("date", globals.get("os").get("date"));
            safeOs.set("clock", globals.get("os").get("clock"));
            safeOs.set("difftime", globals.get("os").get("difftime"));


            globals.set("os", safeOs);

            globals.set("io", LuaValue.NIL);
            globals.set("luajava", LuaValue.NIL);
            globals.set("coroutine", LuaValue.NIL);
            globals.set("debug", LuaValue.NIL);


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
                ModCore.error("An error occurred while preloading lua modules", e);
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
            public LuaValue call(LuaValue table) {
                textFieldDef(table);
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getTag", new LuaFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(getTag());
            }
        });
        luaFunction.set("getTrain", new LuaFunction() {
            @Override
            public LuaValue call() {
                return getTrainConsist();
            }
        });
        luaFunction.set("setIndividualCG", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                setIndividualCG(luaValue);
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getIndividualCG", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                return getIndividualCG(luaValue);
            }
        });
        luaFunction.set("engineStartStop", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                setTurnedOnLua(luaValue.toboolean());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("isTurnedOn", new LuaFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(getEngineState());
            }
        });
        luaFunction.set("setTag", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue tag) {
                setEntityTag(tag.tojstring());
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

    public void setUnitControlGroup(String controlName, float newValControl) {
        List<List<EntityCoupleableRollingStock>> units = this.getUnit(false);
        for (List<EntityCoupleableRollingStock> unit : units) {
            if (unit.contains(this)) {
                for (EntityCoupleableRollingStock stock : unit) {
                    stock.controlPositions.put(controlName, Pair.of(false, newValControl));
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

    public void textFieldDef(LuaValue result) {
        Map<String, DataBlock.Value> textField = new HashMap<>();
        if (result.istable()) {
            for (LuaValue key : result.checktable().keys()) {
                DataBlock.Value value = new ObjectValue(convertLuaValueText(key, result.get(key))); // Fixed here
                textField.put(key.tojstring(), value);
            }
        }

        Identifier font = textField.get("font") != null ? textField.get("font").asIdentifier() : null;
        String textFieldId = textField.get("ID") != null ? textField.get("ID").asString() : null;
        int resX = textField.get("resX") != null ? textField.get("resX").asInteger() : 0;
        int resY = textField.get("resY") != null ? textField.get("resY").asInteger() : 0;
        boolean flipped = textField.get("flipped") != null && textField.get("flipped").asBoolean();
        int textureHeight = textField.get("textureHeight") != null ? textField.get("textureHeight").asInteger() : 12;
        int fontSize = textField.get("fontSize") != null ? textField.get("fontSize").asInteger() : textureHeight;
        int fontLength = textField.get("textureWidth") != null ? textField.get("textureWidth").asInteger() : 512;
        int fontGap = textField.get("fontGap") != null ? textField.get("fontGap").asInteger() : 1;
        Identifier overlay = textField.get("overlay") != null ? textField.get("overlay").asIdentifier() : null;
        String hexCode = textField.get("color") != null ? textField.get("color").asString() : null;
        boolean fullbright = textField.get("fullbright") != null ? textField.get("fullbright").asBoolean() : false;
        boolean allStock = textField.get("global") != null ? textField.get("global").asBoolean() : false;
        boolean useAlternative = textField.get("useAltAlignment") != null ? textField.get("useAltAlignment").asBoolean() : false;
        int lineSpacingPixels = textField.get("lineSpacing") != null ? textField.get("lineSpacing").asInteger() : 1;
        int offset = textField.get("offset") != null ? textField.get("offset").asInteger() : 0;

        Font.TextAlign align;
        if (textField.get("align") != null) {
            String alignStr = textField.get("align").asString();
            if (alignStr.equalsIgnoreCase("right")) {
                align = Font.TextAlign.RIGHT;
            } else if (alignStr.equalsIgnoreCase("center")) {
                align = Font.TextAlign.CENTER;
            } else {
                align = Font.TextAlign.LEFT;
            }
        } else {
            align = Font.TextAlign.LEFT;
        }

        String text = textField.get("text") != null ? textField.get("text").asString() : "";
        try {
            TextRenderOptions options = new TextRenderOptions(
                    font, text, resX, resY, align, flipped, textFieldId, fontSize, fontLength, fontGap, overlay, hexCode, fullbright, textureHeight, useAlternative, lineSpacingPixels, offset
            );
            if (allStock) {
                setAllText(options);
            } else {
                setText(options);
            }
        } catch (IOException e) {
            ModCore.error(String.format("Couldn't load Font %s", font != null ? font.toString() : "null"));
        }
    }

    private Vec3d getVec3dmin (List<Vec3d> vectors) {
        return vectors.stream().min(Comparator.comparingDouble(Vec3d::length)).orElse(null);
    }

    private Vec3d getVec3dmax (List<Vec3d> vectors) {
        return vectors.stream().max(Comparator.comparingDouble(Vec3d::length)).orElse(null);
    }

    public void setAllText(TextRenderOptions options) {
        this.mapTrain(this, false, stock -> {
            if (stock == null) {
                ModCore.error("Stock is null when setting text.");
                return;
            }
            if (options == null) {
                ModCore.error("Options is null when setting text.");
                return;
            }
            stock.setTextTrain(options);
        });
    }

    private void setText(TextRenderOptions options) throws IOException {
        String currentText = componentTextMap.get(options.componentId);
        if (currentText == null || !options.newText.equals(currentText)) {
            LinkedHashMap<String, OBJGroup> group = this.getDefinition().getModel().groups;
            for (Map.Entry<String, OBJGroup> entry : group.entrySet()) {
                if (entry.getKey().contains(String.format("TEXTFIELD_%s", options.componentId))) {
                    EntityRollingStockDefinition.Position getPosition = getDefinition().normals.get(entry.getKey());
                    Vec3d vec3dmin = getVec3dmin(getPosition.vertices);
                    Vec3d vec3dmax = getVec3dmax(getPosition.vertices);
                    Vec3d vec3dNormal = getPosition.normal;
                    RenderText renderText = RenderText.getInstance(String.valueOf(getUUID()));
                    File file = new File(options.id.getPath());
                    String jsonPath = file.getName();
                    Identifier jsonId = options.id.getRelative(jsonPath.replaceAll(".png", ".json"));
                    InputStream json = jsonId.getResourceStream();

                    renderText.setText(
                            options.componentId, options.newText, options.id, vec3dmin, vec3dmax, json,
                            options.resX, options.resY, options.align, options.flipped, options.fontSize, options.fontX,
                            options.fontGap, options.overlay, vec3dNormal, options.hexCode, options.fullbright, options.textureHeight, options.useAlternative, options.lineSpacingPixels, options.offset
                    );

                    // Update the map with the new text for this component
                    componentTextMap.put(options.componentId, options.newText);
                }
            }
        }
    }

    public LuaValue getTrainConsist() {
        List<List<EntityCoupleableRollingStock>> unit = getUnit(false);
        LuaValue table = LuaValue.tableOf();

        for (int i = 0; i < unit.size(); i++) {
            List<EntityCoupleableRollingStock> oneUnit = unit.get(i);
            LuaValue luaUnit = LuaValue.tableOf();

            for (int j = 0; j < oneUnit.size(); j++) {
                EntityCoupleableRollingStock obj = oneUnit.get(j);

                LuaValue objTable = LuaValue.tableOf();

                objTable.set("UUID", LuaValue.valueOf(obj.getUUID().toString()));

                objTable.set("defID", obj.defID != null ? LuaValue.valueOf(obj.defID) : LuaValue.NIL);

                objTable.set("coupledFront", obj.coupledFront != null ? LuaValue.valueOf(obj.coupledFront.toString()) : LuaValue.valueOf(""));

                objTable.set("coupledBack", obj.coupledBack != null ? LuaValue.valueOf(obj.coupledBack.toString()) : LuaValue.valueOf(""));

                objTable.set("distanceTraveled", obj.distanceTraveled != 0 ? LuaValue.valueOf(obj.distanceTraveled) : LuaValue.valueOf(0));
                LuaTable controlPosition = LuaValue.tableOf();

                obj.controlPositions.forEach((key, pair) -> {
                    LuaValue booleanValue = LuaValue.valueOf(pair.getLeft());
                    LuaValue floatValue = LuaValue.valueOf(pair.getRight());

                    LuaValue pairTable = LuaValue.tableOf(new LuaValue[]{ booleanValue, floatValue });

                    controlPosition.set(LuaValue.valueOf(key), pairTable);
                });

                objTable.set("controlPositions", controlPosition);
                objTable.set("tag", obj.tag);
                luaUnit.set(j + 1, objTable);
            }
            table.set(i + 1, luaUnit);
        }
        return table;
    }

    private String getTag() {
        return this.tag;
    }

    public void callFuction() {
        tickEvent.call();
    }

    public void setIndividualCG(LuaValue stockUnit) {
        List<List<EntityCoupleableRollingStock>> allUnit = getUnit(false);
        int unit = stockUnit.checktable().get(1).toint();
        int stock = stockUnit.checktable().get(2).toint();
        String control = stockUnit.checktable().get(3).tojstring();
        float value = stockUnit.checktable().get(4).tofloat();
        List<EntityCoupleableRollingStock> individualUnit = allUnit.get(unit - 1);
        if (allUnit.size() <= unit && individualUnit.size() <= stock) {
            EntityCoupleableRollingStock individualStock = individualUnit.get(stock - 1);
            individualStock.controlPositions.put(control, Pair.of(false, value));
        }
    }

    public LuaValue getIndividualCG(LuaValue stockUnit) {
        List<List<EntityCoupleableRollingStock>> allUnit = getUnit(false);
        int unit = stockUnit.checktable().get(1).toint();
        int stock = stockUnit.checktable().get(2).toint();
        String control = stockUnit.checktable().get(3).tojstring();

        List<EntityCoupleableRollingStock> individualUnit = allUnit.get(unit - 1);
        if (allUnit.size() <= unit && individualUnit.size() <= stock) {
            EntityCoupleableRollingStock individualStock = individualUnit.get(stock - 1);
            Pair<Boolean, Float> value = individualStock.controlPositions.containsKey(control) ? individualStock.controlPositions.get(control) : Pair.of(false, 0f);
            return LuaValue.valueOf(value.getRight());
        } else {
            return LuaValue.valueOf(0);
        }
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
            getDefinition().setSounds(newSound, this);
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

    private static Object convertLuaValueText(LuaValue k, LuaValue value) {
        if ("text".equals(k.tojstring())) {
            return value.tojstring();  // Force the value to be a string
        }

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

    public void setTurnedOnLua(boolean b) {
    }

    public boolean getEngineState() {
        return false;
    }

    @Override
    public void setTextTrain(TextRenderOptions options) {
        try {
            setText(options);
        } catch (IOException e) {
            ModCore.error("An error occurred while creating textfields", e);
        }
    }
}
