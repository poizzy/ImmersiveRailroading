package cam72cam.immersiverailroading.gui.components;

import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.gui.screen.TextField;
import cam72cam.mod.text.TextColor;

import java.util.*;
import java.util.stream.Collectors;

import static cam72cam.immersiverailroading.gui.components.GuiUtils.fitString;

public abstract class ListSelector<T> {
    int width;
    T currentValue;
    Map<String, T> rawOptions;
    int page;
    int pageSize;
    boolean visible;

    TextField search;
    Button pagination;
    List<Button> options;

    Map<Button, T> usableButtons;
    Map<Button, Integer> buttonsX;
    Map<Button, Integer> buttonsY;

    public ListSelector(IScreenBuilder screen, int xOff, int width, int height, T currentValue, Map<String, T> rawOptions) {
        this.width = width;
        this.rawOptions = rawOptions;
        this.currentValue = currentValue;
        visible = false;
        page = 0;

        int xtop = -GUIHelpers.getScreenWidth() / 2 + xOff;
        int ytop = -GUIHelpers.getScreenHeight() / 4;

        search = new TextField(screen, xtop, ytop, width - 1, height);

        pagination = new Button(screen, xtop, ytop + height, width + 1, height, "") {
            @Override
            public void onClick(Player.Hand hand) {
                page += hand == Player.Hand.PRIMARY ? 1 : -1;
                updateSearch(search.getText());
            }
        };

        pageSize = Math.max(1, GUIHelpers.getScreenHeight() / height - 2);

        // Hack
        if (rawOptions.size() < pageSize) {
            ytop -= height;
        }

        options = new ArrayList<>();
        buttonsX = new HashMap<>();
        buttonsY = new HashMap<>();
        for (int i = 0; i < pageSize; i++) {
            Button btn = new Button(screen, xtop, ytop + height * 2 + i * height, width + 1, height, "") {
                @Override
                public void onClick(Player.Hand hand) {
                    ListSelector.this.currentValue = usableButtons.get(this);
                    ListSelector.this.onClick(ListSelector.this.currentValue);
                    ListSelector.this.updateSearch(search.getText());
                }
            };
            buttonsX.put(btn, xtop);
            buttonsY.put(btn, ytop + height * 2 + i * height);
            options.add(btn);
        }

        search.setValidator(s -> {
            page = 0;
            this.updateSearch(s);
            return true;
        });
        this.updateSearch("");

        this.setVisible(false);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        search.setVisible(visible && rawOptions.size() > pageSize);
        pagination.setVisible(visible && rawOptions.size() > pageSize);
        options.forEach(b -> b.setVisible(visible && !b.getText().isEmpty()));
    }

    public boolean isVisible() {
        return visible;
    }

    public abstract void onClick(T option);

    void updateSearch(String search) {
        Collection<String> names = search.isEmpty() ? rawOptions.keySet() : rawOptions.keySet().stream()
                .filter(v -> v.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        int nPages = pageSize > 0 ? (int) Math.ceil(names.size() / (float) pageSize) : 0;
        if (page >= nPages) {
            page = 0;
        }
        if (page < 0) {
            page = nPages - 1;
        }

        pagination.setText(GuiText.SELECTOR_PAGE.toString(page + 1, Math.max(1, nPages)));

        options.forEach(b -> {
            b.setText("");
            b.setVisible(false);
            b.setEnabled(false);
        });

        usableButtons = new HashMap<>();
        int bid = 0;
        for (Map.Entry<String, T> entry : rawOptions.entrySet().stream()
                .filter(e -> names.contains(e.getKey()))
                .skip((long) page * pageSize).limit(pageSize)
                .collect(Collectors.toList())) {
            Button button = options.get(bid);
            button.setEnabled(true);
            button.setVisible(true);
            String text = fitString(entry.getKey(), (int) Math.floor(width/6.0));
            if (entry.getValue() == currentValue) {
                text = TextColor.YELLOW.wrap(text);
            }
            button.setText(text);
            usableButtons.put(button, entry.getValue());

            bid++;
        }
    }

    public void render(ButtonRenderer<T> renderer) {
        if (!isVisible()) {
            return;
        }

        for (Map.Entry<Button, T> entry : usableButtons.entrySet()) {
            renderer.render(entry.getKey(), GUIHelpers.getScreenWidth()/2 + buttonsX.get(entry.getKey()), GUIHelpers.getScreenHeight()/4 + buttonsY.get(entry.getKey()), entry.getValue());
        }
    }

    @FunctionalInterface
    public interface ButtonRenderer<T> {
        void render(Button button, int x, int y, T value);
    }
}
