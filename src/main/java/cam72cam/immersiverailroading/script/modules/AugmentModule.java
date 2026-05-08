package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.immersiverailroading.script.LuaModule;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AugmentModule implements LuaModule {
    private final TileRailBase tile;

    /**
     * Pending {@code Utils.wait(...)} callbacks bucketed by absolute tick at which they should fire.
     * Replaces the previous {@code HashSet#removeIf} that decremented every entry on every world tick.
     */
    private final Map<Long, List<Runnable>> schedule = new HashMap<>();

    public AugmentModule(TileRailBase tile) {
        this.tile = tile;

        World.onTick(world -> {
            if (world.isClient) {
                return;
            }
            if (schedule.isEmpty()) {
                return;
            }
            List<Runnable> due = schedule.remove((long) tile.getTicksExisted());
            if (due != null) {
                for (int i = 0; i < due.size(); i++) {
                    due.get(i).run();
                }
            }
        });
    }

    @LuaFunction(module = "")
    public void print(LuaValue... str) {
        List<String> args = Arrays.stream(str).map(LuaValue::tojstring).collect(Collectors.toList());
        String formatedArgs = String.join("    ", args);
        ModCore.info("[Lua Augment] %s", formatedArgs);
    }

    @LuaFunction(module = "Augment")
    private LuaValue getRedstone() {
        int redstone = tile.getWorld().getRedstone(tile.getPos());
        return LuaValue.valueOf(redstone);
    }

    @LuaFunction(module = "Augment")
    private void setRedstone(LuaValue level) {
        tile.setRedstoneLevel(level.toint());
    }

    @LuaFunction(module = "Utils")
    private void wait(LuaValue sec, LuaValue func) {
        float seconds = sec.tofloat();
        // Clamp to >= 1 so wait(0, fn) still fires on a future tick (matches the previous
        // semantics of 'ticks-- ... if (ticks <= 0)' which always required at least one
        // World#onTick pass after the wait() call before firing).
        int delayTicks = Math.max(1, Math.round(seconds * 20));
        long fireAt = (long) tile.getTicksExisted() + delayTicks;

        Runnable runnable = () -> {
            try {
                func.call();
            } catch (Exception e) {
                ModCore.error("[Lua] Error while executing scheduled wait function: " + e.getMessage());
            }
        };

        schedule.computeIfAbsent(fireAt, k -> new ArrayList<>(2)).add(runnable);
    }

    @LuaFunction(module = "Utils")
    private void writeToChat(LuaValue arg) {
        tile.getWorld().getEntities(Player.class).forEach(p -> p.sendMessage(PlayerMessage.direct(arg.tojstring())));
    }
}
