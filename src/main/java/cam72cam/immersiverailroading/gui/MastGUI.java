package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.items.ItemMast;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.gui.screen.TextField;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderState;

public class MastGUI implements IScreen {
    private final ItemMast.Data mastData;
    private TextField offset;

    public MastGUI() {
        ItemStack stack = MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY);
        mastData = new ItemMast.Data(stack);
    }


    @Override
    public void init(IScreenBuilder iScreenBuilder) {
        int xtop = -GUIHelpers.getScreenWidth() / 2;
        int ytop = -GUIHelpers.getScreenHeight()/4;
        int width = 200;
        int height = 20;

        Button offsetLabel = new Button(iScreenBuilder, xtop, ytop, width / 2 + 10, height, GuiText.MAST_LENGTH.toString(), ((_, _) -> {}));
        offsetLabel.setEnabled(false);
        offset = new TextField(iScreenBuilder, xtop, ytop, width, height);
        offset.setText(String.valueOf(mastData.getDistance()));
        offset.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            try {
                Float.parseFloat(s);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        offset.setFocused(true);
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        GUIHelpers.drawRect(200, 0, GUIHelpers.getScreenWidth() - 200, GUIHelpers.getScreenHeight(), 0xCC000000);
        GUIHelpers.drawRect(0, 0, 200, GUIHelpers.getScreenHeight(), 0xEE000000);
    }

    @Override
    public void onClose() {
        mastData.distance = Float.parseFloat(offset.getText());
        mastData.write();
    }
}
