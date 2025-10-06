package cam72cam.immersiverailroading.script;

import cam72cam.mod.ModCore;
import cam72cam.mod.resource.Identifier;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class LuaContext {
    private final Globals globals;
    private Map<String, LuaValue> backingMap;
    private LuaTable tagFieldTable;

    private static Globals sandBoxedGlobals() {
        Globals env = new Globals();
        env.load(new JseBaseLib());
        env.load(new PackageLib());
        env.load(new Bit32Lib());
        env.load(new TableLib());
        env.load(new JseStringLib());
        env.load(new JseMathLib());
        env.load(new JseOsLib());
        LoadState.install(env);
        LuaC.install(env);

        LuaValue os = LuaValue.tableOf();
        os.set("time", env.get("os").get("time"));
        os.set("date", env.get("os").get("date"));
        os.set("clock", env.get("os").get("clock"));
        os.set("difftime", env.get("os").get("difftime"));

        env.set("os", os);

        return env;
    }

    public static LuaContext create(Object object, Globals globals) {
        return new LuaContext(object, globals);
    }

    public static LuaContext create(Object object) {
        return create(object, sandBoxedGlobals());
    }

    private LuaContext(Object object, Globals globals) {
        this.globals = globals;
        initializeFunctions(object, globals);
        initializeSerialization();
    }

    private void initializeFunctions(Object object, Globals globals) {
        Class<?> parent = object.getClass();

        ArrayList<Method> methods = new ArrayList<>();

        while (parent != null && parent != Object.class) {
            methods.addAll(Arrays.asList(parent.getDeclaredMethods()));
            parent = parent.getSuperclass();
        }

        Map<String, LuaValue> modules = new HashMap<>();

        for (Method method : methods) {
            method.setAccessible(true);

            LuaFunction tag = method.getAnnotation(LuaFunction.class);

            if (tag == null) {
                continue;
            }

            String functionName = tag.name().isEmpty() ? method.getName() : tag.name();
            String module = tag.module();

            LuaTable functions = (LuaTable) modules.getOrDefault(module, new LuaTable());

            Class<?> returnType = method.getReturnType();
            if (!(Varargs.class.isAssignableFrom(returnType) || returnType.equals(void.class))) {
                continue;
            }

            boolean hasReturn = !returnType.equals(void.class);

            LuaValue func = new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs varargs) {
                    int narg = varargs.narg();
                    LuaValue[] args = new LuaValue[narg];

                    for (int i = 1; i <= narg ; i++) {
                        args[i - 1] = varargs.arg(i);
                    }

                    if (hasReturn) {
                        return (Varargs) invokeMethod(object, method, (Object[]) args);
                    } else {
                        invokeMethod(object, method, (Object[]) args);
                        return NIL;
                    }
                }
            };

            if (module.isEmpty()) {
                modules.put(functionName, func);
            } else {
                functions.set(LuaValue.valueOf(functionName), func);
                modules.put(module, functions);
            }
        }

        for (Map.Entry<String, LuaValue> functions : modules.entrySet()) {
            globals.set(functions.getKey(), functions.getValue());
        }
    }

    private void initializeSerialization() {
        backingMap = new HashMap<>();
        tagFieldTable = createTagField(backingMap);
        globals.set("_TagField", tagFieldTable);
    }

    @SuppressWarnings("unused")
    public Globals getGlobals() {
        return globals;
    }

    public void loadScript(Identifier path) {
        try (InputStream inputStream = path.getResourceStream()) {
            String fileName = new File(path.getPath()).getName();
            LuaValue chunk = globals.load(inputStream, fileName, "t", globals);
            chunk.call();
        } catch (Exception e) {
            ModCore.catching(e, "An error occurred while loading script %s", path.getPath());
        }
    }

    public void loadModules(List<String> modules, Identifier origin) {
        LuaValue packageLib = globals.get("package");
        LuaValue preloadTable = packageLib.get("preload");

        for (String module : modules) {
            String name = module.replace(".lua", "");

            try (InputStream inputStream = origin.getRelative(module).getResourceStream()) {
                LuaValue chunk = globals.load(inputStream, name, "t", globals);
                preloadTable.set(name, chunk);
            } catch (Exception e) {
                ModCore.error("Package %s does not exist in the directory %s", module, new File(origin.getPath()).getPath());
            }
        }
    }

    public void registerLibrary(LuaModule library) {
        initializeFunctions(library, globals);
    }

    public void refreshSerialization(Map<String, LuaValue> tagFields) {
        for (Map.Entry<String, LuaValue> entry : backingMap.entrySet()) {
            tagFields.putIfAbsent(entry.getKey(), entry.getValue());
        }

        backingMap = tagFields;
        tagFieldTable.setmetatable(buildMeta(backingMap));

        for (Map.Entry<String, LuaValue> entry : tagFields.entrySet()) {
            tagFieldTable.set(entry.getKey(), entry.getValue());
        }
    }

    private Object invokeMethod(Object object, Method method, Object... args) {
        try {
            Object[] methodArgs = args;
            if (method.isVarArgs()) {
                Class<?>[] paramTypes = method.getParameterTypes();
                int fixed = paramTypes.length - 1;

                methodArgs = new Object[paramTypes.length];

                if (fixed >= 0) System.arraycopy(args, 0, methodArgs, 0, fixed);

                Class<?> compType = paramTypes[fixed].getComponentType();
                int varCount = Math.max(0, args.length - fixed);
                Object varArray = Array.newInstance(compType, varCount);
                for (int i = 0; i < varCount; i++) {
                    Array.set(varArray, i, args[fixed + i]);
                }
                methodArgs[fixed] = varArray;
            }

            return method.invoke(object, methodArgs);
        } catch (IllegalAccessException e) {
            ModCore.error("Method %s is not accessible: %s", method.getName(), e.getMessage());
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            ModCore.catching(e);
        }

        return null;
    }

    private LuaTable buildMeta(Map<String, LuaValue> map) {
        LuaTable meta = new LuaTable();

        meta.set("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue key) {
                LuaValue v = map.get(key.tojstring());
                return v != null ? v : LuaValue.NIL;
            }
        });

        meta.set("__newindex", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue key, LuaValue value) {
                map.put(key.tojstring(), value);
                return LuaValue.NIL;
            }
        });

        return meta;
    }

    private LuaTable createTagField(Map<String, LuaValue> tagFields) {
        LuaTable t = LuaValue.tableOf();
        t.setmetatable(buildMeta(tagFields));
        return t;
    }
}
