package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
 * A click anywhere on a row ticks its box; the download buttons use the boxes, and a single finished
 * download shows the result alert the player can read without hunting through the log.
 *
 * <p>The concrete {@code ModUpdatesScreen} is provided per Minecraft input API, because the click
 * signature changed in 1.21.9.</p>
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

    private double scroll;
    private List<ModUpdate> shown = List.of();
    private ButtonWidget downloadSelected;
    private ButtonWidget downloadAll;
    private double shownProgress;
    private boolean resultShown;
    private List<ModInstaller.Outcome> lastOutcomes = List.of();

    protected AbstractUpdatesScreen(Screen parent, ModUpdaterService service) {
        super(Text.translatable("cleanmcmod.screen.updates.title"));
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
            this.downloadSelected = ButtonWidget.builder(
                            Text.translatable("cleanmcmod.button.download_selected", 0),
                            button -> downloadSelected())
                    .dimensions(centerX - 154, this.height - 50, 150, 20)
                    .build();
            this.downloadAll = ButtonWidget.builder(Text.translatable("cleanmcmod.button.download_all"),
                            button -> downloadAll())
                    .dimensions(centerX + 4, this.height - 50, 150, 20)
                    .build();
            this.addDrawableChild(this.downloadSelected);
            this.addDrawableChild(this.downloadAll);
        }
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cleanmcmod.button.refresh"),
                        button -> {
                            selected.clear();
                            scroll = 0;
                            resultShown = false;
                            lastOutcomes = List.of();
                            service.checkAsync();
                        })
                .dimensions(centerX - 154, this.height - 26, 150, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(centerX + 4, this.height - 26, 150, 20)
                .build());
    }

    private void downloadSelected() {
        List<ModUpdate> targets = new ArrayList<>();
        for (ModUpdate update : shown) {
            if (selected.contains(update.local().path())) {
                targets.add(update);
            }
        }
        if (!targets.isEmpty()) {
            this.client.setScreen(new ConfirmInstallScreen(this, service, targets));
        }
    }

    private void downloadAll() {
        if (!shown.isEmpty()) {
            this.client.setScreen(new ConfirmInstallScreen(this, service, List.copyOf(shown)));
        }
    }

    @Override
    public void tick() {
        super.tick();
        ModUpdaterService.Snapshot snapshot = service.snapshot();
        if (snapshot.state() == ModUpdaterService.State.INSTALLING) {
            resultShown = false;
            return;
        }
        if (!resultShown && snapshot.state() == ModUpdaterService.State.READY
                && !snapshot.outcomes().isEmpty() && snapshot.outcomes() != lastOutcomes) {
            resultShown = true;
            lastOutcomes = snapshot.outcomes();
            this.client.setScreen(new UpdateResultScreen(this, lastOutcomes));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        ModUpdaterService.Snapshot snapshot = service.snapshot();
        if (!snapshot.updates().equals(shown)) {
            shown = snapshot.updates();
            selected.retainAll(shown.stream().map(update -> update.local().path()).toList());
        }
        updateButtons(snapshot);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 14, 0xFFFFFFFF);

        if (service.delegated()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    trimmed(Text.translatable("cleanmcmod.screen.updates.delegated")),
                    centerX, 28, Texts.COLOR_WARN);
        }

        switch (snapshot.state()) {
            case CHECKING -> context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.updates.checking"), centerX, 40, Texts.COLOR_WARN);
            case INSTALLING -> context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.updates.installing"), centerX, 40, Texts.COLOR_WARN);
            case FAILED -> context.drawCenteredTextWithShadow(this.textRenderer,
                    trimmed(Text.translatable("cleanmcmod.screen.updates.failed",
                            snapshot.error() == null ? "" : snapshot.error())),
                    centerX, 40, Texts.COLOR_ERROR);
            case IDLE, READY -> {
                if (!snapshot.busy() && shown.isEmpty()) {
                    Text message = snapshot.scannedMods() == 0
                            ? Text.translatable("cleanmcmod.screen.updates.no_mods")
                            : Text.translatable("cleanmcmod.screen.updates.up_to_date", snapshot.scannedMods());
                    context.drawCenteredTextWithShadow(this.textRenderer, message, centerX, LIST_TOP + 10,
                            Texts.COLOR_OK);
                } else {
                    context.drawCenteredTextWithShadow(this.textRenderer,
                            Text.translatable("cleanmcmod.screen.updates.found", shown.size()), centerX, 40,
                            0xFFFFFFFF);
                    renderList(context, snapshot, mouseX, mouseY);
                }
            }
        }

        if (snapshot.busy()) {
            renderProgress(context, snapshot);
        } else {
            shownProgress = 0;
        }
    }

    /** A real progress bar: the fill eases towards the step count the service reports. */
    private void renderProgress(DrawContext context, ModUpdaterService.Snapshot snapshot) {
        double target = Math.max(snapshot.progress(), snapshot.progressTotal() == 0 ? 0.02 : 0.02);
        shownProgress += (target - shownProgress) * 0.18;
        if (target > shownProgress + 0.5) {
            shownProgress = target;
        }
        int x = listLeft();
        int width = listWidth();
        int y = 52;
        context.fill(x, y, x + width, y + 6, 0xFF1B1B1B);
        int filled = (int) Math.round(width * shownProgress);
        context.fill(x, y, x + Math.max(1, filled), y + 6, 0xFF3BAA5C);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(Math.round(shownProgress * 100) + "%"), this.width / 2, y + 10,
                0xFFFFFFFF);
    }

    private void updateButtons(ModUpdaterService.Snapshot snapshot) {
        if (this.downloadSelected != null) {
            this.downloadSelected.setMessage(Text.translatable(
                    "cleanmcmod.button.download_selected", selected.size()));
            this.downloadSelected.active = !selected.isEmpty() && !snapshot.busy();
        }
        if (this.downloadAll != null) {
            this.downloadAll.active = !shown.isEmpty() && !snapshot.busy();
        }
    }

    private void renderList(DrawContext context, ModUpdaterService.Snapshot snapshot, int mouseX, int mouseY) {
        int left = listLeft();
        int width = listWidth();
        int right = left + width;
        int listBottom = this.height - BOTTOM_RESERVED;
        int rows = Math.max(1, (listBottom - LIST_TOP) / ROW_HEIGHT);
        int first = (int) (scroll / ROW_HEIGHT);
        int y = LIST_TOP - (int) (scroll % ROW_HEIGHT);

        int checkX = right - CHECK_WIDTH;
        int newX = checkX - VERSION_WIDTH;
        int installedX = newX - VERSION_WIDTH;

        context.drawText(this.textRenderer, Text.translatable("cleanmcmod.screen.updates.installed"),
                installedX, LIST_TOP - 12, Texts.COLOR_INFO, false);
        context.drawText(this.textRenderer, Text.translatable("cleanmcmod.screen.updates.new"),
                newX, LIST_TOP - 12, Texts.COLOR_INFO, false);
        context.fill(left, LIST_TOP - 2, right, LIST_TOP - 1, 0x33FFFFFF);

        for (int index = first; index < shown.size() && index < first + rows; index++) {
            ModUpdate update = shown.get(index);
            boolean isSelected = selected.contains(update.local().path());
            boolean blocked = update.blocked();
            boolean resolvable = update.resolvable();

            if (isSelected) {
                context.fill(left, y - 3, right, y + ROW_HEIGHT - 4, 0x332E7D32);
            }
            if (mouseX >= left && mouseX <= right && mouseY >= y - 3 && mouseY < y + ROW_HEIGHT - 3) {
                context.fill(left, y - 3, right, y + ROW_HEIGHT - 4, 0x22FFFFFF);
                List<Text> tooltip = new ArrayList<>(2);
                tooltip.add(Texts.source(update.candidate().source()));
                if (update.reason() != null) {
                    tooltip.add(Text.literal(this.textRenderer.trimToWidth(update.reason(), 220)));
                }
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }

            // Icon: a tile with the mod's initial, so the row is recognisable before the name reads.
            context.fill(left, y + 1, left + ICON, y + 1 + ICON, 0xFF2B2B2B);
            String initial = update.displayName().isEmpty() ? "?"
                    : update.displayName().substring(0, 1).toUpperCase(Locale.ROOT);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(initial),
                    left + ICON / 2, y + 5, Texts.COLOR_INFO);

            int nameX = left + ICON + 6;
            int nameRoom = Math.max(40, installedX - nameX - 8);
            String tag = "[" + update.candidate().source().tag() + "]";
            int tagWidth = this.textRenderer.getWidth(tag + " ");
            context.drawText(this.textRenderer, Text.literal(tag), nameX, y + 4,
                    Texts.sourceColor(update.candidate().source()), false);
            String name = this.textRenderer.trimToWidth(update.displayName(), Math.max(24, nameRoom - tagWidth));
            int nameColor = blocked ? (resolvable ? Texts.COLOR_WARN : Texts.COLOR_ERROR) : 0xFFFFFFFF;
            context.drawText(this.textRenderer, Text.literal(name), nameX + tagWidth, y + 4, nameColor, false);

            context.drawText(this.textRenderer,
                    Text.literal(this.textRenderer.trimToWidth(update.installedVersion(), VERSION_WIDTH - 6)),
                    installedX, y + 4, Texts.COLOR_INFO, false);
            context.drawText(this.textRenderer,
                    Text.literal(this.textRenderer.trimToWidth(update.availableVersion(), VERSION_WIDTH - 6)),
                    newX, y + 4, blocked ? Texts.COLOR_WARN : Texts.COLOR_OK, false);

            int box = checkX + 4;
            context.fill(box, y + 4, box + 12, y + 16, isSelected ? 0xFF2E7D32 : 0xFF3A3A3A);
            if (isSelected) {
                context.drawText(this.textRenderer, Text.literal("✔"), box + 2, y + 5, 0xFFFFFFFF, false);
            }
            y += ROW_HEIGHT;
        }

        double maxScroll = Math.max(0, shown.size() * ROW_HEIGHT - (listBottom - LIST_TOP));
        if (maxScroll > 0) {
            int trackTop = LIST_TOP;
            int trackHeight = listBottom - LIST_TOP;
            int thumbHeight = Math.max(16, (int) (trackHeight * (trackHeight / (double) (shown.size() * ROW_HEIGHT))));
            int thumbTop = trackTop + (int) ((trackHeight - thumbHeight) * (scroll / maxScroll));
            context.fill(right + 4, trackTop, right + 6, trackTop + trackHeight, 0x22FFFFFF);
            context.fill(right + 4, thumbTop, right + 6, thumbTop + thumbHeight, 0x88FFFFFF);
        }

        if (snapshot.outcomes().isEmpty() || shown.size() <= rows) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.updates.hint", selected.size()),
                    this.width / 2, listBottom + 4, Texts.COLOR_INFO);
        }
    }

    private int listWidth() {
        return Math.min(this.width - 24, 480);
    }

    private int listLeft() {
        return (this.width - listWidth()) / 2;
    }

    private Text trimmed(Text text) {
        return Text.literal(this.textRenderer.trimToWidth(text.getString(), this.width - 40));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
            double verticalAmount) {
        int listHeight = this.height - BOTTOM_RESERVED - LIST_TOP;
        double maxScroll = Math.max(0, shown.size() * ROW_HEIGHT - listHeight);
        if (maxScroll > 0) {
            this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - verticalAmount * ROW_HEIGHT));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /** Handles a left click on the table: any click in a row ticks or clears its box. */
    protected boolean clickRow(double mouseX, double mouseY, int button) {
        if (button != 0 || shown.isEmpty()) {
            return false;
        }
        int left = listLeft();
        int right = left + listWidth();
        int index = rowAt(mouseX, mouseY);
        if (index < 0 || mouseX < left || mouseX > right) {
            return false;
        }
        Path key = shown.get(index).local().path();
        if (!selected.remove(key)) {
            selected.add(key);
        }
        return true;
    }

    private int rowAt(double mouseX, double mouseY) {
        if (mouseY < LIST_TOP || mouseY >= this.height - BOTTOM_RESERVED) {
            return -1;
        }
        int index = (int) ((mouseY - LIST_TOP + scroll) / ROW_HEIGHT);
        return index >= 0 && index < shown.size() ? index : -1;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
