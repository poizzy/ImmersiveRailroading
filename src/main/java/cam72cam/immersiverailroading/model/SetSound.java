package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.model.part.PartSound;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;

public class SetSound {
    private PartSound idle;
    private static SetSound instance;

    private SetSound() {
        this.idle = null;
    }

    public static SetSound getInstance() {
        if (instance == null) {
            instance = new SetSound();
        }
        return instance;
    }

    public void newIdle(LocomotiveDieselDefinition def, EntityRollingStockDefinition.SoundDefinition sound){
        idle = def.isCabCar() ? null : new PartSound(sound, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::idle);
    }

    public PartSound getIdle(){
        return idle;
    }
}
