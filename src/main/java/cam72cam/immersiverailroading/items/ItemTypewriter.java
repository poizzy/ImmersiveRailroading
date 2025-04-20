package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.RenderText;
import cam72cam.immersiverailroading.entity.TextRenderOptions;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.net.Packet;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemTypewriter extends CustomItem {
    public ItemTypewriter() {
        super(ImmersiveRailroading.MODID, "item_typewriter");
        Recipes.shapedRecipe(this, 3,
                Fuzzy.IRON_INGOT, Fuzzy.PAPER, Fuzzy.IRON_INGOT,
                Fuzzy.IRON_BLOCK, null, Fuzzy.IRON_BLOCK,
                Fuzzy.WOOD_PLANK, Fuzzy.WOOD_PLANK, Fuzzy.WOOD_PLANK);
    }

    @Override
    public int getStackSize() {
        return 1;
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.MAIN_TAB);
    }

    public static void onStockInteract(EntityRollingStock stock, Player player, Player.Hand hand) {
        if (player.getWorld().isClient) {
            GuiTypes.TEXT_FIELD.open(player);
        }
    }

    @Override
    public void onClickAir(Player player, World world, Player.Hand hand) {
        if (world.isServer) {
            player.sendMessage(ChatText.TYPEWRITER_NO_STOCK.getMessage());
        }
    }

    public static class TypewriterPacket extends Packet {
        @TagField("stock")
        public EntityRollingStock stock;
        @TagField(value = "settings", mapper = TextRenderOptions.TextRenderOptionsMapper.class)
        public TextRenderOptions settings;
        @TagField("vec3dmin")
        public Vec3d vec3dmin;
        @TagField("vec3dmax")
        public Vec3d vec3dmax;
        @TagField("vec3dNormal")
        public Vec3d vec3dNormal;
        @TagField("key")
        public String key;

        public TypewriterPacket() {

        }

        public TypewriterPacket(EntityRollingStock stock, TextRenderOptions settings, Vec3d min, Vec3d max, String key, Vec3d normal) {
            this.stock = stock;
            this.settings = settings;
            this.vec3dmax = max;
            this.vec3dmin = min;
            this.key = key;
            this.vec3dNormal = normal;
        }

        @Override
        protected void handle() {
            new TypewriterSyncPacket(stock, settings, vec3dmin, vec3dmax, key, vec3dNormal).sendToObserving(stock);
        }
    }

    public static class TypewriterSyncPacket extends Packet {
        @TagField("stock")
        public EntityRollingStock stock;
        @TagField(value = "settings", mapper = TextRenderOptions.TextRenderOptionsMapper.class)
        public TextRenderOptions settings;
        @TagField("vec3dmin")
        public Vec3d vec3dmin;
        @TagField("vec3dmax")
        public Vec3d vec3dmax;
        @TagField("vec3dNormal")
        public Vec3d vec3dNormal;
        @TagField("key")
        public String key;

        public TypewriterSyncPacket() {

        }

        public TypewriterSyncPacket(EntityRollingStock stock, TextRenderOptions settings, Vec3d min, Vec3d max, String key, Vec3d normal) {
            this.stock = stock;
            this.settings = settings;
            this.vec3dmax = max;
            this.vec3dmin = min;
            this.key = key;
            this.vec3dNormal = normal;
        }

        @Override
        protected void handle() {
            RenderText renderText = RenderText.getInstance(String.valueOf(stock.getUUID()));

            File file = new File(settings.id.getPath());
            String jsonPath = file.getName();
            Identifier jsonId = settings.id.getRelative(jsonPath.replaceAll(".png", ".json"));
            InputStream json = null;
            try {
                json = jsonId.getResourceStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            renderText.setText(settings.componentId, settings.newText, settings.id, vec3dmin, vec3dmax, json,
                    settings.resX, settings.resY, settings.align, settings.flipped, settings.fontSize, settings.fontX,
                    settings.fontGap, new Identifier(ImmersiveRailroading.MODID, "not_needed"), vec3dNormal, settings.hexCode, settings.fullbright, settings.textureHeight, settings.useAlternative, settings.lineSpacingPixels, settings.offset, key);
        }
    }
}
