package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.entity.physics.SimulationState;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.physics.MovementTrack;
import cam72cam.immersiverailroading.registry.LocomotiveElectricDefinition;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.serialization.TagField;

import java.util.List;

public class LocomotiveElectric extends Locomotive {
    @TagSync
    @TagField("main_switch")
    public boolean mainSwitch = false;
    @TagSync
    @TagField("electricalConnection")
    public boolean electricalConnection;
    @TagSync
    @TagField
    public boolean pantographState = false;

    public LocomotiveElectric() {}

    public boolean isRunning() {
        return mainSwitch && electricalConnection;
    }

    public void updateElectricalConnection() {
        this.electricalConnection =  isOnPoweredRail() && pantographState;
    }

    public void updatePantographState() {
        if (getDefinition().sharedPantograph) {
            for (DirectionalStock d : getDirectionalTrain(false)) {
                pantographState = d.stock.getDefinition().getModel().pantographs.stream().anyMatch(p -> p.isUp(this));
                if (pantographState) {
                    break;
                }
            }
        } else {
            pantographState = getDefinition().getModel().pantographs.stream().anyMatch(p -> p.isUp(this));
        }
    }

    public void operatePantograph(String name) {
        if (getDefinition().sharedPantograph) {
            mapTrain(this, false, (s) -> {
                s.getDefinition().getModel().pantographs.stream().filter(p -> p.name.equals(name)).forEach(p -> p.operate(this, !pantographState));
            });
        } else {
            getDefinition().getModel().pantographs.stream().filter(p -> p.name.equals(name)).forEach(p -> p.operate(this, !pantographState));
        }
    }

    private boolean isOnPoweredRail() {
        SimulationState state = getCurrentState();
        if (state == null) {
            return false;
        }
        ITrack track = MovementTrack.findTrack(getWorld(), state.position, state.yaw, gauge.value());
        if (track instanceof TileRailBase base) {
            return base.hasElectricalPower();
        }
        return false;
    }

    @Override
    public void onTick() {
        super.onTick();
        if (getWorld().isServer && getTickCount() % 20 == 0) {
            updateElectricalConnection();
            updatePantographState();
        }
    }

    @Override
    public LocomotiveElectricDefinition getDefinition() {
        return getDefinition(LocomotiveElectricDefinition.class);
    }

    @Override
    public void handleKeyPress(Player source, KeyTypes key, boolean disableIndependentThrottle) {
        switch (key) {
            case START_STOP_ENGINE -> {
                if (pantographState) {
                    setMainSwitch(!isMainSwitchOn());
                } else {
                    source.sendMessage(ChatText.STOCK_NO_PANTOGRAPH.getMessage());
                }
            }
            case PANTOGRAPH_1 -> operatePantograph("PANTOGRAPH_1");
            case PANTOGRAPH_2 -> operatePantograph("PANTOGRAPH_2");
            default -> super.handleKeyPress(source, key, disableIndependentThrottle);
        }
    }

    public void setMainSwitch(boolean value) {
        mainSwitch = value;
        setControlPositions(ModelComponentType.ENGINE_START_X, mainSwitch ? 1 : 0);
    }

    public boolean isMainSwitchOn() {
        return mainSwitch;
    }

    @Override
    public void setReverser(float newReverser) {
        super.setReverser(Math.round(newReverser));
    }

    @Override
    protected float getReverserDelta() {
        return 0.51f;
    }

    @Override
    public double getAppliedTractiveEffort(Speed speed) {
        if (isRunning()) {
            double maxPower_W = getDefinition().getWatt(gauge);
            // Approximation by claude
            double efficiency = 0.92;
            double speedMS = Math.abs(speed.metric()) / 3.6;

            double maxStaticEffort = getStaticTractiveEffort(speed);

            double baseSpeedMS = (maxPower_W * efficiency) / Math.max(1.0, maxStaticEffort);

            double appliedMagnitude;
            if (speedMS <= baseSpeedMS) {
                appliedMagnitude = maxStaticEffort * getThrottle();
            } else {
                appliedMagnitude = (maxPower_W * efficiency / speedMS) * getThrottle();
            }

            return appliedMagnitude * getReverser();
        }
        return 0;
    }

    @Override
    public boolean providesElectricalPower() {
        return electricalConnection;
    }

    @Override
    public FluidQuantity getTankCapacity() {
        return FluidQuantity.ZERO;
    }

    @Override
    public List<Fluid> getFluidFilter() {
        return List.of();
    }

    @Override
    public int getInventoryWidth() {
        return 0;
    }
}
