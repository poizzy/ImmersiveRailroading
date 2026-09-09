package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.WireDefinition;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.immersiverailroading.util.WireBuilder;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.serialization.TagField;
import org.glassfish.jaxb.core.v2.model.core.EnumLeafInfo;

public class OverheadWire {
    @TagField
    private String definitionID;
    @TagField
    public Vec3d delta;
    @TagField
    public Vec3d connectionPoint;

    private Model model;

    public OverheadWire() {}

    public OverheadWire(TileMast mast1, TileMast mast2, String defID, String firstConnector, String secondConnector) {
        this.connectionPoint = mast2.getConnectionPoint(secondConnector).subtract(new Vec3d(mast2.getPos()));
        this.delta = mast2.getConnectionPoint(secondConnector).subtract(mast1.getConnectionPoint(firstConnector));
        this.definitionID = defID;
    }

    public void render(RenderState state) {
        state.translate(connectionPoint);

        if (this.model == null) {
            model = WireBuilder.build(getDefinition(), delta.scale(-1));
        }

        try (ModelRenderer.Binding binding = ModelRenderer.getRendererFor(model).bind(state)) {
            binding.enqueueOpaque();
        }
    }

    public WireDefinition getDefinition() {
        return DefinitionManager.getWire(definitionID);
    }

    public void removed() {
        if (model != null) model.free();
    }

}
