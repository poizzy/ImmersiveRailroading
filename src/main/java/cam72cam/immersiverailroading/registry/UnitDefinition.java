package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.util.DataBlock;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class UnitDefinition {
    private final DataBlock block;
    public String defId;
    public String name;
    public Set<String> tooltips;
    public LinkedList<Stock> unitList = new LinkedList<>();

    public UnitDefinition(String defId, DataBlock block) {
        this.defId = defId;
        this.block = block;
    }

    public void initDefinitions() {
        if (block == null) {
            return;
        }

        name = block.getValue("name").asString("");

        List<DataBlock.Value> tooltips = block.getValues("add_tooltip");
        if (tooltips != null) {
            this.tooltips = tooltips.stream().map(DataBlock.Value::asString).collect(Collectors.toSet());
        }

        List<DataBlock> consist = block.getBlocks("consist");
        for (DataBlock dataBlock : consist) {
            Map<String, DataBlock.Value> valueMap = dataBlock.getValueMap();
            String stock = valueMap.get("stock").asString();
            // TODO what to do if stock doesn't exist
            EntityRollingStockDefinition stockDef = DefinitionManager.getDefinitions().stream().filter(s -> s.defID.contains(stock)).findFirst().orElseThrow(() -> new NoSuchElementException(String.format("Stock %s isn't loaded! This consist will not work %s", stock, name)));

            String texture = valueMap.get("texture").asString(null);
            boolean flipped = valueMap.get("flipped").asBoolean(false);

            Stock s = new Stock(stockDef, flipped, texture);
            unitList.add(s);
        }

    }

    public static class Stock {
        public EntityRollingStockDefinition definition;
        public boolean flipped;
        public String texture;

        public Stock(EntityRollingStockDefinition stock, boolean flipped, @Nullable String texture) {
            this.definition = stock;
            this.flipped = flipped;
            this.texture = texture;
        }
    }
}
