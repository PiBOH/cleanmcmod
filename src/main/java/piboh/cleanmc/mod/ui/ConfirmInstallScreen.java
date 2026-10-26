package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import piboh.cleanmc.mod.update.ModUpdate;
import piboh.cleanmc.mod.update.ModUpdaterService;

import java.util.List;

/** The step between "install" and any network traffic: says exactly what will be downloaded. */
public class ConfirmInstallScreen extends Screen {

    private final Screen parent;
    private final ModUpdaterService service;
    private final List<ModUpdate> updates;

    public ConfirmInstallScreen(Screen parent, ModUpdaterService service, List<ModUpdate> updates) {
        super(Component.translatable("cleanmcmod.screen.confirm.title"));
        this.parent = parent;
        this.service = service;
        this.updates = List.copyOf(updates);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("cleanmcmod.button.install"), button -> {
                    service.installAsync(updates);
                    this.minecraft.setScreenAndShow(this.parent);
                })
                .bounds(centerX - 154, this.height - 50, 150, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(centerX + 4, this.height - 50, 150, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        graphics.centeredText(this.font, this.title, centerX, 28, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.translatable(updates.size() == 1
                                ? "cleanmcmod.screen.confirm.body.one"
                                : "cleanmcmod.screen.confirm.body",
                        updates.size()), centerX, 50, Texts.COLOR_INFO);
        graphics.centeredText(this.font,
                Component.translatable("cleanmcmod.screen.confirm.warning"), centerX, 66, Texts.COLOR_WARN);

        int y = 90;
        for (ModUpdate update : updates) {
            if (y > this.height - 70) {
                break;
            }
            Component line = Component.literal(update.displayName() + "  ")
                    .append(Component.translatable("cleanmcmod.screen.updates.versions",
                            update.installedVersion(), update.availableVersion()));
            graphics.centeredText(this.font,
                    Component.literal(this.font.plainSubstrByWidth(line.getString(), this.width - 40)), centerX, y, 0xFFFFFFFF);
            y += 14;
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
