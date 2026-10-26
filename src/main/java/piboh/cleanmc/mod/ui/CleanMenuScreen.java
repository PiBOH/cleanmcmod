package piboh.cleanmc.mod.ui;

import piboh.cleanmc.mod.clean.CacheScan;
import piboh.cleanmc.mod.clean.CleanMcModService;
import piboh.cleanmc.mod.clean.CleanReport;
import piboh.cleanmc.mod.clean.TargetResult;
import piboh.cleanmc.mod.clean.TargetState;
import piboh.cleanmc.mod.update.ModUpdaterService;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** The main screen: what is currently cached, one button to clean it, one button to configure it. */
public class CleanMenuScreen extends Screen {

    private static final int BUTTON_WIDTH = 150;

    private final CleanMcModService service;
    private final ModUpdaterService updater;
    private CacheScan scan;
    private CleanReport shownReport;

    public CleanMenuScreen(CleanMcModService service, ModUpdaterService updater) {
        super(Text.translatable("cleanmcmod.screen.main.title"));
        this.service = service;
        this.updater = updater;
    }

    @Override
    protected void init() {
        this.scan = service.scan();
        this.shownReport = service.lastReport();

        int centerX = this.width / 2;
        int buttonY = this.height - 76;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cleanmcmod.button.clean"), button -> service.cleanAsync())
                .dimensions(centerX - BUTTON_WIDTH - 2, buttonY, BUTTON_WIDTH, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cleanmcmod.button.rescan"), button -> this.scan = service.scan())
                .dimensions(centerX + 2, buttonY, BUTTON_WIDTH, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cleanmcmod.button.update_mods"),
                        button -> this.client.setScreen(new ModUpdatesScreen(this, updater)))
                .dimensions(centerX - BUTTON_WIDTH - 2, buttonY + 24, BUTTON_WIDTH, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cleanmcmod.button.settings"),
                        button -> this.client.setScreen(new CacheSettingsScreen(this, service)))
                .dimensions(centerX + 2, buttonY + 24, BUTTON_WIDTH, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(centerX - BUTTON_WIDTH / 2, buttonY + 48, BUTTON_WIDTH, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        CleanReport report = service.lastReport();
        if (report != shownReport) {
            shownReport = report;
            this.scan = service.lastScan() != null ? service.lastScan() : service.scan();
        }

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 14, 0xFFFFFFFF);

        if (service.config().dryRun) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.main.dry_run"), centerX, 30, Texts.COLOR_WARN);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.main.subtitle"), centerX, 30, Texts.COLOR_INFO);
        }

        CacheScan current = this.scan;
        int listTop = 52;
        int listBottom = this.height - 92;
        if (current == null || current.empty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.main.nothing_cached"), centerX, listTop + 6, Texts.COLOR_OK);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.main.summary",
                            current.presentCount(), Texts.bytes(current.bytes())),
                    centerX, 40, 0xFFFFFFFF);
            renderTargets(context, current, listTop, listBottom);
        }

        if (service.isRunning()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.main.running"), centerX, listBottom + 4, Texts.COLOR_WARN);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.screen.main.hint"), centerX, listBottom + 4, Texts.COLOR_INFO);
        }
    }

    private void renderTargets(DrawContext context, CacheScan current, int listTop, int listBottom) {
        List<TargetState> present = current.presentTargets();
        int maxRows = Math.max(1, (listBottom - listTop) / 11);
        int left = this.width / 2 - 156;
        int right = this.width / 2 + 156;
        int y = listTop;

        for (int i = 0; i < Math.min(maxRows, present.size()); i++) {
            TargetState state = present.get(i);
            TargetResult result = findResult(state);
            int color = result == null ? 0xFFFFFFFF : Texts.statusColor(result);

            Text name = Texts.targetName(state.target());
            String measured = "%d · %s".formatted(state.files(), CleanReport.formatBytes(state.bytes()));
            int measuredWidth = this.textRenderer.getWidth(measured);

            context.drawText(this.textRenderer, name, left, y, color, false);
            context.drawText(this.textRenderer, Text.literal(measured), right - measuredWidth, y, Texts.COLOR_INFO, false);

            if (result != null) {
                Text statusLine = Texts.status(result);
                int statusWidth = this.textRenderer.getWidth(statusLine);
                context.drawText(this.textRenderer, statusLine, right - measuredWidth - statusWidth - 8, y,
                        Texts.statusColor(result), false);
            }
            y += 11;
        }

        int hidden = present.size() - maxRows;
        if (hidden > 0) {
            context.drawText(this.textRenderer, Text.translatable("cleanmcmod.screen.main.more", hidden),
                    left, y, Texts.COLOR_INFO, false);
        }
    }

    private TargetResult findResult(TargetState state) {
        if (shownReport == null) {
            return null;
        }
        for (TargetResult result : shownReport.results()) {
            if (result.targetId().equals(state.target().id())) {
                return result;
            }
        }
        return null;
    }

    /** Closing at the title screen would leave a black screen; go back to the title instead. */
    @Override
    public void close() {
        this.client.setScreen(this.client.world != null ? null : new TitleScreen());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
