package cam72cam.immersiverailroading.script;

import cam72cam.mod.ModCore;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.*;

/**
 * Helper class to construct LuaLibrary
 */
public class LuaLibrary {
    private final HashMap<String, Object[]> functions;
    private final String type;

    private LuaLibrary(String typeName){
        this.type = typeName;
        this.functions = new HashMap<>();
    }

    /**
     * A static factory method
     * @param typeName Type of the object or namespace of the library
     * @return The constructed LuaLibrary
     */
    public static LuaLibrary create(String typeName){
        return new LuaLibrary(typeName);
    }

    /**
     * Register a function which has no input and output
     * @param name Name of the function
     * @param function Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunction(String name, Runnable function){
        Object[] func = functions.getOrDefault(name, new Object[4]);
        if(func[0] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[0] = function;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a single parameter consumer function, like setter
     * @param name Name of the function
     * @param consumer Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunction(String name, Consumer<LuaValue> consumer){
        Object[] func = functions.getOrDefault(name, new Object[4]);
        if(func[1] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[1] = consumer;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a double parameter consumer function, like setter
     * @param name Name of the function
     * @param consumer Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunction(String name, BiConsumer<LuaValue, LuaValue> consumer){
        Object[] func = functions.getOrDefault(name, new Object[4]);
        if(func[2] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[2] = consumer;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a Triple parameter consumer function, like setter
     * @param name Name of the function
     * @param consumer Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunction(String name, TriConsumer<LuaValue, LuaValue, LuaValue> consumer){
        Object[] func = functions.getOrDefault(name, new Object[4]);
        if(func[3] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[3] = consumer;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a zero parameter function with a return value, like getter
     * @param name Name of the function
     * @param supplier Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunctionWithReturn(String name, Supplier<LuaValue> supplier){
        Object[] func = functions.getOrDefault(name, new Object[4]);
        if(func[0] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[0] = supplier;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a single parameter function with a return value
     * @param name Name of the function
     * @param function Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunctionWithReturn(String name, Function<LuaValue, LuaValue> function){
        Object[] func = functions.getOrDefault(name, new Object[4]);
        if(func[1] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[1] = function;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a double parameter function with a return value
     * @param name Name of the function
     * @param function Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunctionWithReturn(String name, BiFunction<LuaValue, LuaValue, LuaValue> function){
        Object[] func = functions.getOrDefault(name, new Object[4]);
        if(func[2] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[2] = function;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a triple parameter function with a return value
     * @param name Name of the function
     * @param function Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunctionWithReturn(String name, TriFunction<LuaValue, LuaValue, LuaValue, LuaValue> function){
        Object[] func = functions.getOrDefault(name, new Object[4]);
        if(func[3] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[3] = function;
        functions.put(name, func);
        return this;
    }

    /**
     * A terminal function to set up this object as library in the given globals
     * @param globals The environment
     */
    public void setAsLibrary(Globals globals){
        globals.set(this.type, initFunctions(new LuaTable()));
    }

    /**
     * Internal function to set up functions
     */
    @SuppressWarnings("All")
    private LuaTable initFunctions(LuaTable object){
        for(Map.Entry<String, Object[]> func : functions.entrySet()){
            object.set(func.getKey(), new LuaFunction() {
                @Override
                public LuaValue call() {
                    if(func.getValue()[0] instanceof Runnable){
                        ((Runnable)func.getValue()[0]).run();
                        return NIL;
                    } else {
                        return ((Supplier<LuaValue>) func.getValue()[0]).get();
                    }
                }

                @Override
                public LuaValue call(LuaValue arg) {
                    if(func.getValue()[1] instanceof Consumer){
                        ((Consumer<LuaValue>)func.getValue()[1]).accept(arg);
                        return NIL;
                    } else {
                        return ((Function<LuaValue, LuaValue>) func.getValue()[1]).apply(arg);
                    }
                }

                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    if(func.getValue()[2] instanceof BiConsumer){
                        ((BiConsumer<LuaValue, LuaValue>)func.getValue()[2]).accept(arg1, arg2);
                        return NIL;
                    } else {
                        return ((BiFunction<LuaValue, LuaValue, LuaValue>) func.getValue()[2]).apply(arg1, arg2);
                    }
                }

                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    if(func.getValue()[3] instanceof TriConsumer){
                        ((TriConsumer<LuaValue, LuaValue, LuaValue>)func.getValue()[3]).accept(arg1, arg2, arg3);
                        return NIL;
                    } else {
                        return ((TriFunction<LuaValue, LuaValue, LuaValue, LuaValue>) func.getValue()[3]).apply(arg1, arg2, arg3);
                    }
                }
            });
        }
        return object;
    }

    /**
     * A consumer which consumes three parameters
     * @param <I> Parameter No.1's type
     * @param <J> Parameter No.2's type
     * @param <K> Parameter No.3's type
     */
    @FunctionalInterface
    public interface TriConsumer<I,J,K> {
        void accept(I arg1, J arg2, K arg3);
    }

    /**
     * A function which turns three parameters into one return value
     * @param <I> Parameter No.1's type
     * @param <J> Parameter No.2's type
     * @param <K> Parameter No.3's type
     * @param <R> Return value's type
     */
    @FunctionalInterface
    public interface TriFunction<I,J,K,R> {
        R apply(I arg1, J arg2, K arg3);
    }
}
