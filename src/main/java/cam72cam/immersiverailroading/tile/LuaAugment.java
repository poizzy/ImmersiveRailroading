package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import org.apache.commons.io.IOUtils;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.ast.Str;
import org.luaj.vm2.lib.jse.JsePlatform;
import scala.reflect.internal.Trees;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LuaAugment {
    public List<Identifier> scripts = new ArrayList<>();
    public Identifier selectedScript;

    public LuaAugment(List<DataBlock> dataBlocks) {
        for (DataBlock data : dataBlocks) {
            List<DataBlock.Value> values = data.getValues("scripts");
            values.forEach(v -> scripts.add(v.asIdentifier()));
        }
    }

    public LuaAugment(List<Identifier> s, Identifier id) {
        this.scripts = s;
        this.selectedScript = id;
    }

    public LuaAugment(Identifier id) {
        this.selectedScript = id;

        loadLuaScript();
    }

    public void loadLuaScript() {
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

        try {
            String luaScript = IOUtils.toString(selectedScript.getResourceStream(), StandardCharsets.UTF_8);

            LuaValue chunk = globals.load(luaScript);
            chunk.call();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
