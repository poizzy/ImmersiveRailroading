package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.immersiverailroading.model.ElectricLocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;

public class LocomotiveElectricDefinition extends LocomotiveDefinition {
    public SoundDefinition idle;
    public SoundDefinition running;
    public SoundDefinition horn;
    public float enginePitchRange;
    public boolean hornSus;
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
        DataBlock sounds = data.getBlock("sounds");
        this.idle = SoundDefinition.getOrDefault(sounds, "idle");
        this.running = SoundDefinition.getOrDefault(sounds, "running");
        this.horn = SoundDefinition.getOrDefault(sounds, "horn");
        this.enginePitchRange = sounds.getValue("engine_pitch_range").asFloat();

        DataBlock properties = data.getBlock("properties");
        hornSus = properties.getValue("horn_sustained").asBoolean();
    }

    @Override
    protected Identifier defaultDataLocation() {
        // TODO add electric.caml
        return new Identifier(ImmersiveRailroading.MODID, "rolling_stock/default/diesel.caml");
    }

    @Override
    protected StockModel<?, ?> createModel() throws Exception {
        return new ElectricLocomotiveModel(this);
    }
}
