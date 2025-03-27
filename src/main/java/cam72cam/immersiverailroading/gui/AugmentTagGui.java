package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.net.AugmentStateChangePacket;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderState;

import java.util.ArrayList;
import java.util.Objects;

public class AugmentTagGui extends AbstractSearchGui<String> implements IScreen {
    private final TileRailBase tileRailBase;

    public AugmentTagGui(TileRailBase tileRailBase) {
        this.tileRailBase = tileRailBase;
    }

    @Override
    public void init(IScreenBuilder screen) {
        Player player = MinecraftClient.getPlayer();
        this.candidate = new ArrayList<>();
        if(player != null && player.getHeldItem(Player.Hand.PRIMARY).is(IRItems.ITEM_ROLLING_STOCK)) {
            ItemStack stack = player.getHeldItem(Player.Hand.PRIMARY);
            EntityRollingStockDefinition definition = new  ItemRollingStock.Data(stack).def;
            candidate.add(definition.name);
            if(!Objects.equals(definition.packName, "N/A")){
                candidate.add(definition.packName);
            }
            if(!Objects.equals(definition.packName, "N/A")){
                candidate.add(definition.modelerName);
            }
            candidate.addAll(definition.tags);
        }
        this.current = tileRailBase.getCurrentFilter();
        this.tooltip = "Select augment filter";
        super.init(screen);
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {
        builder.close();
    }

    @Override
    public void onClose() {
        if (current != null) {
            tileRailBase.setCurrentFilter(current);
            new AugmentStateChangePacket(tileRailBase.getPos(), this.current).sendToServer();
        }
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        super.draw(builder, state);
    }
}
