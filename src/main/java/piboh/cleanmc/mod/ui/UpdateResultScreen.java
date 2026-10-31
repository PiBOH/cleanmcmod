package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import piboh.cleanmc.mod.update.ModInstaller;

import java.util.List;

/**
 * The alert shown when a download finishes: one line per mod, saying whether it was updated and to
 * which version, instead of leaving the player to read the log.
 */
public class UpdateResultScreen extends Screen {

    private static final int LINE_HEIGHT = 12;

    private final Screen parent;
    private final List<ModInstaller.Outcome> outcomes;

    public UpdateResultScreen(Screen parent, List<ModInstaller.Outcome> outcomes) {
        super(Lang.text("cleanmcmod.screen.updates.result.title"));
        this.parent = parent;
        this.outcomes = List.copyOf(outcomes);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.addRenderableWidget(Button.builder(Lang.text("gui.done"),
                        button -> this.onClose())
                .bounds(centerX - 75, this.height - 32, 150, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        graphics.centeredText(this.font, this.title, centerX, 24, 0xFFFFFFFF);

        long ok = outcomes.stream().filter(ModInstaller.Outcome::success).count();
        graphics.centeredText(this.font,
                Lang.text("cleanmcmod.screen.updates.result.summary", ok, outcomes.size()),
                centerX, 42, ok == outcomes.size() ? Texts.COLOR_OK : Texts.COLOR_WARN);

        int y = 62;
        int room = (this.height - 70 - y) / LINE_HEIGHT;
        for (int index = 0; index < outcomes.size(); index++) {
            if (index >= room) {
                graphics.centeredText(this.font,
                        Lang.text("cleanmcmod.screen.updates.result.more", outcomes.size() - index),
                        centerX, y, Texts.COLOR_INFO);
                break;
            }
            ModInstaller.Outcome outcome = outcomes.get(index);
            Component line;
            int color;
            if (!outcome.success()) {
                line = Lang.text("cleanmcmod.screen.updates.result.failed",
                        outcome.update().displayName(), outcome.detail());
                color = Texts.COLOR_ERROR;
            } else if (outcome.detail() != null) {
                line = Lang.text("cleanmcmod.screen.updates.result.old_kept",
                        outcome.update().displayName(), outcome.detail());
                color = Texts.COLOR_WARN;
            } else {
                line = Lang.text("cleanmcmod.screen.updates.result.ok",
                        outcome.update().displayName(), outcome.update().availableVersion());
                color = Texts.COLOR_OK;
            }
            graphics.centeredText(this.font,
                    Component.literal(this.font.plainSubstrByWidth(line.getString(), this.width - 40)),
                    centerX, y, color);
            y += LINE_HEIGHT;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    /** Skips the game's full-screen blur: see {@link ScreenBackdrop}. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level == null) {
            this.extractMenuBackground(graphics);
        }
        this.extractTransparentBackground(graphics);
    }

}
