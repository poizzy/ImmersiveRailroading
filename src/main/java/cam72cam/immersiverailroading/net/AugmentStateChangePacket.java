package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class AugmentStateChangePacket extends Packet {
    @TagField
    private Vec3i tilePos;
    @TagField
    private String tag;

    public AugmentStateChangePacket() {
    }

    public AugmentStateChangePacket(Vec3i tilePos, String tag) {
        this.tilePos = tilePos;
        this.tag = tag;
    }

    @Override
    protected void handle() {
        if(getWorld().getBlockEntity(tilePos, TileRailBase.class) != null){
            getWorld().getBlockEntity(tilePos, TileRailBase.class).setCurrentFilter(tag);
            if(tag != null){
                getPlayer().sendMessage(ChatText.SET_AUGMENT_FILTER.getMessage(tag));
            } else {
                getPlayer().sendMessage(ChatText.RESET_AUGMENT_FILTER.getMessage());
            }
        }
    }
}
