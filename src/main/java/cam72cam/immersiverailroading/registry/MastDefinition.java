package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.MastConnector;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.ModelLoader;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagSerializer;
import scala.util.matching.Regex;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MastDefinition {
    public final String defID;
    public final String name;
    public final Model model;
    public final Map<String, Vec3d> connectorPos = new HashMap<>();
    public final Map<Integer, MastConnector> connectors = new HashMap<>();

    public MastDefinition(String mastID, DataBlock data) throws Exception {
        this.defID = mastID;
        this.name = data.getValue("name").asString();
        Identifier modelIdent = data.getValue("model").asIdentifier();
        this.model = ModelLoader.load(modelIdent);

        Pattern connectorPattern = Pattern.compile("CONNECTOR_(\\d+)_([AB])");
        List<ModelGroup> connectorGroups = model.getGroups().entrySet().stream().filter(e -> e.getKey().contains("CONNECTOR")).map(Map.Entry::getValue).toList();
        for (ModelGroup con : connectorGroups) {
            Matcher matcher = connectorPattern.matcher(con.name);
            if (!matcher.matches()) {
                ImmersiveRailroading.error("Mast %s has an invalid connector: %s", defID, con);
            }

            int id = Integer.parseInt(matcher.group(1));
            String type = matcher.group(2);

            MastConnector connector = connectors.computeIfAbsent(id, MastConnector::new);

            Vec3d max = con.max();
            Vec3d min = con.min();
            Vec3d center = new Vec3d((min.x + max.x) / 2.0, (min.y + max.y) / 2.0, (min.z + max.z) / 2.0);

            switch (type) {
                case "A" -> connector.a = center;
                case "B" -> connector.b = center;
                case null, default -> ImmersiveRailroading.error("Invalid connectorType on mast %s, type needs to be either A or B");
            }
        }
    }
}
