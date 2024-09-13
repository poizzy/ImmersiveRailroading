package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.model.part.PartSound;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;

import java.util.HashMap;
import java.util.Map;

public class SetSound {
    private PartSound idle;
    private PartSound newIdle;
    private static Map<String, SetSound> instances = new HashMap<>();
    private PartSound newRunning;
    private PartSound running;

    private SetSound() {
        this.idle = null;
        this.newIdle = null;
    }

    public static SetSound getInstance(String name) {
        SetSound instance = instances.get(name);
        if (instance == null) {
            instance = new SetSound();
            instances.put(name, instance);
        }
        return instance;
    }

    public void defaultIdle(LocomotiveDieselDefinition def) {
        idle = def.isCabCar() ? null : new PartSound(def.idle, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::idle);
    }

    public void newIdle(LocomotiveDieselDefinition def, EntityRollingStockDefinition.SoundDefinition sound){
        newIdle = def.isCabCar() ? null : new PartSound(sound, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::idle);
    }

    public PartSound getIdle(){
        if (newIdle != null) {
            return newIdle;
        }else {
            return idle;
        }
    }

    public void newRunning(LocomotiveDieselDefinition def, EntityRollingStockDefinition.SoundDefinition sound) {
        newRunning = def.isCabCar() ? null : new PartSound(sound, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::running);
    }

    public void defaultRunning(LocomotiveDieselDefinition def) {
        running = def.isCabCar() || def.running == null ? null : new PartSound(def.running, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::running);
    }

    public PartSound getRunning() {
        if (newRunning != null) {
            return newRunning;
        } else {
            return running;
        }
    }
}
