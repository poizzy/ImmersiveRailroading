package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.items.ItemTypewriter;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.script.LuaLibrary;
import cam72cam.immersiverailroading.script.ScriptVectorUtil;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.*;
import cam72cam.mod.text.PlayerMessage;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.LuaValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class EntityScriptableRollingStock extends EntityCoupleableRollingStock {

    private LuaValue tickEvent;
    public boolean isLuaLoaded = false;
    private boolean isSleeping;
    private long lastExecutionTime;
    private boolean wakeLuaScriptCalled = false;
    private final Map<String, InputStream> moduleMap = new HashMap<>();
    private final Map<String, String> componentTextMap = new HashMap<>();
    private final Map<Integer, ParticleState> particleStates = new HashMap<>();
    private final Map<Integer, ParticleState> oldParticleState = new HashMap<>();

    @TagField(value = "textRenderOptions", mapper = MapTextRenderOptionsMapper.class)
    public Map<String, TextRenderOptions> textRenderOptions = new HashMap<>();

    public Globals globals;

    private final Set<ScheduleEvent> schedule = new HashSet<>();

    @Override
    public void onTick() {
        super.onTick();
        if (getWorld().isClient) {
            return;
        }
        if (getDefinition().script != null && !ConfigPerformance.disableLuaScript) {
            long currentTime = System.currentTimeMillis();

            // Handle sleep logic if luaScriptSleep is greater than 0
            if (ConfigPerformance.luaScriptSleep > 0) {
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
            } else {
                // If luaScriptSleep is 0, reset sleep state
                isSleeping = false;
                lastExecutionTime = currentTime;
            }

            // Execute Lua script if not sleeping or luaScriptSleep is 0
            try {
                if (LoadLuaFile()) return;
                callFuction();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        schedule.removeIf(t -> {
            t.ticks--;
            if (t.ticks <= 0) {
                t.runnable.run();
                return true;
            }
            return false;
        });
    }

    @Override
    public ClickResult onClick(Player player, Player.Hand hand) {
        if (player.getHeldItem(hand).is(IRItems.ITEM_TYPEWRITER) && !textRenderOptions.isEmpty() && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
            ItemTypewriter.onStockInteract(this, player, hand);
            return ClickResult.ACCEPTED;
        }
        return super.onClick(player, hand);
    }

    @Override
    public void kill() {
        super.kill();
        getDefinition().inputs.remove(getUUID());
    }

    @Override
    public void load(TagCompound data) {
        super.load(data);
        if (!textRenderOptions.isEmpty()) {
            textRenderOptions.forEach((s, options) -> new ItemTypewriter.TypewriterPacket(this, options));
        }
    }

    public boolean LoadLuaFile() throws IOException {
        if (!isLuaLoaded) {
            globals = JsePlatform.standardGlobals();

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
                ModCore.error(String.format("Script file %s does not exist", identifier));
                return true;
            }

            String luaScript = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if (luaScript == null) {
                ModCore.error(String.format("Lua script file %s not found", identifier));
                return true;
            }else if (luaScript.isEmpty()) {
                ModCore.error(String.format("Lua script %s 's content is empty", identifier));
                return true;
            }

            if (getDefinition().addScripts != null) {
                for (String modules : getDefinition().addScripts) {
                    Identifier newModule = identifier.getRelative(modules);
                    moduleMap.put(modules.replace(".lua", ""), newModule.getResourceStream());
                }
                preloadModules(globals, moduleMap);
            }

            LuaLibrary.create("IR")
                    .addFunction("setCG", (control, val) -> this.setControlGroup(control.tojstring(), val.tofloat()))
                    .addFunctionWithReturn("getCG", control -> LuaValue.valueOf(this.getControlGroup(control.tojstring())))
                    .addFunction("setPaint", newTexture -> this.setNewTexture(newTexture.tojstring()))
                    .addFunctionWithReturn("getPaint", () -> LuaValue.valueOf(this.getCurrentTexture()))
                    .addFunctionWithReturn("getReadout", readout -> LuaValue.valueOf(this.getReadout(readout.tojstring())))
                    .addFunction("setPerformance", this::setPerformance)
                    .addFunctionWithReturn("getPerformance", this::getPerformance)
                    .addFunction("couplerEngaged", this::setCouplerEngagedLua)
                    .addFunction("setThrottle", this::setThrottleLua)
                    .addFunctionWithReturn("getThrottle", this::getThrottleLua)
                    .addFunction("setReverser", this::setReverserLua)
                    .addFunctionWithReturn("getReverser", this::getReverserLua)
                    .addFunction("setTrainBrake", this::setTrainBrakeLua)
                    .addFunctionWithReturn("getTrainBrake", this::getTrainBrakeLua)
                    .addFunction("setIndependentBrake", this::setIndependentBrakeLua)
                    .addFunction("setSound", val -> {/*this.setNewSound(val)*/})
                    .addFunction("setGlobal", (control, val) -> this.setGlobalControlGroup(control.tojstring(), val.tofloat()))
                    .addFunction("setUnit", (control, val) -> this.setUnitControlGroup(control.tojstring(), val.tofloat()))
                    .addFunction("setText", this::textFieldDef)
                    .addFunction("setTag", val -> this.setEntityTag(val.tojstring()))
                    .addFunctionWithReturn("getTag", () -> LuaValue.valueOf(this.getTag()))
                    .addFunctionWithReturn("getTrain", this::getTrainConsist)
                    .addFunction("setIndividualCG", this::setIndividualCG)
                    .addFunctionWithReturn("getIndividualCG", this::getIndividualCG)
                    .addFunctionWithReturn("isTurnedOn", () -> LuaValue.valueOf(this.getEngineState()))
                    .addFunction("engineStartStop", this::setTurnedOnLua)
                    .addFunction("newParticle", this::particleDefinition)
                    .addFunction("setNBTTag", (k, v) -> this.setNBTTag(k.tojstring(), v))
                    .addFunctionWithReturn("getNBTTag", (k) -> this.getNBTTag(k.tojstring()))
                    .addFunctionWithReturn("getStockPosition", () -> ScriptVectorUtil.constructVec3Table(this.getPosition()))
                    .addFunctionWithReturn("getStockMatrix", () -> ScriptVectorUtil.constructMatrix4Table(this.getModelMatrix()))
                    .addFunctionWithReturn("newVector", (x, y, z) -> ScriptVectorUtil.constructVec3Table(x, y, z))
                    .addFunctionWithReturn("getCoupled", this::getCoupled)
                    .setInGlobals(globals);

            LuaLibrary.create("World")
                    .addFunctionWithReturn("isRainingAt", pos -> LuaValue.valueOf(this.getWorld().isRaining(ScriptVectorUtil.convertToVec3i(pos))))
                    .addFunctionWithReturn("getTemperatureAt", pos -> LuaValue.valueOf(this.getWorld().getTemperature(ScriptVectorUtil.convertToVec3i(pos))))
                    .addFunctionWithReturn("getSnowLevelAt", pos -> LuaValue.valueOf(this.getWorld().getSnowLevel(ScriptVectorUtil.convertToVec3i(pos))))
                    .addFunctionWithReturn("getBlockLightLevelAt", pos -> LuaValue.valueOf(this.getWorld().getBlockLightLevel(ScriptVectorUtil.convertToVec3i(pos))))
                    .addFunctionWithReturn("getSkyLightLevelAt", pos -> LuaValue.valueOf(this.getWorld().getSkyLightLevel(ScriptVectorUtil.convertToVec3i(pos))))
                    .addFunctionWithReturn("getTicks", () -> LuaValue.valueOf(this.getWorld().getTicks()))
                    .setInGlobals(globals);

            LuaLibrary.create("Debug")
                    .addFunction("printToInfoLog", arg -> ModCore.info(arg.tojstring()))
                    .addFunction("printToWarnLog", arg -> ModCore.warn(arg.tojstring()))
                    .addFunction("printToErrorLog", arg -> ModCore.error(arg.tojstring()))
                    .addFunction("printToPassengerDialog", arg -> this.getPassengers().stream()
                            .filter(Entity::isPlayer)
                            .map(Entity::asPlayer)
                            .forEach(player -> player.sendMessage(PlayerMessage.direct(arg.tojstring()))))
                    .setInGlobals(globals);

            LuaLibrary.create("Utils")
                    .addFunction("wait", this::luaWait)
                    .setInGlobals(globals);

            ScriptVectorUtil.VecUtil.setInGlobals(globals);
            LuaValue chunk = globals.load(luaScript);
            chunk.call();

            tickEvent = globals.get("tickEvent");
            if (tickEvent.isnil()) {
                ModCore.error(String.format("Function \"tickEvent\" in lua script %s is not defined!", identifier));
            }

            isLuaLoaded = true;
            ModCore.info(String.format("Lua environment from %s initialized and script loaded successfully", this.defID));
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
        Readouts readouts = Readouts.valueOf(readout.toUpperCase());
        return readouts.getValue(this);
    }

    public void luaWait(LuaValue sec, LuaValue func) {
        float seconds = sec.tofloat();
        Runnable runnable = () -> {
            try {
                func.call();
            } catch (Exception e) {
                ModCore.error("[Lua] Error while executing scheduled wait function: " + e.getMessage());
            }
        };

        int ticks = Math.round(seconds * 20);
        ScheduleEvent event = new ScheduleEvent(runnable, ticks, func);
        schedule.add(event);
    }

    protected void setPerformance(LuaValue performanceType, LuaValue val) {
        /* */
    }

    public void setCouplerEngagedLua(LuaValue positionLua, LuaValue engagedLua) {
        String position = positionLua.tojstring();
        boolean engaged = engagedLua.toboolean();
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
        if (!result.istable()) {
            ModCore.error("[Lua] Given parameter of IR.setText is not a table, this function will not work!");
            return;
        }

        LuaTable textField = result.checktable();

        String textFieldID = textField.get("ID").tojstring();

        TextRenderOptions allOptions;

        if (textRenderOptions.containsKey(textFieldID)) {
            TextRenderOptions options  = textRenderOptions.get(textFieldID);

            Optional.ofNullable(textField.get("font")).ifPresent(o -> options.id = new Identifier(o.tojstring()));
            Optional.ofNullable(textField.get("ID")).ifPresent(o -> options.componentId = o.tojstring());
            Optional.ofNullable(textField.get("resX")).ifPresent(o -> options.resX = o.toint());
            Optional.ofNullable(textField.get("resY")).ifPresent(o -> options.resY = o.toint());
            Optional.ofNullable(textField.get("flipped")).ifPresent(o -> options.flipped = o.toboolean());
            Optional.ofNullable(textField.get("textureHeight")).ifPresent(o -> options.textureHeight = o.toint());
            Optional.ofNullable(textField.get("fontSize")).ifPresent(o -> options.fontSize = o.toint());
            Optional.ofNullable(textField.get("textureWidth")).ifPresent(o -> options.fontX = o.toint());
            Optional.ofNullable(textField.get("fontGap")).ifPresent(o -> options.fontGap = o.toint());
            Optional.ofNullable(textField.get("color")).ifPresent(o -> options.hexCode = o.tojstring());
            Optional.ofNullable(textField.get("fullbright")).ifPresent(o -> options.fullbright = o.toboolean());
            Optional.ofNullable(textField.get("global")).ifPresent(o -> options.global = o.toboolean());
            Optional.ofNullable(textField.get("useAltAlignment")).ifPresent(o -> options.useAlternative = o.toboolean());
            Optional.ofNullable(textField.get("lineSpacing")).ifPresent(o -> options.lineSpacingPixels = o.toint());
            Optional.ofNullable(textField.get("offset")).ifPresent(o -> options.offset = o.toint());
            Optional.ofNullable(textField.get("align")).ifPresent(o -> {
                if (o.tojstring().equalsIgnoreCase("right")) {
                    options.align = Font.TextAlign.RIGHT;
                } else if (o.tojstring().equalsIgnoreCase("center")) {
                    options.align = Font.TextAlign.CENTER;
                } else {
                    options.align = Font.TextAlign.LEFT;
                }
            });
            Optional.ofNullable(textField.get("text")).ifPresent(o -> options.newText = o.tojstring());

            allOptions = options;
        } else {
            Identifier font = textField.get("font") != null ? new Identifier(textField.get("font").tojstring()) : null;
            String textFieldId = textField.get("ID") != null ? textField.get("ID").tojstring() : null;
            int resX = textField.get("resX") != null ? textField.get("resX").toint() : 0;
            int resY = textField.get("resY") != null ? textField.get("resY").toint() : 0;
            boolean flipped = textField.get("flipped") != null && textField.get("flipped").toboolean();
            int textureHeight = textField.get("textureHeight") != null ? textField.get("textureHeight").toint() : 12;
            int fontSize = textField.get("fontSize") != null ? textField.get("fontSize").toint() : textureHeight;
            int fontLength = textField.get("textureWidth") != null ? textField.get("textureWidth").toint() : 512;
            int fontGap = textField.get("fontGap") != null ? textField.get("fontGap").toint() : 1;
            String hexCode = textField.get("color") != null ? textField.get("color").tojstring() : null;
            boolean fullbright = textField.get("fullbright") != null && textField.get("fullbright").toboolean();
            boolean allStock = textField.get("global") != null && textField.get("global").toboolean();
            boolean useAlternative = textField.get("useAltAlignment") != null && textField.get("useAltAlignment").toboolean();
            int lineSpacingPixels = textField.get("lineSpacing") != null ? textField.get("lineSpacing").toint() : 1;
            int offset = textField.get("offset") != null ? textField.get("offset").toint() : 0;

            Font.TextAlign align;
            if (textField.get("align") != null) {
                String alignStr = textField.get("align").tojstring();
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

            String text = textField.get("text") != null ? textField.get("text").tojstring() : "";

            allOptions = new TextRenderOptions(
                    font, text, resX, resY, align, flipped, textFieldId, fontSize, fontLength, fontGap, new ArrayList<>(), hexCode, fullbright, textureHeight, useAlternative, lineSpacingPixels, offset, allStock, this.getDefinition()
            );
        }

        if (allOptions == null) {
            return;
        }

        new ItemTypewriter.TypewriterPacket(this, allOptions).sendToServer();
    }

    public void setTextGlobal(TextRenderOptions options) {
        this.mapTrain(this, false, stock -> {
            if (stock == null) {
                ModCore.error("Stock is null when setting text.");
                return;
            }
            if (options == null) {
                ModCore.error("Options is null when setting text.");
                return;
            }
            if (stock.getDefinition().getModel().groups.keySet().stream().anyMatch(s -> s.contains(String.format("TEXTFIELD_%s", options.componentId)))) {
                ((EntityScriptableRollingStock)stock).setText(options);
            }
        });
    }

    public void setText(TextRenderOptions options) {
        RenderText renderText = RenderText.getInstance(String.valueOf(this.getUUID()));
        String currentText = componentTextMap.get(options.componentId);
        if (options.newText.equals(currentText)) {
            return;
        }
        renderText.setText(options, this.getDefinition());
        componentTextMap.put(options.componentId, options.newText);
        textRenderOptions.put(options.componentId, options);
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

    private LuaTable getCoupled(LuaValue type) {
        LuaTable table = new LuaTable();
        String sType = type.tojstring();

        EntityCoupleableRollingStock stock = getCoupled(sType.equalsIgnoreCase("front") ? CouplerType.FRONT : CouplerType.BACK);
        CouplerType coupler = this.getCouplerFor(stock);
        UUID uuid = stock.getUUID();
        String defID = stock.defID;
        String tag = stock.tag;

        table.set("coupler", coupler.toString());
        table.set("uuid", uuid.toString());
        table.set("defID", defID);
        table.set("tag", tag);
        return table;
    }

    private String getTag() {
        return this.tag;
    }

    public void callFuction() {
        tickEvent.call();
    }

    @TagField(value = "luaData", mapper = LuaDataMapper.class)
    protected Map<String, Object> luaData = new HashMap<>();

    public void setNBTTag(String key, LuaValue table) {
        luaData.put(key, convertData(table));
    }

    private LuaValue getNBTTag(String key) {
        return convertToLuaValue(luaData.get(key));
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

    @Deprecated
    public void setNewSound(LuaValue result) {
    }

    protected LuaValue getPerformance(LuaValue type) {
        String strType = type.tojstring();
        switch (strType) {
            case "max_speed_kmh":
                return LuaValue.valueOf(getDefinition().getMaxSpeed());
            case "horsepower":
                return LuaValue.valueOf(getDefinition().getHorsepower());
            case "traction":
                return LuaValue.valueOf(getDefinition().getTraction());
            default:
                return LuaValue.valueOf(0);
        }
    }

    @Deprecated
    static class ParticleState {
        Vec3d motion;
        int lifespan;
        float darken;
        float thickness;
        double diameter;
        boolean alwaysRunning;
        Identifier texture;
        boolean render;
        int r;
        int g;
        int b;
        double a;
        double expansionRate;
        boolean normalWidth;

        ParticleState(Vec3d motion, int lifespan, float darken, float thickness, double diameter,Identifier texture, boolean alwaysRunning, boolean render, int r, int g, int b, double a, double expansionRate, boolean normalWidth) {
            this.motion = motion;
            this.lifespan = lifespan;
            this.darken = darken;
            this.thickness = thickness;
            this.diameter = diameter;
            this.alwaysRunning = alwaysRunning;
            this.texture = texture;
            this.render = render;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.expansionRate = expansionRate;
            this.normalWidth = normalWidth;

        }
        boolean equals(ParticleState other) {
            if (other == null) return false;
            return this.motion.equals(other.motion) &&
                    this.lifespan == other.lifespan &&
                    this.darken == other.darken &&
                    this.thickness == other.thickness &&
                    this.diameter == other.diameter &&
                    this.texture.equals(other.texture) &&
                    this.alwaysRunning == other.alwaysRunning;
        }
    }

    @Deprecated
    private void particleDefinition(LuaValue result) {
    }

    @Deprecated
    private void newParticle(Map<Integer, ParticleState> particle) {
    }

    public void setThrottleLua(LuaValue val) {

    }

    public void setReverserLua(LuaValue val) {

    }

    public void setTrainBrakeLua(LuaValue val) {

    }

    public void setIndependentBrakeLua(LuaValue val) {

    }

    public LuaValue getThrottleLua() {
        return LuaValue.valueOf(0);
    }

    public LuaValue getReverserLua() {
        return LuaValue.valueOf(0);
    }

    public LuaValue getTrainBrakeLua() {
        return LuaValue.valueOf(0);
    }

    @Override
    public void wakeLuaScript() {
        super.wakeLuaScript();
        wakeLuaScriptCalled = true;
    }

    public void setTurnedOnLua(LuaValue b) {
    }

    public static class Data {
        boolean b;
        int i;
        long l;
        double d;
        String s;
        Map<?, ?> map = new HashMap<>();
        List<?> list;

        Data(LuaValue value) {
            if (value.istable()) {
                LuaTable table = value.checktable();
                boolean isList = true;
                for (LuaValue key : table.keys()) {
                    if (!key.isint()) {
                        isList = false;
                        break;
                    }
                }

                if (isList) {
                    List<Object> tempList = new ArrayList<>();
                    for (int i = 1; i <= table.length(); i++) {
                        LuaValue element = table.get(i);
                        tempList.add(convertData(element));
                    }
                    this.list = tempList;
                } else {
                    Map<Object, Object> tempMap = new HashMap<>();
                    for (LuaValue key : table.keys()) {
                        LuaValue val = table.get(key);
                        tempMap.put(convertData(key), convertData(val));
                    }
                    this.map = tempMap;
                }
            } else if (value.isboolean()) {
                this.b = value.toboolean();
            } else if (value.isint()) {
                this.i = value.toint();
            } else if (value.islong()) {
                this.l = value.tolong();
            } else if (value.isnumber()) {
                this.d = value.todouble();
            } else if (value.isstring()) {
                this.s = value.tojstring();
            }
        }
        public LuaValue toLuaValue() {
            LuaTable table = new LuaTable();

            // Add each field to the Lua table if it is non-null
            if (this.s != null) {
                table.set("s", LuaValue.valueOf(this.s));
            }
            if (this.b) {
                table.set("b", LuaValue.valueOf(this.b));
            }
            if (this.i != 0) {
                table.set("i", LuaValue.valueOf(this.i));
            }
            if (this.l != 0L) {
                table.set("l", LuaValue.valueOf(this.l));
            }
            if (this.d != 0.0) {
                table.set("d", LuaValue.valueOf(this.d));
            }

            if (this.map != null && !this.map.isEmpty()) {
                LuaTable mapTable = new LuaTable();
                for (Map.Entry<?, ?> entry : this.map.entrySet()) {
                    LuaValue key = convertToLuaValue(entry.getKey());
                    LuaValue val = convertToLuaValue(entry.getValue());
                    mapTable.set(key, val);
                }
                table.set("map", mapTable);
            }

            if (this.list != null && !this.list.isEmpty()) {
                LuaTable listTable = new LuaTable();
                int index = 1; // Lua lists are 1-based index
                for (Object element : this.list) {
                    LuaValue luaElement = convertToLuaValue(element);
                    listTable.set(index++, luaElement);
                }
                table.set("list", listTable);
            }

            return table;
        }

        private LuaValue convertToLuaValue(Object value) {
            if (value == null) {
                return LuaValue.NIL;
            } else if (value instanceof String) {
                return LuaValue.valueOf((String) value);
            } else if (value instanceof Boolean) {
                return LuaValue.valueOf((Boolean) value);
            } else if (value instanceof Integer) {
                return LuaValue.valueOf((Integer) value);
            } else if (value instanceof Long) {
                return LuaValue.valueOf((Long) value);
            } else if (value instanceof Double) {
                return LuaValue.valueOf((Double) value);
            } else if (value instanceof Data) {
                return ((Data) value).toLuaValue();
            } else if (value instanceof List) {
                LuaTable listTable = new LuaTable();
                int index = 1;
                for (Object element : (List<?>) value) {
                    listTable.set(index++, convertToLuaValue(element));
                }
                return listTable;
            } else if (value instanceof Map) {
                LuaTable mapTable = new LuaTable();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    LuaValue key = convertToLuaValue(entry.getKey());
                    LuaValue val = convertToLuaValue(entry.getValue());
                    mapTable.set(key, val);
                }
                return mapTable;
            }

            return LuaValue.NIL;
        }
    }

    private static Object convertData(LuaValue value) {
        if (value.isboolean()) {
            return value.toboolean();
        } else if (value.isint()) {
            return value.toint();
        } else if (value.islong()) {
            return value.tolong();
        } else if (value.isnumber()) {
            return value.todouble();
        } else if (value.isstring()) {
            return value.tojstring();
        } else if (value.istable()) {
            return new Data(value);
        }
        return null;
    }

    private static LuaValue convertToLuaValue(Object obj) {
        if (obj instanceof Boolean) {
            return LuaValue.valueOf((Boolean) obj);
        } else if (obj instanceof Integer) {
            return LuaValue.valueOf((Integer) obj);
        } else if (obj instanceof Long) {
            return LuaValue.valueOf((Long) obj);
        } else if (obj instanceof Double) {
            return LuaValue.valueOf((Double) obj);
        } else if (obj instanceof String) {
            return LuaValue.valueOf((String) obj);
        } else if (obj instanceof Data) {
            return ((Data) obj).toLuaValue();
        }
        return LuaValue.NIL;  // If the type doesn't match, return Lua NIL
    }

    private static class MapTextRenderOptionsMapper implements TagMapper<Map<String, TextRenderOptions>> {

        @Override
        public TagAccessor<Map<String, TextRenderOptions>> apply(Class<Map<String, TextRenderOptions>> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> {
                        d.setMap(fieldName, o, k -> k, v -> new TagCompound()
                                .setString("id", v.id.toString())
                                .setString("newText", v.newText)
                                .setInteger("resX", v.resX)
                                .setInteger("resY", v.resY)
                                .setEnum("align", v.align)
                                .setBoolean("flipped", v.flipped)
                                .setString("componentId", v.componentId)
                                .setInteger("fontSize", v.fontSize)
                                .setInteger("fontX", v.fontX)
                                .setInteger("fontGap", v.fontGap)
                                .setList("fontId", v.fontId, i -> new TagCompound().setInteger("id", i))
                                .setString("hexCode", v.hexCode)
                                .setBoolean("fullbright", v.fullbright)
                                .setInteger("textureHeight", v.textureHeight)
                                .setBoolean("useAlternative", v.useAlternative)
                                .setInteger("lineSpacingPixels", v.lineSpacingPixels)
                                .setInteger("offset", v.offset)
                                .setBoolean("global", v.global)
                                .setList("linked", v.linked, l -> new TagCompound().setString("l", l))
                                .setBoolean("selectable", v.selectable)
                                .setBoolean("unique", v.unique)
                                .setBoolean("isNumberPlate", v.isNumberPlate)
                                .setString("lastText", v.lastText)
                                .setList("filter", v.filter, f -> new TagCompound().setString("f", f))
                                .setBoolean("assigned", v.assigned)
                                .setVec3d("min", v.min)
                                .setVec3d("max", v.max)
                                .setVec3d("normal", v.normal)
                                .setString("groupName", v.groupName));
                    },
                    (d) -> d.getMap(fieldName, k -> k, TextRenderOptions::new)
            );
        }
    }

    private static class LuaDataMapper implements TagMapper<Map<String, Object>> {
        @Override
        public TagAccessor<Map<String, Object>> apply(Class<Map<String, Object>> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    (tagCompound, map) -> {
                        // Serializer logic
                        tagCompound.setMap(fieldName, map, String::toString, this::convertToNBT);
                    },
                    tagCompound -> {
                        // Deserializer logic
                        return tagCompound.getMap(fieldName, key -> key, this::convertToLuaValue);
                    }
            );
        }

        private TagCompound convertToNBT(Object value) {
            TagCompound tag = new TagCompound();

            if (value instanceof String) {
                tag.setString("value", (String) value);
            } else if (value instanceof Integer) {
                tag.setInteger("value", (Integer) value);
            } else if (value instanceof Boolean) {
                tag.setBoolean("value", (Boolean) value);
            } else if (value instanceof Double) {
                tag.setDouble("value", (Double) value);
            } else if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) value;
                tag.setMap("value", map, String::toString, this::convertToNBT);
            }

            return tag;
        }

        private Object convertToLuaValue(TagCompound compound) {
            if (compound.hasKey("value")) {
                if (compound.getString("value") != null) {
                    return compound.getString("value");
                } else if (compound.getInteger("value") != null) {
                    return compound.getInteger("value");
                } else if (compound.getBoolean("value") != null) {
                    return compound.getBoolean("value");
                } else if (compound.getDouble("value") != null) {
                    return compound.getDouble("value");
                } else if (compound.get("value") != null) {
                    return compound.getMap("value", key -> key, this::convertToLuaValue);
                }
            }
            return null;
        }
    }

    private static class ScheduleEvent {
        public Runnable runnable;
        public Integer ticks;
        public LuaValue func;

        public ScheduleEvent(Runnable runnable, Integer ticks, LuaValue func) {
            this.runnable = runnable;
            this.ticks = ticks;
            this.func = func;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            ScheduleEvent that = (ScheduleEvent) object;
            return func.equals(that.func);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(func);
        }
    }

}
