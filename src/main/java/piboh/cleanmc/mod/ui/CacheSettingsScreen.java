package piboh.cleanmc.mod.ui;

import piboh.cleanmc.mod.clean.CleanMcModService;
import piboh.cleanmc.mod.config.CleanMcModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Toggles for what a clean removes. Every change is written to disk immediately. */
public class CacheSettingsScreen extends Screen {

    private static final int COLUMNS = 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 6;

    private final Screen parent;
    private final CleanMcModService service;

    public CacheSettingsScreen(Screen parent, CleanMcModService service) {
        super(Text.translatable("cleanmcmod.screen.settings.title"));
        this.parent = parent;
        this.service = service;
    }

    @Override
    protected void init() {
        List<Option> options = options();
        int buttonWidth = Math.min(150, (this.width - 24) / COLUMNS);
        int totalWidth = buttonWidth * COLUMNS + 6;
        int startX = (this.width - totalWidth) / 2;
        int startY = 64;

        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int column = i % COLUMNS;
            int row = i / COLUMNS;
            int x = startX + column * (buttonWidth + 6);
            int y = startY + row * (BUTTON_HEIGHT + ROW_GAP);

            ButtonWidget button = ButtonWidget.builder(label(option), pressed -> {
                        option.setter().accept(!option.getter().get());
                        service.saveConfig();
                        pressed.setMessage(label(option));
                    })
                    .dimensions(x, y, buttonWidth, BUTTON_HEIGHT)
                    .build();
            this.addDrawableChild(button);
        }

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(this.width / 2 - 75, this.height - 30, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("cleanmcmod.screen.settings.intro"), this.width / 2, 38, Texts.COLOR_INFO);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("cleanmcmod.screen.settings.note"), this.width / 2, this.height - 46, Texts.COLOR_WARN);
    }

    private Text label(Option option) {
        return Text.translatable(option.getter().get() ? "cleanmcmod.setting.on" : "cleanmcmod.setting.off",
                Text.translatable(option.labelKey()));
    }

    private List<Option> options() {
        CleanMcModConfig config = service.config();
        return List.of(
                new Option("cleanmcmod.setting.logs", () -> config.includeLogs, value -> config.includeLogs = value),
                new Option("cleanmcmod.setting.crash_reports", () -> config.includeCrashReports,
                        value -> config.includeCrashReports = value),
                new Option("cleanmcmod.setting.debug_dumps", () -> config.includeDebugDumps,
                        value -> config.includeDebugDumps = value),
                new Option("cleanmcmod.setting.server_resource_packs", () -> config.includeServerResourcePacks,
                        value -> config.includeServerResourcePacks = value),
                new Option("cleanmcmod.setting.loader_caches", () -> config.includeLoaderCaches,
                        value -> config.includeLoaderCaches = value),
                new Option("cleanmcmod.setting.asset_cache", () -> config.includeAssetCache,
                        value -> config.includeAssetCache = value),
                new Option("cleanmcmod.setting.auto_detect", () -> config.autoDetect,
                        value -> config.autoDetect = value),
                new Option("cleanmcmod.setting.dry_run", () -> config.dryRun,
                        value -> config.dryRun = value));
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

    private record Option(String labelKey, Supplier<Boolean> getter, Consumer<Boolean> setter) {
    }
}
