package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemTypewriter;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.script.*;
import cam72cam.immersiverailroading.script.library.ILuaEvent;
import cam72cam.immersiverailroading.script.library.LuaSerialization;
import cam72cam.immersiverailroading.script.modules.*;
import cam72cam.immersiverailroading.script.sound.SoundConfig;
import cam72cam.immersiverailroading.textfield.TextFieldConfig;
import cam72cam.immersiverailroading.textfield.library.TextFieldMapMapper;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.util.*;
import java.util.stream.Collectors;

public abstract class EntityScriptableRollingStock extends EntityCoupleableRollingStock implements ILuaEvent {
    /**
     * Lazily initialised. Stocks without their own script never allocate a context from {@link #onTick()}.
     * External callers (e.g. the {@code LUA_SCRIPTER} augment via {@link #getGlobals()}) trigger
     * {@link #ensureContext()} on demand, so a stock only pays the LuaJ-VM cost when something actually
     * touches its globals.
     * @see cam72cam.immersiverailroading.tile.TileRailBase
     */
    private LuaContext context;
    /**
     * Cached result of {@link #hasOwnScript()}. {@code null} = not yet computed.
     */
    private Boolean hasOwnScript;
    /**
     * Used by {@link IRModule}
     */
    // TagSync is actually a terrible idea in this case. Due to it creating an instance of TextFieldConfig every tick, the memory usage will go up by around 1GB before the GC erases it.
    // @TagSync
    @TagField(mapper = TextFieldMapMapper.class)
    public Map<String, TextFieldConfig> textFields = new HashMap<>();
    public Map<String, SoundConfig> sounds = new HashMap<>();
    /**
     * Used by {@link EventModule}
     */
    public final Map<String, List<LuaValue>> luaEventCallbacks = new HashMap<>();
    /**
     * Used by {@link LuaContext}
     */
    @TagSync
    @TagField(value = "luaTagField", mapper = LuaSerialization.LuaMapper.class)
    private Map<String, LuaValue> tagFields = new HashMap<>();

    /**
     * Pending {@code Utils.wait(...)} callbacks bucketed by absolute tick at which they should fire.
     * O(1) per-tick lookup beats the previous {@code HashSet#removeIf} pass that allocated an iterator
     * and decremented every entry on every tick.
     */
    protected final Map<Long, List<Runnable>> schedule = new HashMap<>();

    /**
     * <h2>Overrides</h2>
     */

    @Override
    public void onTick() {
        super.onTick();

        if (getWorld().isClient) {
            return;
        }

        if (Config.ConfigPerformance.disableLuaScript) {
            return;
        }

        // Skip the entire Lua pipeline for stocks without a script of their own.
        // External callers can still trigger lazy context creation via getGlobals().
        if (!hasOwnScript()) {
            return;
        }

        ensureContext();

        if (!schedule.isEmpty()) {
            List<Runnable> due = schedule.remove((long) getTickCount());
            if (due != null) {
                for (int i = 0; i < due.size(); i++) {
                    due.get(i).run();
                }
            }
        }

        triggerEvent("onTick");
    }

    @Override
    public ClickResult onClick(Player player, Player.Hand hand) {
        if (player.getHeldItem(hand).is(IRItems.ITEM_TYPEWRITER) && !textFields.isEmpty() && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
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
    public Map<String, List<LuaValue>> getLuaEventCallbacks() {
        return luaEventCallbacks;
    }

    @Override
    public void handleKeyPress(Player source, KeyTypes key, boolean disableIndependentThrottle) {
        boolean hasPermission;
        switch (key) {
            case INDEPENDENT_BRAKE_UP:
            case INDEPENDENT_BRAKE_DOWN:
            case INDEPENDENT_BRAKE_ZERO:
                hasPermission = source.hasPermission(Permissions.BRAKE_CONTROL);
                break;
            default:
                hasPermission = source.hasPermission(Permissions.LOCOMOTIVE_CONTROL);
                break;
        }
        if (getWorld().isServer) {
            triggerEvent("onKeyPress", LuaValue.valueOf(key.toString()), LuaValue.valueOf(hasPermission));
        }

        super.handleKeyPress(source, key, disableIndependentThrottle);
    }

    private void registerModules() {
        context.registerLibrary(new ScriptVectorUtil.VectorLibrary());
        context.registerLibrary(new MarkupModule());

        context.registerLibrary(new IRModule(this));
        context.registerLibrary(new WorldModule(getWorld()));
        context.registerLibrary(new StockDebugModule(this));
        context.registerLibrary(new EventModule(this));
    }

    private void loadLuaScript() {
        Identifier script = getDefinition().script;

        List<String> modules = getDefinition().addScripts;
        if (modules != null && !modules.isEmpty() && script != null) {
            context.loadModules(modules, script);
        }

        if (script != null && script.canLoad()) {
            context.loadScript(script);
        }
    }

    /**
     * @return {@code true} if this stock's definition references at least one Lua resource
     *         (a {@code script} identifier that resolves, or a non-empty {@code add_scripts} list).
     *         Cached after the first successful read of the definition.
     */
    private boolean hasOwnScript() {
        if (hasOwnScript == null) {
            EntityRollingStockDefinition def = getDefinition();
            if (def == null) {
                // Definition not ready yet; recompute next tick instead of caching false.
                return false;
            }
            Identifier script = def.script;
            List<String> modules = def.addScripts;
            hasOwnScript = (script != null && script.canLoad())
                        || (modules != null && !modules.isEmpty());
        }
        return hasOwnScript;
    }

    private void ensureContext() {
        if (context != null) {
            return;
        }
        context = LuaContext.create(this);
        registerModules();
        loadLuaScript();
        context.refreshSerialization(tagFields);
    }

    public Globals getGlobals() {
        ensureContext();
        return context.getGlobals();
    }

    public Map<String, TextFieldConfig> getTextFieldConfig() {
        return this.textFields;
    }

    public void initTextField(TextFieldConfig config) {
        if (config.isGlobal()) {
            mapTrain(this, false, stock -> {
                EntityScriptableRollingStock next = (EntityScriptableRollingStock) stock;
                next.textFields.computeIfPresent(config.getObject(), (k, v) -> new TextFieldConfig(config));
            });
        }

        if (config.getLinked() != null && !config.getLinked().isEmpty()) {
            for (String linkedObject : config.getLinked()) {
                if (linkedObject.equals(config.getObject())) {
                    continue;
                }

                TextFieldConfig linked = textFields.get(linkedObject);

                if (linked == null) {
                    continue;
                }

                linked.copyConfig(config);
                initTextField(linked);
            }
        }

        textFields.put(config.getObject(), config);
    }

    @LuaFunction(module = "")
    private LuaValue getName() {
        return LuaValue.valueOf(getDefinition().name());
    }

    @LuaFunction(module = "")
    public void print(LuaValue... str) {
        List<String> args = Arrays.stream(str).map(LuaValue::tojstring).collect(Collectors.toList());
        String formatedArgs = String.join("    ", args);
        ModCore.info("[Lua, %s] %s", getDefinition().name(), formatedArgs);
    }


    @LuaFunction(module = "Utils")
    public void wait(LuaValue sec, LuaValue func) {
        float seconds = sec.tofloat();
        // Clamp to >= 1 so wait(0, fn) still fires on a future tick (matches the previous
        // semantics of 'ticks-- ... if (ticks <= 0)' which always required at least one
        // onTick pass after the wait() call before firing).
        int delayTicks = Math.max(1, Math.round(seconds * 20));
        long fireAt = (long) getTickCount() + delayTicks;

        Runnable runnable = () -> {
            try {
                func.call();
            } catch (Exception e) {
                ModCore.error("[Lua] Error while executing scheduled wait function: " + e.getMessage());
            }
        };

        schedule.computeIfAbsent(fireAt, k -> new ArrayList<>(2)).add(runnable);
    }
}
