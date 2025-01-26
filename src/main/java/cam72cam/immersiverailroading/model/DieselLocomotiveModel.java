package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;

import java.util.*;

public class DieselLocomotiveModel extends LocomotiveModel<LocomotiveDiesel, LocomotiveDieselDefinition> {
    private List<ModelComponent> components;
    private DieselExhaust exhaust;
    private Horn horn;
    private final PartSound idle;
    private final PartSound running;

    private final boolean newSound;

    private Map<Integer, Queue<PartSound>> soundQueueMap = new HashMap<>();

    private Map<Integer, List<PartSound>> soundListMap = new HashMap<>();

    private final Map<String, PartSound> sounds = new HashMap<>();

    private Map<UUID, Float> runningFade = new HashMap<>();

    public DieselLocomotiveModel(LocomotiveDieselDefinition def) throws Exception {
        super(def);
        idle = def.isCabCar() ? null : new PartSound(def.idle, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::idle);
        running = def.isCabCar() || def.running == null ? null : new PartSound(def.running, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::running);

        newSound = def.useNewSound;

        for (Map.Entry<String, EntityRollingStockDefinition.SoundDefinition> entry : def.soundDefinition.entrySet()) {
            String k = entry.getKey();
            EntityRollingStockDefinition.SoundDefinition s = entry.getValue();

            if (s == null) continue;

            if (k.equalsIgnoreCase("idle")) {
                sounds.put(k, def.isCabCar() ? null : new PartSound(def.idle, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::idle));
            } else if (k.equalsIgnoreCase("running")) {
                sounds.put(k, def.isCabCar() || def.running == null ? null : new PartSound(def.running, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::running));
            } else {
                sounds.put(k, new PartSound(s, s.looping, 80, ConfigSound.SoundCategories.Locomotive.Diesel::running));
            }
        }

        int i = 0;

        Map<String, PartSound> soundsCopy = new HashMap<>(sounds);
        Set<String> processed = new HashSet<>();

        for (Map.Entry<String, PartSound> entry : soundsCopy.entrySet()) {
            String key = entry.getKey();

            if (processed.contains(key)) {
                continue;
            }

            PartSound value = entry.getValue();

            if (value.def.next == null) {
                continue;
            }
            boolean isNext = true;
            List<PartSound> next = new ArrayList<>();
            Set<String> visited = new HashSet<>();

            while (isNext) {
                if (visited.contains(key)) {
                    System.out.println("Circular dependency detected at: " + key);
                    break;
                }

                visited.add(key);
                next.add(value);
                processed.add(key);

                String nextVal = value.def.next;

                if (!sounds.containsKey(nextVal) || nextVal.equalsIgnoreCase(key)) {
                    isNext = false;
                } else {
                    value = sounds.get(nextVal);
                    sounds.remove(nextVal);
                    key = nextVal;
                }
            }

            soundListMap.put(i, next);
            i++;
        }


    }

    @Override
    protected void parseControllable(ComponentProvider provider, LocomotiveDieselDefinition def) {
        super.parseControllable(provider, def);
        addGauge(provider, ModelComponentType.GAUGE_TEMPERATURE_X, Readouts.TEMPERATURE);
        addControl(provider, ModelComponentType.ENGINE_START_X);
        addControl(provider, ModelComponentType.HORN_CONTROL_X);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, LocomotiveDieselDefinition def) {
        components = provider.parse(
                ModelComponentType.FUEL_TANK,
                ModelComponentType.ALTERNATOR,
                ModelComponentType.ENGINE_BLOCK,
                ModelComponentType.CRANKSHAFT,
                ModelComponentType.GEARBOX,
                ModelComponentType.FLUID_COUPLING,
                ModelComponentType.FINAL_DRIVE,
                ModelComponentType.TORQUE_CONVERTER
        );

        components.addAll(
                provider.parseAll(
                        ModelComponentType.PISTON_X,
                        ModelComponentType.FAN_X,
                        ModelComponentType.DRIVE_SHAFT_X
                )
        );

        rocking.include(components);

        exhaust = DieselExhaust.get(provider);
        horn = Horn.get(provider, rocking, def.horn, def.getHornSus());

        super.parseComponents(provider, def);
    }

    @Override
    protected void effects(LocomotiveDiesel stock) {
        super.effects(stock);
        exhaust.effects(stock);
        horn.effects(stock,
                stock.getHornTime() > 0 && (stock.isRunning() || stock.getDefinition().isCabCar())
                        ? stock.getDefinition().getHornSus() ? stock.getHornTime() / 10f : 1
                        : 0);
//        if (idle != null) {
//            if (stock.isRunning()) {
//                float volume = Math.max(0.1f, stock.getRelativeRPM());
//                float pitchRange = stock.getDefinition().getEnginePitchRange();
//                float pitch = (1-pitchRange) + stock.getRelativeRPM() * pitchRange;
//                if (running == null) {
//                    // Simple
//                    idle.effects(stock, volume, pitch);
//                } else {
//                    boolean isThrottledUp = stock.getRelativeRPM() > 0.01;
//                    float fade = runningFade.getOrDefault(stock.getUUID(), 0f);
//                    fade += 0.05f * (isThrottledUp ? 1 : -1);
//                    fade = Math.min(Math.max(fade, 0), 1);
//                    runningFade.put(stock.getUUID(), fade);
//
//                    idle.effects(stock, 1 - fade + 0.01f, 1);
//                    running.effects(stock, fade + 0.01f, pitch);
//                }
//            } else {
//                idle.effects(stock, false);
//                if (running != null) {
//                    running.effects(stock, false);
//                    runningFade.put(stock.getUUID(), 0f);
//                }
//            }
//        }

        for (Map.Entry<String, PartSound> sound : sounds.entrySet()) {
            String key = sound.getKey();
            PartSound value = sound.getValue();

            if (!newSound) {
                oldSound(stock, key, value);
            }

//            value.effects(stock, true);
        }

        for (Queue<PartSound> soundQueue : soundQueueMap.values()) {

        }
    }

    private void processSoundQue() {
        
    }

    private void oldSound(LocomotiveDiesel stock, String key, PartSound value) {
        if (key.equalsIgnoreCase("horn")) {
            return;
        }

        if (key.equalsIgnoreCase("idle")) {
            if (stock.isRunning()) {
                float volume = Math.max(0.1f, stock.getRelativeRPM());
                float pitchRange = stock.getDefinition().getEnginePitchRange();
                float pitch = (1-pitchRange) + stock.getRelativeRPM() * pitchRange;
                if (!sounds.containsKey("running")) {
                    value.effects(stock, volume, pitch);
                } else {
                    boolean isThrottledUp = stock.getRelativeRPM() > 0.01;
                    float fade = runningFade.getOrDefault(stock.getUUID(), 0f);
                    fade += 0.05f * (isThrottledUp ? 1 : -1);
                    fade = Math.min(Math.max(fade, 0), 1);
                    runningFade.put(stock.getUUID(), fade);

                    PartSound running = sounds.get("running");

                    running.effects(stock, fade +0.01f, pitch);
                }
            } else {
                value.effects(stock, false);
                if (sounds.containsKey("running")) {
                    sounds.get("running").effects(stock, false);
                    runningFade.put(stock.getUUID(), 0f);
                }
            }
            return;
        }
    }

    private void newSoundSystem (LocomotiveDiesel stock, String key, PartSound value) {
        EntityRollingStockDefinition.SoundDefinition soundDefinition = stock.getDefinition().soundDefinition.get(key);
        if (stock.isRunning() && key.equalsIgnoreCase("idle")) {
            if (soundDefinition.looping) {
                float volume = Math.max(0.1f, stock.getRelativeRPM());
                float pitchRange = stock.getDefinition().getEnginePitchRange();
                float pitch = (1-pitchRange) + stock.getRelativeRPM() * pitchRange;
                value.effects(stock, volume, pitch);
            }
        }
    }

    @Override
    protected void removed(LocomotiveDiesel stock) {
        super.removed(stock);
        horn.removed(stock);
        sounds.forEach((k, v) -> {
            v.removed(stock);
        });
        if (idle != null) {
            idle.removed(stock);
        }
        if (running != null) {
            running.removed(stock);
        }
    }
}
