package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.entity.EntityRollingStock;

import java.util.HashMap;
import java.util.Map;

public interface ReadoutsEventHandler {

    Map<String, Float> readoutState = new HashMap<>();

    default void getReadout() {
        for (Readouts readout : Readouts.values()) {
            Float readoutValue = readout.getValue((EntityRollingStock) this);
            readoutState.put(readout.toString(), readoutValue);
        }
    }

}
