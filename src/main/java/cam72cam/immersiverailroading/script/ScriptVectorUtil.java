package cam72cam.immersiverailroading.script;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class ScriptVectorUtil {
    public static LuaTable constructVec3Table(Vec3i vec3i){
        return constructVec3Table(vec3i.x, vec3i.y, vec3i.z);
    }

    public static LuaTable constructVec3Table(Vec3d vec3d){
        return constructVec3Table(vec3d.x, vec3d.y, vec3d.z);
    }

    public static LuaTable constructVec3Table(LuaValue x, LuaValue y, LuaValue z){
        return constructVec3Table(x.todouble(), y.todouble(), z.todouble());
    }

    public static LuaTable constructVec3Table(double x, double y, double z){
        LuaTable vector3d = new LuaTable();
        vector3d.set("x", x);
        vector3d.set("y", y);
        vector3d.set("z", z);
        vector3d.set("add", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                vector3d.set("x", vector3d.get("x").add(arg.get("x")));
                vector3d.set("y", vector3d.get("y").add(arg.get("y")));
                vector3d.set("z", vector3d.get("z").add(arg.get("z")));
                return LuaValue.NIL;
            }
        });
        vector3d.set("scale", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                vector3d.set("x", vector3d.get("x").add(arg.get("x")));
                vector3d.set("y", vector3d.get("y").add(arg.get("y")));
                vector3d.set("z", vector3d.get("z").add(arg.get("z")));
                return LuaValue.NIL;
            }
        });
        vector3d.set("lengthSquared", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return vector3d.get("x").mul(vector3d.get("x")).add(
                        vector3d.get("y").mul(vector3d.get("y")).add(
                                vector3d.get("z").mul(vector3d.get("z"))));
            }
        });
        return vector3d;
    }

    public static Vec3i convertToVec3i(LuaValue vector){
        return new Vec3i(vector.get("x").toint(), vector.get("y").toint(), vector.get("z").toint());
    }
}
