package cam72cam.immersiverailroading.script.library;

import cam72cam.mod.ModCore;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.List;
import java.util.Map;

public interface ILuaEvent {

    Map<String, List<LuaValue>> getLuaEventCallbacks();

    /**
     * No-arg fast path. Java picks this overload over the varargs one for {@code triggerEvent("foo")}
     * call sites, so hot-path events like {@code onTick} avoid both the {@code LuaValue[0]} array
     * allocation and the {@link LuaValue#varargsOf} wrapper. {@link LuaValue#NONE} is a singleton
     * that already implements {@link Varargs} with zero elements.
     */
    default void triggerEvent(String eventName) {
        triggerEvent(eventName, LuaValue.NONE);
    }

    default void triggerEvent(String eventName, LuaValue... args) {
        triggerEvent(eventName, LuaValue.varargsOf(args));
    }

    default void triggerEvent(String eventName, Varargs args) {
        List<LuaValue> callBacks = getLuaEventCallbacks().get(eventName);
        if (callBacks == null || callBacks.isEmpty()) {
            return;
        }
        // Indexed loop avoids the Iterator allocation that the for-each version made on every trigger.
        // Matches the previous semantics for concurrent modification: a callback that mutates the list
        // could already produce surprising behaviour before this change.
        for (int i = 0, n = callBacks.size(); i < n; i++) {
            LuaValue callback = callBacks.get(i);
            try {
                callback.invoke(args);
            } catch (Exception e) {
                ModCore.catching(e, "Lua callback for event %s failed:", eventName);
            }
        }
    }

}
