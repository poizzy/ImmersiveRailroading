package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.WireDefinition;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.immersiverailroading.util.WireBuilder;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.serialization.TagField;

public class OverheadWire {
    @TagField
    private String definitionID;
    @TagField
    public Vec3d delta;
    @TagField
    public Vec3d connectionPoint;

    private DirectDraw model;

    public OverheadWire() {}

    public OverheadWire(TileMast mast1, TileMast mast2, String defID, String firstConnector, String secondConnector) {
        this.connectionPoint = mast2.getConnectionPoint(secondConnector).subtract(new Vec3d(mast2.getPos()));
        this.delta = mast2.getConnectionPoint(secondConnector).subtract(mast1.getConnectionPoint(firstConnector));
        this.definitionID = defID;
    }

    private DirectDraw getOrCreateModel() {
        if (this.model == null) {
            this.model = WireBuilder.build(getDefinition(), delta.scale(-1));
        }
        return model;
    }

    public void render(RenderState state) {
        state.translate(connectionPoint);
        getOrCreateModel().draw(state);
    }

    public WireDefinition getDefinition() {
        return DefinitionManager.getWire(definitionID);
    }

}
