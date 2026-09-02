package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.tile.TileMast;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;

import java.util.*;
import java.util.stream.Collectors;

public class Pantograph<T extends EntityMoveableRollingStock> {
    public String name;
    private final Vec3d center;

    public static <T extends EntityMoveableRollingStock> List<Pantograph<T>> get(ComponentProvider provider, ModelState state, ModelComponentType type, ModelComponentType.ModelPosition pos) {
        return provider.parseAll(type, pos).stream().map(part -> new Pantograph<T>(part, state)).collect(Collectors.toList());
    }

    public static <T extends EntityMoveableRollingStock> List<Pantograph<T>> get(ComponentProvider provider, ModelState state, ModelComponentType type) {
        return provider.parseAll(type).stream().map(part -> new Pantograph<T>(part, state)).collect(Collectors.toList());
    }

    public boolean isUp(EntityRollingStock stock) {
        return stock.getControlPosition(name) >= 0.75;
    }

    public void operate(EntityRollingStock stock, boolean raise) {
        stock.setControlPosition(name, raise ? 1 : 0);
        ImmersiveRailroading.info("Using pantograph %s. New state: %s", name, raise);
    }

    public Pantograph(ModelComponent part, ModelState state) {
        this.center = part.center;
        this.name = "PANTOGRAPH_" + part.id;
        state.include(part);
    }
}