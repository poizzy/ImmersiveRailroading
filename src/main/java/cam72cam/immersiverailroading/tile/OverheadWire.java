package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.library.MastConnector;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.WireDefinition;
import cam72cam.immersiverailroading.util.WireBuilder;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.serialization.TagField;

public class OverheadWire {
    @TagField
    private String definitionID;
    @TagField
    public Vec3d connectionPoint;
    @TagField
    private MastConnector connector1;
    @TagField
    private MastConnector connector2;
    @TagField
    private Vec3d parentOrigin;
    @TagField
    private Vec3d targetOrigin;
    @TagField
    private float parentRotation;
    @TagField
    private float targetRotation;

    private Model model;

    public OverheadWire() {}

    public OverheadWire(TileMast target, TileMast parent, String defID, MastConnector connector1, MastConnector connector2) {
        this.connectionPoint = parent.getConnectionPoint(connector2.id, "A");
        this.connector1 = connector1;
        this.connector2 = connector2;
        this.definitionID = defID;
        this.parentOrigin = new Vec3d(parent.getPos()).add(0.5, 0, 0.5);
        this.targetOrigin = new Vec3d(target.getPos()).add(0.5, 0, 0.5);
        this.parentRotation = parent.getAngle();
        this.targetRotation = target.getAngle();
    }

    public void render(RenderState state) {
        state.translate(connectionPoint);

        if (this.model == null) {
            model = WireBuilder.build(getDefinition(), connector1.getA(targetOrigin, targetRotation), connector1.getB(targetOrigin, targetRotation), connector2.getA(parentOrigin, parentRotation), connector2.getB(parentOrigin, parentRotation));
        }

        try (ModelRenderer.Binding binding = ModelRenderer.getRendererFor(model).bind(state)) {
            binding.enqueueOpaque();
        }
    }

    public Vec3d getDelta() {
        return connector2.getA(parentOrigin, parentRotation).subtract(connector1.getA(targetOrigin, targetRotation)).scale(-1);
    }

    public WireDefinition getDefinition() {
        return DefinitionManager.getWire(definitionID);
    }

    public void removed() {
        if (model != null) model.free();
    }

}
