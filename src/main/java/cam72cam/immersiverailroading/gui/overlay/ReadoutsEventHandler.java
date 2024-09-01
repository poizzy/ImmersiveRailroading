package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.entity.EntityRollingStock;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public interface ReadoutsEventHandler {

    ConcurrentMap<Readouts, Float> readoutState = new ConcurrentHashMap<>();

    default void getReadout() {
        for (Readouts readout : Readouts.values()) {
            Float readoutValue = readout.getValue((EntityRollingStock) this);
            Float previousValue = readoutState.get(readout);

            if (!Objects.equals(readoutValue, previousValue)) {
                readoutEvent(readout, (previousValue != null ? previousValue : 0), readoutValue);
            }
            readoutState.put(readout, readoutValue);
        }
    }

    default void readoutEvent(Readouts readout, float oldVal, float newVal) {

    }
}
