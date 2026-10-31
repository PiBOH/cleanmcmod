package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
        this.addDrawableChild(ButtonWidget.builder(Lang.text("gui.done"), button -> this.close())
                .dimensions(centerX - 75, this.height - 32, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 24, 0xFFFFFFFF);

        long ok = outcomes.stream().filter(ModInstaller.Outcome::success).count();
        context.drawCenteredTextWithShadow(this.textRenderer,
                Lang.text("cleanmcmod.screen.updates.result.summary", ok, outcomes.size()),
                centerX, 42, ok == outcomes.size() ? Texts.COLOR_OK : Texts.COLOR_WARN);

        int y = 62;
        int room = (this.height - 70 - y) / LINE_HEIGHT;
        for (int index = 0; index < outcomes.size(); index++) {
            if (index >= room) {
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Lang.text("cleanmcmod.screen.updates.result.more", outcomes.size() - index),
                        centerX, y, Texts.COLOR_INFO);
                break;
            }
            ModInstaller.Outcome outcome = outcomes.get(index);
            Text line;
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
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(this.textRenderer.trimToWidth(line.getString(), this.width - 40)),
                    centerX, y, color);
            y += LINE_HEIGHT;
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    /**
     * Darkens the world instead of blurring it: see {@link ScreenBackdrop}.
     */
    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY,
            float delta) {
        if (this.client != null && this.client.world == null) {
            this.renderPanoramaBackground(context, delta);
        }
        ScreenBackdrop.dim(context, this.width, this.height);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
