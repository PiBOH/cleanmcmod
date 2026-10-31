package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import piboh.cleanmc.mod.update.ModInstaller;
import piboh.cleanmc.mod.update.ModUpdate;
import piboh.cleanmc.mod.update.ModUpdaterService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Mod Updates list, drawn as a table: icon and name, installed version, new version, checkbox.
 *
 * <p>Every row is a real button laid over the row rectangle, so the click is handled by the widget
 * system exactly like the buttons below the list: pressing anywhere on a row ticks its box. The
 * screen paints the row content on top of the (empty) button, which keeps the table look and the
 * click in one place.</p>
 */
public abstract class AbstractUpdatesScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 68;
    private static final int BOTTOM_RESERVED = 62;
    private static final int ICON = 16;
    private static final int CHECK_WIDTH = 22;
    private static final int VERSION_WIDTH = 92;

    private final Screen parent;
    private final ModUpdaterService service;
    private final Set<Path> selected = new HashSet<>();
    private final List<Button> rowButtons = new ArrayList<>();

    private int scrollRows;
    private List<ModUpdate> shown = List.of();
    private Button downloadSelected;
    private Button downloadAll;
    private double shownProgress;
    private boolean resultShown;
    private List<ModInstaller.Outcome> lastOutcomes = List.of();

    protected AbstractUpdatesScreen(Screen parent, ModUpdaterService service) {
        super(Component.translatable("cleanmcmod.screen.updates.title"));
        this.parent = parent;
        this.service = service;
    }

    @Override
    protected void init() {
        if (service.snapshot().state() == ModUpdaterService.State.IDLE) {
            service.checkAsync();
        }
        int centerX = this.width / 2;
        if (!service.delegated()) {
            this.downloadSelected = Button.builder(
                            Component.translatable("cleanmcmod.button.download_selected", 0),
                            button -> downloadSelected())
                    .bounds(centerX - 154, this.height - 50, 150, 20)
                    .build();
            this.downloadAll = Button.builder(Component.translatable("cleanmcmod.button.download_all"),
                            button -> downloadAll())
                    .bounds(centerX + 4, this.height - 50, 150, 20)
                    .build();
            this.addRenderableWidget(this.downloadSelected);
            this.addRenderableWidget(this.downloadAll);
        }

        this.rowButtons.clear();
        int visible = visibleRows();
        for (int index = 0; index < visible; index++) {
            Button row = Button.builder(Component.empty(), button -> toggleRow(button))
                    .bounds(listLeft() + 2, LIST_TOP + index * ROW_HEIGHT, listWidth() - 4,
                            ROW_HEIGHT - 2)
                    .build();
            this.rowButtons.add(row);
            this.addRenderableWidget(row);
        }

        this.addRenderableWidget(Button.builder(Component.translatable("cleanmcmod.button.refresh"),
                        button -> {
                            selected.clear();
                            scrollRows = 0;
                            resultShown = false;
                            lastOutcomes = List.of();
                            service.checkAsync();
                        })
                .bounds(centerX - 154, this.height - 26, 150, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                        button -> this.onClose())
                .bounds(centerX + 4, this.height - 26, 150, 20)
                .build());
    }

    private int visibleRows() {
        return Math.max(1, (this.height - BOTTOM_RESERVED - LIST_TOP) / ROW_HEIGHT);
    }

    private void toggleRow(Button button) {
        int index = this.rowButtons.indexOf(button) + this.scrollRows;
        if (index < 0 || index >= shown.size()) {
            return;
        }
        Path key = shown.get(index).local().path();
        if (!selected.remove(key)) {
            selected.add(key);
        }
    }

    private void downloadSelected() {
        List<ModUpdate> targets = new ArrayList<>();
        for (ModUpdate update : shown) {
            if (selected.contains(update.local().path())) {
                targets.add(update);
            }
        }
        if (!targets.isEmpty()) {
            this.minecraft.setScreenAndShow(new ConfirmInstallScreen(this, service, targets));
        }
    }

    private void downloadAll() {
        if (!shown.isEmpty()) {
            this.minecraft.setScreenAndShow(new ConfirmInstallScreen(this, service, List.copyOf(shown)));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        ModUpdaterService.Snapshot snapshot = service.snapshot();
        if (!snapshot.updates().equals(shown)) {
            shown = snapshot.updates();
            selected.retainAll(shown.stream().map(update -> update.local().path()).toList());
            scrollRows = 0;
        }
        updateButtons(snapshot);
        bindRows();

        if (!resultShown && !snapshot.busy() && !snapshot.outcomes().isEmpty()
                && snapshot.outcomes() != lastOutcomes) {
            resultShown = true;
            lastOutcomes = snapshot.outcomes();
            this.minecraft.setScreenAndShow(new UpdateResultScreen(this, lastOutcomes));
            return;
        }
        if (snapshot.state() == ModUpdaterService.State.INSTALLING) {
            resultShown = false;
        }

        int centerX = this.width / 2;
        graphics.centeredText(this.font, this.title, centerX, 14, 0xFFFFFFFF);

        if (service.delegated()) {
            graphics.centeredText(this.font, trimmed(Component.translatable("cleanmcmod.screen.updates.delegated")),
                    centerX, 28, Texts.COLOR_WARN);
        }

        switch (snapshot.state()) {
            case CHECKING -> graphics.centeredText(this.font,
                    Component.translatable("cleanmcmod.screen.updates.checking"), centerX, 40, Texts.COLOR_WARN);
            case INSTALLING -> graphics.centeredText(this.font,
                    Component.translatable("cleanmcmod.screen.updates.installing"), centerX, 40, Texts.COLOR_WARN);
            case FAILED -> graphics.centeredText(this.font,
                    trimmed(Component.translatable("cleanmcmod.screen.updates.failed",
                            snapshot.error() == null ? "" : snapshot.error())),
                    centerX, 40, Texts.COLOR_ERROR);
            case IDLE, READY -> {
                if (!snapshot.busy() && shown.isEmpty()) {
                    Component message = snapshot.scannedMods() == 0
                            ? Component.translatable("cleanmcmod.screen.updates.no_mods")
                            : Component.translatable("cleanmcmod.screen.updates.up_to_date", snapshot.scannedMods());
                    graphics.centeredText(this.font, message, centerX, LIST_TOP + 10, Texts.COLOR_OK);
                } else {
                    graphics.centeredText(this.font,
                            Component.translatable("cleanmcmod.screen.updates.found", shown.size()), centerX, 40,
                            0xFFFFFFFF);
                    renderList(graphics, mouseX, mouseY);
                }
            }
        }

        if (snapshot.busy()) {
            renderProgress(graphics, snapshot);
        } else {
            shownProgress = 0;
        }
    }

    /** A real progress bar: the fill eases towards the step count the service reports. */
    private void renderProgress(GuiGraphicsExtractor graphics, ModUpdaterService.Snapshot snapshot) {
        double target = Math.max(snapshot.progress(), 0.02);
        shownProgress += (target - shownProgress) * 0.18;
        if (target > shownProgress + 0.5) {
            shownProgress = target;
        }
        int x = listLeft();
        int width = listWidth();
        int y = 52;
        graphics.fill(x, y, x + width, y + 6, 0xFF1B1B1B);
        int filled = (int) Math.round(width * shownProgress);
        graphics.fill(x, y, x + Math.max(1, filled), y + 6, 0xFF3BAA5C);
        graphics.centeredText(this.font, Component.literal(Math.round(shownProgress * 100) + "%"),
                this.width / 2, y + 10, 0xFFFFFFFF);
    }

    private void updateButtons(ModUpdaterService.Snapshot snapshot) {
        if (this.downloadSelected != null) {
            this.downloadSelected.setMessage(Component.translatable(
                    "cleanmcmod.button.download_selected", selected.size()));
            this.downloadSelected.active = !selected.isEmpty() && !snapshot.busy();
        }
        if (this.downloadAll != null) {
            this.downloadAll.active = !shown.isEmpty() && !snapshot.busy();
        }
    }

    /** Points each pooled row button at the row it now shows, and hides the ones past the end. */
    private void bindRows() {
        int maxScroll = Math.max(0, shown.size() - visibleRows());
        if (scrollRows > maxScroll) {
            scrollRows = maxScroll;
        }
        for (int index = 0; index < rowButtons.size(); index++) {
            Button button = rowButtons.get(index);
            int data = scrollRows + index;
            boolean visible = data < shown.size();
            button.visible = visible;
            button.active = visible;
            button.setX(listLeft() + 2);
            button.setY(LIST_TOP + index * ROW_HEIGHT);
        }
    }

    private void renderList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = listLeft();
        int width = listWidth();
        int right = left + width;

        int checkX = right - CHECK_WIDTH;
        int newX = checkX - VERSION_WIDTH;
        int installedX = newX - VERSION_WIDTH;

        graphics.text(this.font, Component.translatable("cleanmcmod.screen.updates.installed"),
                installedX, LIST_TOP - 12, Texts.COLOR_INFO, false);
        graphics.text(this.font, Component.translatable("cleanmcmod.screen.updates.new"),
                newX, LIST_TOP - 12, Texts.COLOR_INFO, false);
        graphics.fill(left, LIST_TOP - 2, right, LIST_TOP - 1, 0x33FFFFFF);

        for (int index = 0; index < rowButtons.size(); index++) {
            int data = scrollRows + index;
            if (data >= shown.size()) {
                break;
            }
            ModUpdate update = shown.get(data);
            int y = LIST_TOP + index * ROW_HEIGHT;
            int rowTop = y - 2;
            int rowBottom = y + ROW_HEIGHT - 4;
            boolean isSelected = selected.contains(update.local().path());
            boolean blocked = update.blocked();
            boolean resolvable = update.resolvable();
            boolean hovered = mouseX >= left && mouseX <= right && mouseY >= rowTop && mouseY < rowBottom;

            int background = isSelected ? 0xFF20351F : hovered ? 0xFF2C2C2C : 0xFF1A1A1A;
            graphics.fill(left, rowTop, right, rowBottom, background);
            graphics.fill(left, rowTop, left + 2, rowBottom, isSelected ? 0xFF55FF55 : 0xFF3A3A3A);

            Identifier icon = ModIcons.get(update.local());
            int iconX = left + 6;
            int iconY = rowTop + 3;
            if (icon != null) {
                graphics.blit(icon, iconX, iconY, 0, 0, ICON, ICON, ICON, ICON);
            } else {
                graphics.fill(iconX, iconY, iconX + ICON, iconY + ICON, 0xFF2B2B2B);
                String initial = update.displayName().isEmpty() ? "?"
                        : update.displayName().substring(0, 1).toUpperCase(Locale.ROOT);
                graphics.centeredText(this.font, Component.literal(initial), iconX + ICON / 2, iconY + 4,
                        Texts.COLOR_INFO);
            }

            int nameX = iconX + ICON + 5;
            int nameRoom = Math.max(40, installedX - nameX - 8);
            String tag = "[" + update.candidate().source().tag() + "]";
            int tagWidth = this.font.width(tag + " ");
            graphics.text(this.font, Component.literal(tag), nameX, iconY + 1,
                    Texts.sourceColor(update.candidate().source()), false);
            String name = this.font.plainSubstrByWidth(update.displayName(), Math.max(24, nameRoom - tagWidth));
            int nameColor = blocked ? (resolvable ? Texts.COLOR_WARN : Texts.COLOR_ERROR) : 0xFFFFFFFF;
            graphics.text(this.font, Component.literal(name), nameX + tagWidth, iconY + 1, nameColor, false);

            graphics.text(this.font,
                    Component.literal(this.font.plainSubstrByWidth(update.installedVersion(), VERSION_WIDTH - 6)),
                    installedX, iconY + 1, Texts.COLOR_INFO, false);
            graphics.text(this.font,
                    Component.literal(this.font.plainSubstrByWidth(update.availableVersion(), VERSION_WIDTH - 6)),
                    newX, iconY + 1, blocked ? Texts.COLOR_WARN : Texts.COLOR_OK, false);

            int box = checkX + 4;
            graphics.fill(box, iconY, box + 12, iconY + 12, isSelected ? 0xFF2E7D32 : 0xFF3A3A3A);
            if (isSelected) {
                graphics.text(this.font, Component.literal("✔"), box + 2, iconY + 2, 0xFFFFFFFF, false);
            }

            if (hovered) {
                List<Component> tooltip = new ArrayList<>(2);
                tooltip.add(Texts.source(update.candidate().source()));
                if (update.reason() != null) {
                    tooltip.add(Component.literal(this.font.plainSubstrByWidth(update.reason(), 220)));
                }
                graphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
            }
        }

        double maxScroll = Math.max(0, shown.size() - visibleRows());
        if (maxScroll > 0) {
            int trackTop = LIST_TOP;
            int trackHeight = (this.height - BOTTOM_RESERVED) - LIST_TOP;
            int thumbHeight = Math.max(16, (int) (trackHeight * (visibleRows() / (double) shown.size())));
            int thumbTop = trackTop + (int) ((trackHeight - thumbHeight) * (scrollRows / maxScroll));
            graphics.fill(right + 4, trackTop, right + 6, trackTop + trackHeight, 0x22FFFFFF);
            graphics.fill(right + 4, thumbTop, right + 6, thumbTop + thumbHeight, 0x88FFFFFF);
        }

        graphics.centeredText(this.font,
                Component.translatable("cleanmcmod.screen.updates.hint", selected.size()),
                this.width / 2, this.height - BOTTOM_RESERVED + 4, Texts.COLOR_INFO);
    }

    private int listWidth() {
        return Math.min(this.width - 24, 480);
    }

    private int listLeft() {
        return (this.width - listWidth()) / 2;
    }

    private Component trimmed(Component text) {
        return Component.literal(this.font.plainSubstrByWidth(text.getString(), this.width - 40));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
            double verticalAmount) {
        int maxScroll = Math.max(0, shown.size() - visibleRows());
        if (maxScroll > 0) {
            int step = verticalAmount > 0 ? -1 : verticalAmount < 0 ? 1 : 0;
            this.scrollRows = Math.max(0, Math.min(maxScroll, this.scrollRows + step));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
