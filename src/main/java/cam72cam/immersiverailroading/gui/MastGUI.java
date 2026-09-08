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
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;

public class MastGUI implements IScreen {
    private final ItemMast.Data mastData;
    private TextField offsetX;
    private TextField offsetY;
    private TextField offsetZ;

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

        // Offset X
        Button offsetXLabel = new Button(iScreenBuilder, xtop, ytop, width / 2 + 10, height, "Offset X:", ((_, _) -> {}));
        offsetXLabel.setEnabled(false);
        offsetX = new TextField(iScreenBuilder, xtop + width / 2 + 10, ytop, width, height);
        offsetX.setText(String.valueOf(mastData.getOffset().x));
        offsetX.setValidator(s -> {
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
        offsetX.setFocused(true);

        ytop += height;

        // Offset Y
        Button offsetYLabel = new Button(iScreenBuilder, xtop, ytop, width / 2 + 10, height, "Offset Y:", ((_, _) -> {}));
        offsetYLabel.setEnabled(false);
        offsetY = new TextField(iScreenBuilder, xtop + width / 2 + 10, ytop, width, height);
        offsetY.setText(String.valueOf(mastData.getOffset().y));
        offsetY.setValidator(s -> {
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
        offsetY.setFocused(true);

        ytop += height;

        // Offset Z

        Button offsetZLabel = new Button(iScreenBuilder, xtop, ytop, width / 2 + 10, height, "Offset Z:", ((_, _) -> {}));
        offsetZLabel.setEnabled(false);
        offsetZ = new TextField(iScreenBuilder, xtop + width / 2 + 10, ytop, width, height);
        offsetZ.setText(String.valueOf(mastData.getOffset().z));
        offsetZ.setValidator(s -> {
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
        offsetZ.setFocused(true);

        ytop += height;
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        GUIHelpers.drawRect(200, 0, GUIHelpers.getScreenWidth() - 200, GUIHelpers.getScreenHeight(), 0xCC000000);
        GUIHelpers.drawRect(0, 0, 200, GUIHelpers.getScreenHeight(), 0xEE000000);
    }

    @Override
    public void onClose() {
        mastData.offset = new Vec3d(Double.parseDouble(offsetX.getText()), Double.parseDouble(offsetY.getText()), Double.parseDouble(offsetZ.getText()));
        mastData.write();
    }
}
