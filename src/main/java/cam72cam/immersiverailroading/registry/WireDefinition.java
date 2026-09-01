package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.util.DataBlock;

import java.util.ArrayList;
import java.util.List;

public class WireDefinition {
    public String defID;
    public String name;
    public List<Wire> wires;
    public List<Connector> connectors;

    public WireDefinition(String defID, DataBlock data) throws Exception {
        this.defID = defID;
        this.name = data.getValue("name").asString();
        this.wires = new ArrayList<>();
        for (DataBlock d : data.getBlocks("wires")) {
            wires.add(new Wire(d));
        }
        this.connectors = new ArrayList<>();
        for (DataBlock d : data.getBlocks("connectors")) {
            connectors.add(new Connector(d));
        }
    }

    public static class Wire {
        public String name;
        public float width;
        public float sagRatio;
        public String color;
        public int segments;
        public float yOffset;

        public Wire(DataBlock data) {
            this.name = data.getValue("name").asString();
            this.width = data.getValue("width").asFloat();
            this.sagRatio = data.getValue("sagRatio").asFloat();
            this.color = data.getValue("color").asString();
            this.segments = data.getValue("segments").asInteger(24);
            this.yOffset = data.getValue("yOffset").asFloat(0);
        }
    }

    public static class Connector {
        public String from;
        public String to;
        public float spacing;
        public float width;
        public boolean excludeEnds;
        public String color;

        public Connector(DataBlock data) {
            this.from = data.getValue("from").asString();
            this.to = data.getValue("to").asString();
            this.spacing = data.getValue("spacing").asFloat();
            this.width = data.getValue("width").asFloat();
            this.excludeEnds = data.getValue("excludeEnds").asBoolean(true);
            this.color = data.getValue("color").asString();
        }
    }
}
