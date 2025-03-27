package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.gui.helpers.MouseHelper;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.gui.screen.TextField;
import cam72cam.mod.render.opengl.RenderState;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSearchGui<T> {
    protected TextField searchField;
    protected Button dropdownToggle;
    protected boolean isDropdownOpen = false;

    private int guiLeft, guiTop;
    private static final int DROPDOWN_HEIGHT = 80;
    private static final int SCRIPT_LINE_HEIGHT = 12;

    protected String lastInput = "";

    protected List<T> candidate;
    protected List<T> filtered;
    protected String tooltip;
    protected T current;
    protected int selectedItemIndex = -1;

    public AbstractSearchGui() {}

    public void init(IScreenBuilder screen) {
        guiLeft = (int) (GUIHelpers.getScreenWidth() / 1.5);
        guiTop = (GUIHelpers.getScreenHeight() / 4);

        searchField = new TextField(screen, guiLeft - (GUIHelpers.getScreenWidth() / 2), guiTop - (guiTop / 45), 120, 14);

        dropdownToggle = new Button(screen, guiLeft - (GUIHelpers.getScreenWidth() / 2) + (120 - 14), guiTop - (guiTop / 45), 14, 14, "▼") {
            @Override
            public void onClick(Player.Hand hand) {
                isDropdownOpen = !isDropdownOpen;
            }
        };

        if (this.current != null) {
            searchField.setText(this.current.toString());
        }

        updateFilter();
    }

    public void updateFilter() {
        String query = searchField.getText().toLowerCase();
        if (query.isEmpty()) {
            filtered = candidate;
        } else {
            filtered = new ArrayList<>();
            for (T element : candidate) {
                if (element.toString() != null && element.toString().toLowerCase().contains(query)) {
                    filtered.add(element);
                }
            }
        }
    }

    public abstract void onEnterKey(IScreenBuilder builder);

    public abstract void onClose();

    public void draw(IScreenBuilder builder, RenderState state) {
        if (!searchField.getText().equals(lastInput)) {
            updateFilter();
            lastInput = searchField.getText();
        }

        GUIHelpers.drawRect(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0xCC000000);
        state.depth_test(true);

        GUIHelpers.drawCenteredString(tooltip, guiLeft / 2, guiTop + (GUIHelpers.getScreenHeight() / 4), 0xFFD3D3D3);

        if (isDropdownOpen) {
            dropdownToggle.setText("▲");

            int dropdownX = guiLeft;
            int dropdownY = guiTop + (GUIHelpers.getScreenHeight() / 4) + 14;
            int dropdownWidth = 120;

            GUIHelpers.drawRect(dropdownX, dropdownY, dropdownWidth, DROPDOWN_HEIGHT, 0xFF505050);

            int visibleScripts = DROPDOWN_HEIGHT / SCRIPT_LINE_HEIGHT;
            for (int i = 0; i < Math.min(filtered.size(), visibleScripts); i++) {
                String string = filtered.get(i).toString();
                int lineY = dropdownY + i * SCRIPT_LINE_HEIGHT;

                if (MouseHelper.mouseY >= lineY && MouseHelper.mouseY < lineY + SCRIPT_LINE_HEIGHT && MouseHelper.mouseX >= dropdownX && MouseHelper.mouseX < dropdownX + dropdownWidth) {
                    GUIHelpers.drawRect(dropdownX, lineY,  dropdownWidth, SCRIPT_LINE_HEIGHT, 0xFF808080);
                }

                GUIHelpers.drawString(string, dropdownX + 2, lineY + 2, 0xFFFFFF);
            }
        } else {
            dropdownToggle.setText("▼");
        }

        if (MouseHelper.clicked) {
            this.mouseClicked(MouseHelper.mouseClickedX, MouseHelper.mouseClickedY, MouseHelper.button);
        }
    }

    private void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isDropdownOpen) {
            int dropdownX = guiLeft;
            int dropdownY = guiTop + (GUIHelpers.getScreenHeight() / 4) + 14;
            int dropdownWidth = 120;
            Rectangle2D rectangle = new Rectangle(dropdownX, dropdownY, dropdownWidth, DROPDOWN_HEIGHT);

            if (rectangle.contains(mouseX, mouseY) && mouseButton == 0) {
                int index = (mouseY - dropdownY) / SCRIPT_LINE_HEIGHT;

                if (index >=  0 && index < filtered.size()) {
                    selectedItemIndex = index;

                    current = filtered.get(selectedItemIndex);

                    searchField.setText(current.toString());

                    isDropdownOpen = false;
                }
            }
        }
    }
}
