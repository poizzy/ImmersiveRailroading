package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.util.DataBlock;

public class LocomotiveElectricDefinition extends LocomotiveDefinition {
    public boolean sharedPantograph;

    public LocomotiveElectricDefinition(String defID, DataBlock data) throws Exception {
        super(LocomotiveElectric.class, defID, data);
    }

    @Override
    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);
        this.sharedPantograph = data.getValue("shared_pantograph").asBoolean(false);
    }
}
