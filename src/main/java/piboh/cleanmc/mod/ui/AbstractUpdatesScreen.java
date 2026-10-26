package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import piboh.cleanmc.mod.update.ModInstaller;
import piboh.cleanmc.mod.update.ModUpdate;
import piboh.cleanmc.mod.update.ModUpdaterService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lists the installed mods Modrinth has a newer version for; nothing is installed without a confirmation. */
/**
 * Everything the Mod Updates screen draws and decides. The concrete
 * {@code ModUpdatesScreen} is provided per Minecraft input API, because the
 * click signature changed in 1.21.9.
 */
public abstract class AbstractUpdatesScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 56;
    private static final int LIST_SIDE = 160;
    private static final int BOTTOM_RESERVED = 96;

    private final Screen parent;
    private final ModUpdaterService service;
    private final Set<String> selected = new HashSet<>();

    private double scroll;
    private List<ModUpdate> shown = List.of();

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
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cleanmcmod.button.install_selected"),
                        button -> installSelected())
                .dimensions(centerX - 154, this.height - 50, 150, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cleanmcmod.button.install_all"),
                        button -> installAll())
                .dimensions(centerX + 4, this.height - 50, 150, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cleanmcmod.button.refresh"),
                        button -> {
                            selected.clear();
                            scroll = 0;
                            service.checkAsync();
                        })
                .dimensions(centerX - 154, this.height - 26, 150, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(centerX + 4, this.height - 26, 150, 20)
                .build());
    }

    private void installSelected() {
        List<ModUpdate> targets = new ArrayList<>();
        for (ModUpdate update : shown) {
            if (selected.contains(update.candidate().projectId())) {
                targets.add(update);
            }
        }
        if (!targets.isEmpty()) {
            this.client.setScreen(new ConfirmInstallScreen(this, service, targets));
        }
    }

    private void installAll() {
        if (!shown.isEmpty()) {
            this.client.setScreen(new ConfirmInstallScreen(this, service, List.copyOf(shown)));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        ModUpdaterService.Snapshot snapshot = service.snapshot();
        if (!snapshot.updates().equals(shown)) {
            shown = snapshot.updates();
            selected.retainAll(shown.stream().map(update -> update.candidate().projectId()).toList());
        }

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 16, 0xFFFFFFFF);

        switch (snapshot.state()) {
            case CHECKING -> context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.updates.checking"), centerX, 34, Texts.COLOR_WARN);
            case INSTALLING -> context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.updates.installing"), centerX, 34, Texts.COLOR_WARN);
            case FAILED -> context.drawCenteredTextWithShadow(this.textRenderer,
                    trimmed(Text.translatable("cleanmcmod.screen.updates.failed",
                            snapshot.error() == null ? "" : snapshot.error())),
                    centerX, 34, Texts.COLOR_ERROR);
            case IDLE, READY -> {
                if (!snapshot.busy() && shown.isEmpty()) {
                    Text message = snapshot.scannedMods() == 0
                            ? Text.translatable("cleanmcmod.screen.updates.no_mods")
                            : Text.translatable("cleanmcmod.screen.updates.up_to_date", snapshot.scannedMods());
                    context.drawCenteredTextWithShadow(this.textRenderer, message, centerX, LIST_TOP + 10, Texts.COLOR_OK);
                } else {
                    context.drawCenteredTextWithShadow(this.textRenderer,
                            Text.translatable("cleanmcmod.screen.updates.found", shown.size()), centerX, 34, 0xFFFFFFFF);
                    renderList(context, snapshot, mouseX, mouseY);
                }
            }
        }

        renderOutcomes(context, snapshot);
    }

    private void renderList(DrawContext context, ModUpdaterService.Snapshot snapshot, int mouseX, int mouseY) {
        int listBottom = this.height - BOTTOM_RESERVED;
        int rows = Math.max(1, (listBottom - LIST_TOP) / ROW_HEIGHT);
        int first = (int) (scroll / ROW_HEIGHT);
        int y = LIST_TOP - (int) (scroll % ROW_HEIGHT);
        int centerX = this.width / 2;

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("cleanmcmod.screen.updates.hint", selected.size()),
                centerX, 44, Texts.COLOR_INFO);

        for (int index = first; index < shown.size() && index < first + rows; index++) {
            ModUpdate update = shown.get(index);
            String projectId = update.candidate().projectId();
            boolean isSelected = selected.contains(projectId);
            boolean blocked = update.blocked();
            // A block the updater can fix by fetching the dependency is a warning, not a refusal.
            boolean resolvable = update.resolvable();
            if (isSelected) {
                context.fill(centerX - LIST_SIDE, y - 3, centerX + LIST_SIDE, y + ROW_HEIGHT - 5, 0x33FFFFFF);
            }
            Text versions = Text.literal(this.textRenderer.trimToWidth(
                    Text.translatable("cleanmcmod.screen.updates.versions",
                            update.installedVersion(), update.availableVersion()).getString(),
                    LIST_SIDE - 8));
            int versionWidth = this.textRenderer.getWidth(versions);
            int nameRoom = centerX + LIST_SIDE - 4 - versionWidth - 6 - (centerX - LIST_SIDE + 4);
            String marker = blocked ? (resolvable ? "＋ " : "⚠ ") : (isSelected ? "▸ " : "  ");
            String name = this.textRenderer.trimToWidth(marker + update.displayName(),
                    Math.max(40, nameRoom));
            int nameColor = resolvable ? Texts.COLOR_WARN
                    : blocked ? Texts.COLOR_ERROR : (isSelected ? Texts.COLOR_OK : 0xFFFFFFFF);
            context.drawText(this.textRenderer, Text.literal(name), centerX - LIST_SIDE + 4, y, nameColor, false);
            context.drawText(this.textRenderer, versions, centerX + LIST_SIDE - versionWidth - 4, y, Texts.COLOR_INFO, false);
            if (mouseX >= centerX - LIST_SIDE && mouseX <= centerX + LIST_SIDE
                    && mouseY >= y && mouseY < y + ROW_HEIGHT && update.reason() != null) {
                context.drawTooltip(this.textRenderer,
                        List.of(Text.literal(this.textRenderer.trimToWidth(update.reason(), 220))),
                        mouseX, mouseY);
            }
            y += ROW_HEIGHT;
        }

        // The outcome lines own the band right below the list; the hint would collide with them.
        if (shown.size() > rows && snapshot.outcomes().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.updates.scroll"), centerX, listBottom + 4, Texts.COLOR_INFO);
        }
    }

    private void renderOutcomes(DrawContext context, ModUpdaterService.Snapshot snapshot) {
        List<ModInstaller.Outcome> outcomes = snapshot.outcomes();
        int y = this.height - 92;
        for (int index = 0; index < Math.min(3, outcomes.size()); index++, y += 12) {
            ModInstaller.Outcome outcome = outcomes.get(index);
            Text line;
            int color;
            if (!outcome.success()) {
                line = Text.translatable("cleanmcmod.screen.updates.result.failed",
                        outcome.update().displayName(), outcome.detail());
                color = Texts.COLOR_ERROR;
            } else if (outcome.detail() != null) {
                line = Text.translatable("cleanmcmod.screen.updates.result.old_kept",
                        outcome.update().displayName(), outcome.detail());
                color = Texts.COLOR_WARN;
            } else {
                line = Text.translatable("cleanmcmod.screen.updates.result.ok",
                        outcome.update().displayName(), outcome.update().availableVersion());
                color = Texts.COLOR_OK;
            }
            context.drawText(this.textRenderer, trimmed(line), this.width / 2 - LIST_SIDE, y, color, false);
        }
        if (outcomes.size() > 3) {
            context.drawText(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.updates.result.more", outcomes.size() - 3),
                    this.width / 2 - LIST_SIDE, y, Texts.COLOR_INFO, false);
        }
    }

    private Text trimmed(Text text) {
        return Text.literal(this.textRenderer.trimToWidth(text.getString(), this.width - 40));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!shown.isEmpty()) {
            this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll - verticalAmount * ROW_HEIGHT));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private double maxScroll() {
        int listBottom = this.height - BOTTOM_RESERVED;
        return Math.max(0, shown.size() * ROW_HEIGHT - (listBottom - LIST_TOP));
    }

    /** Handles a left click on the row list. Returns true when the click was consumed. */
    protected boolean clickRow(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int listBottom = this.height - BOTTOM_RESERVED;
        if (button == 0 && mouseX >= centerX - LIST_SIDE && mouseX <= centerX + LIST_SIDE
                && mouseY >= LIST_TOP && mouseY < listBottom && !shown.isEmpty()) {
            int index = (int) ((mouseY - LIST_TOP + scroll) / ROW_HEIGHT);
            if (index >= 0 && index < shown.size()) {
                String projectId = shown.get(index).candidate().projectId();
                if (!selected.remove(projectId)) {
                    selected.add(projectId);
                }
                return true;
            }
        }
        return false;
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
