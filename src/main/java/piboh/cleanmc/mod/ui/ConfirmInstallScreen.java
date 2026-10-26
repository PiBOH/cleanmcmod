package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import piboh.cleanmc.mod.update.ModUpdate;
import piboh.cleanmc.mod.update.ModUpdaterService;

import java.util.List;

/** The step between "install" and any network traffic: says exactly what will be downloaded. */
public class ConfirmInstallScreen extends Screen {

    private final Screen parent;
    private final ModUpdaterService service;
    private final List<ModUpdate> updates;

    public ConfirmInstallScreen(Screen parent, ModUpdaterService service, List<ModUpdate> updates) {
        super(Text.translatable("cleanmcmod.screen.confirm.title"));
        this.parent = parent;
        this.service = service;
        this.updates = List.copyOf(updates);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cleanmcmod.button.install"), button -> {
                    service.installAsync(updates);
                    this.client.setScreen(this.parent);
                })
                .dimensions(centerX - 154, this.height - 50, 150, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> this.close())
                .dimensions(centerX + 4, this.height - 50, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 28, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable(updates.size() == 1
                                ? "cleanmcmod.screen.confirm.body.one"
                                : "cleanmcmod.screen.confirm.body",
                        updates.size()), centerX, 50, Texts.COLOR_INFO);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("cleanmcmod.screen.confirm.warning"), centerX, 66, Texts.COLOR_WARN);

        int y = 90;
        for (ModUpdate update : updates) {
            if (y > this.height - 70) {
                break;
            }
            Text line = Text.literal(update.displayName() + "  ")
                    .append(Text.translatable("cleanmcmod.screen.updates.versions",
                            update.installedVersion(), update.availableVersion()));
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(this.textRenderer.trimToWidth(line.getString(), this.width - 40)), centerX, y, 0xFFFFFFFF);
            y += 14;
        }
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
