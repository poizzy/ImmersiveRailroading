package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.Horn;
import cam72cam.immersiverailroading.model.part.PartSound;
import cam72cam.immersiverailroading.registry.LocomotiveElectricDefinition;
import cam72cam.immersiverailroading.util.MathUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ElectricLocomotiveModel extends LocomotiveModel<LocomotiveElectric, LocomotiveElectricDefinition> {
    private Horn horn;
    private PartSound idle;
    private PartSound running;

    private Map<UUID, Float> runningFade = new HashMap<>();

    public ElectricLocomotiveModel(LocomotiveElectricDefinition def) throws Exception {
        super(def);
        idle = def.isCabCar() ? null : new PartSound(def.idle, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::idle);
        running = def.isCabCar() || def.running == null ? null : new PartSound(def.running, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::running);
    }

    @Override
    protected void parseControllable(ComponentProvider provider, LocomotiveElectricDefinition def) {
        super.parseControllable(provider, def);
        addControl(provider, ModelComponentType.ENGINE_START_X);
        addControl(provider, ModelComponentType.HORN_CONTROL_X);
        addControl(provider, ModelComponentType.BATTERY_SWITCH_X);
    }

    @Override
    protected void effects(LocomotiveElectric stock) {
        super.effects(stock);
        horn.effects(stock, stock.getHornTime() > 0 && (stock.isRunning() || stock.getDefinition().isCabCar()) ? stock.getDefinition().hornSus ? stock.getHornTime() / 10f : 1 : 0);

        if (idle != null) {
            if (stock.isRunning()) {
                float volume = Math.max(0.1f, stock.getRelativeRPM());
                float pitchRange = stock.getDefinition().enginePitchRange;
                float pitch = (1-pitchRange) + stock.getRelativeRPM() * pitchRange;
                if (running == null) {
                    // Simple
                    idle.effects(stock, volume, pitch);
                } else {
                    boolean isThrottledUp = stock.getRelativeRPM() > 0.01;
                    float fade = runningFade.getOrDefault(stock.getUUID(), 0f);
                    fade += 0.05f * (isThrottledUp ? 1 : -1);
                    fade = MathUtil.clamp(fade, 0, 1);
                    runningFade.put(stock.getUUID(), fade);

                    idle.effects(stock, 1 - fade + 0.01f, 1);
                    running.effects(stock, fade + 0.01f, pitch);
                }
            } else {
                idle.effects(stock, false);
                if (running != null) {
                    running.effects(stock, false);
                    runningFade.put(stock.getUUID(), 0f);
                }
            }
        }
    }

    @Override
    protected void removed(LocomotiveElectric stock) {
        super.removed(stock);
        horn.removed(stock);
        if (idle != null) {
            idle.removed(stock);
        }
        if (running != null) {
            running.removed(stock);
        }
    }

    @Override
    protected void parseComponents(ComponentProvider provider, LocomotiveElectricDefinition def) {
        super.parseComponents(provider, def);
        horn = Horn.get(provider, rocking, def.horn, def.hornSus);
    }
}
