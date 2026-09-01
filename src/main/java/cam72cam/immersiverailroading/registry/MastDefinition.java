package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.ModelLoader;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.resource.Identifier;

import java.util.List;

public class MastDefinition {
    public final String defID;
    public final String name;
    public final Model model;
    public final Vec3d connectionPos;

    public MastDefinition(String mastID, DataBlock data) throws Exception {
        this.defID = mastID;
        this.name = data.getValue("name").asString();
        Identifier modelIdent = data.getValue("model").asIdentifier();
        this.model = ModelLoader.load(modelIdent);

        List<String> connector = model.groups().stream().filter(m -> m.contains("CONNECTOR")).toList();
        this.connectionPos = model.centerOfGroups(connector);
    }
}
