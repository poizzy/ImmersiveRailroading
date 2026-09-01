package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;

public class LocomotiveElectricDefinition extends LocomotiveDefinition {
    public boolean sharedPantograph;

    public LocomotiveElectricDefinition(String defID, DataBlock data) throws Exception {
        super(LocomotiveElectric.class, defID, data);
    }

    @Override
    protected GuiBuilder getDefaultOverlay(DataBlock data) throws IOException {
        return GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/cab_car.caml"));
    }

    @Override
    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);
        this.sharedPantograph = data.getValue("shared_pantograph").asBoolean(false);
    }
}
