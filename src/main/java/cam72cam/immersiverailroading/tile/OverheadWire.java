package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.WireDefinition;
import cam72cam.immersiverailroading.util.WireBuilder;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

public class OverheadWire {
    @TagField
    private String definitionID;
    @TagField
    public Vec3d connectionPoint;
    @TagField
    private Vec3i parent;
    @TagField
    private Vec3i target;
    @TagField
    private int parentConnector;
    @TagField
    private int targetConnector;
    @TagField
    private Vec3d delta;

    private Model model;

    public OverheadWire() {}

    public OverheadWire(TileMast target, TileMast parent, String defID, int targetConnector, int parentConnector) {
        this.connectionPoint = parent.getLocalConnectionPoint(parentConnector, "A");
        this.definitionID = defID;
        this.target = target.getPos();
        this.parent = parent.getPos();
        this.parentConnector = parentConnector;
        this.targetConnector = targetConnector;

        this.delta = parent.getConnectionPoint(parentConnector, "A").subtract(target.getConnectionPoint(targetConnector, "A")).scale(-1);
    }

    public void render(RenderState state) {
        state.translate(connectionPoint);

        if (this.model == null) {
            World world = MinecraftClient.getPlayer().getWorld();

            TileMast parentTile = world.getBlockEntity(parent, TileMast.class);
            TileMast targetTile = world.getBlockEntity(target, TileMast.class);

            if (parentTile == null || targetTile == null) {
                // This will happen if the parent tile is loaded but the target tile not
                return;
            }

            Vec3d parentA = parentTile.getConnectionPoint(parentConnector, "A");
            Vec3d parentB = parentTile.getConnectionPoint(parentConnector, "B");
            Vec3d targetA = targetTile.getConnectionPoint(targetConnector, "A");
            Vec3d targetB = targetTile.getConnectionPoint(targetConnector, "B");

            model = WireBuilder.build(getDefinition(), targetA, targetB, parentA, parentB);
        }

        try (ModelRenderer.Binding binding = ModelRenderer.getRendererFor(model).bind(state)) {
            binding.enqueueOpaque();
        }
    }

    public Vec3d getDelta() {
        return this.delta;
    }

    public WireDefinition getDefinition() {
        return DefinitionManager.getWire(definitionID);
    }

    public void removed() {
        if (model != null) model.free();
    }

}
