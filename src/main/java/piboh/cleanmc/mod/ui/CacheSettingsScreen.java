package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import piboh.cleanmc.mod.clean.CacheScan;
import piboh.cleanmc.mod.clean.CacheTarget;
import piboh.cleanmc.mod.clean.CleanMcModService;
import piboh.cleanmc.mod.clean.TargetState;
import piboh.cleanmc.mod.config.CleanMcModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings, laid out like Reese's Sodium Options: a column of category tabs on the left, a search
 * field above the list, one row per option with its toggle on the right, and a panel at the bottom
 * that explains the option under the cursor together with how much it would free.
 *
 * <p>Still only widgets: it draws, it never decides. Every change is written to disk immediately
 * through {@link CleanMcModService#saveConfig()}, and a row whose value differs from the default is
 * marked, so a reset is always one click away.</p>
 */
public class CacheSettingsScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int TAB_WIDTH = 92;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_GAP = 4;
    private static final int ROW_HEIGHT = 22;
    private static final int TOGGLE_WIDTH = 58;
    private static final int LIST_TOP = 50;
    private static final int FOOTER = 54;

    /** One row of the list. {@code tab} is the tab it belongs to, the label key its name. */
    private record Option(String id, String labelKey, String tab, String targetPrefix,
            boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
    }

    private record Tab(String id, String labelKey) {
    }

    private static final class Row {
        private final Option option;
        private final ButtonWidget button;

        private Row(Option option, ButtonWidget button) {
            this.option = option;
            this.button = button;
        }
    }

    private final Screen parent;
    private final CleanMcModService service;
    private final CleanMcModConfig defaults = new CleanMcModConfig();
    private final List<Row> rows = new ArrayList<>();
    private final List<ButtonWidget> tabButtons = new ArrayList<>();

    private TextFieldWidget search;
    private String activeTab = "all";
    private String filter = "";
    private double scroll;
    private int maxScroll;
    private CacheScan scan;
    private Row hovered;

    public CacheSettingsScreen(Screen parent, CleanMcModService service) {
        super(Text.translatable("cleanmcmod.screen.settings.title"));
        this.parent = parent;
        this.service = service;
    }

    @Override
    protected void init() {
        this.scan = service.scan();
        this.rows.clear();
        this.tabButtons.clear();

        int contentLeft = MARGIN + TAB_WIDTH + 8;
        int contentRight = this.width - MARGIN;

        this.search = new TextFieldWidget(this.textRenderer, contentLeft, LIST_TOP - 24,
                Math.max(80, contentRight - contentLeft), 20,
                Text.translatable("cleanmcmod.settings.search"));
        this.search.setPlaceholder(Text.translatable("cleanmcmod.settings.search"));
        this.search.setText(this.filter);
        this.search.setChangedListener(text -> {
            this.filter = text == null ? "" : text;
            layoutRows();
        });
        this.addDrawableChild(this.search);

        for (Tab tab : tabs()) {
            ButtonWidget button = ButtonWidget.builder(tabLabel(tab), pressed -> {
                        this.activeTab = tab.id();
                        this.scroll = 0;
                        refreshTabs();
                        layoutRows();
                    })
                    .dimensions(MARGIN, LIST_TOP + tabButtons.size() * (TAB_HEIGHT + TAB_GAP), TAB_WIDTH,
                            TAB_HEIGHT)
                    .build();
            this.tabButtons.add(button);
            this.addDrawableChild(button);
        }

        for (Option option : options()) {
            ButtonWidget button = ButtonWidget.builder(stateLabel(option), pressed -> {
                        option.setter().accept(!option.getter().get());
                        service.saveConfig();
                        pressed.setMessage(stateLabel(option));
                    })
                    .dimensions(contentRight - TOGGLE_WIDTH, LIST_TOP, TOGGLE_WIDTH, 18)
                    .build();
            Row row = new Row(option, button);
            this.rows.add(row);
            this.addDrawableChild(button);
        }

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cleanmcmod.settings.reset"),
                        button -> resetAll())
                .dimensions(MARGIN, this.height - 30, 92, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(this.width - MARGIN - 92, this.height - 30, 92, 20)
                .build());

        refreshTabs();
        layoutRows();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFFFF);

        int contentLeft = MARGIN + TAB_WIDTH + 8;
        int contentRight = this.width - MARGIN;
        int listBottom = this.height - FOOTER;

        this.hovered = null;
        for (Row row : rows) {
            if (!row.button.visible) {
                continue;
            }
            int y = row.button.getY() - 2;
            boolean isHovered = mouseX >= contentLeft && mouseX <= contentRight
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (isHovered) {
                this.hovered = row;
                context.fill(contentLeft - 4, y, contentRight, y + ROW_HEIGHT - 2, 0x22FFFFFF);
            }
            boolean modified = row.option.getter().get() != row.option.defaultValue();
            Text label = Text.translatable(row.option.labelKey());
            Text name = modified ? Text.literal("● ").append(label) : label;
            context.drawText(this.textRenderer, name, contentLeft, y + 5, 0xFFFFFFFF, false);

            Text measured = measure(row.option);
            if (measured != null) {
                int width = this.textRenderer.getWidth(measured);
                context.drawText(this.textRenderer, measured, contentRight - TOGGLE_WIDTH - 8 - width, y + 5,
                        Texts.COLOR_INFO, false);
            }
        }

        if (maxScroll > 0 && this.hovered == null) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("cleanmcmod.settings.scroll"),
                    (contentLeft + contentRight) / 2, listBottom + 2, Texts.COLOR_INFO);
        }
        renderExplanation(context, contentLeft, contentRight);
    }

    /** The Reese's Sodium Options detail line: what the hovered option does, or the intro text. */
    private void renderExplanation(DrawContext context, int contentLeft, int contentRight) {
        Text line;
        int color = Texts.COLOR_INFO;
        if (this.hovered == null) {
            line = Text.translatable("cleanmcmod.screen.settings.intro");
        } else {
            Option option = this.hovered.option;
            Text label = Text.translatable(option.labelKey());
            List<CacheTarget> targets = targetsFor(option);
            if (targets.isEmpty()) {
                line = Text.translatable("cleanmcmod.settings.explain", label,
                        Text.translatable("cleanmcmod.screen.settings.intro"));
            } else {
                line = Text.translatable("cleanmcmod.settings.explain", label,
                        Text.translatable(targets.get(0).safety().translationKey()));
                color = Texts.COLOR_OK;
            }
        }
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(this.textRenderer.trimToWidth(line.getString(), this.width - 2 * MARGIN)),
                this.width / 2, this.height - 48, color);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
            double verticalAmount) {
        if (maxScroll > 0) {
            this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - verticalAmount * ROW_HEIGHT));
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /** Positions the visible rows below each other, keeping the ones scrolled out of view hidden. */
    private void layoutRows() {
        int contentRight = this.width - MARGIN;
        int top = LIST_TOP;
        int bottom = this.height - FOOTER;
        int index = 0;
        for (Row row : rows) {
            if (!matches(row.option)) {
                row.button.visible = false;
                continue;
            }
            int y = top + index * ROW_HEIGHT - (int) scroll;
            row.button.setX(contentRight - TOGGLE_WIDTH);
            row.button.setY(y + 2);
            row.button.setMessage(stateLabel(row.option));
            row.button.visible = y + ROW_HEIGHT > top && y < bottom;
            index++;
        }
        this.maxScroll = Math.max(0, index * ROW_HEIGHT - (bottom - top));
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
    }

    private boolean matches(Option option) {
        if (!"all".equals(this.activeTab) && !option.tab().equals(this.activeTab)) {
            return false;
        }
        if (this.filter.isBlank()) {
            return true;
        }
        return Text.translatable(option.labelKey()).getString().toLowerCase(Locale.ROOT)
                .contains(this.filter.trim().toLowerCase(Locale.ROOT));
    }

    private Text stateLabel(Option option) {
        return option.getter().get()
                ? Text.translatable("cleanmcmod.settings.on")
                : Text.translatable("cleanmcmod.settings.off");
    }

    /** Files and bytes the option would free, from the scan taken when the screen opened. */
    private Text measure(Option option) {
        List<CacheTarget> targets = targetsFor(option);
        if (targets.isEmpty() || this.scan == null) {
            return null;
        }
        long bytes = 0L;
        int files = 0;
        for (TargetState state : this.scan.targets()) {
            if (!targets.contains(state.target())) {
                continue;
            }
            files += state.files();
            bytes += state.bytes();
        }
        if (files == 0) {
            return Text.translatable("cleanmcmod.status.missing");
        }
        return Text.literal(files + " · " + piboh.cleanmc.mod.clean.CleanReport.formatBytes(bytes));
    }

    private List<CacheTarget> targetsFor(Option option) {
        if (option.targetPrefix() == null) {
            return List.of();
        }
        List<CacheTarget> found = new ArrayList<>();
        for (CacheTarget target : service.targets()) {
            if (target.id().equals(option.targetPrefix())
                    || target.id().startsWith(option.targetPrefix())) {
                found.add(target);
            }
        }
        return found;
    }

    private void resetAll() {
        for (Option option : options()) {
            option.setter().accept(option.defaultValue());
        }
        service.saveConfig();
        this.scan = service.scan();
        layoutRows();
        piboh.cleanmc.mod.CleanMcMod.LOGGER.info("Cache settings reset to defaults");
    }

    private void refreshTabs() {
        for (int index = 0; index < tabButtons.size(); index++) {
            Tab tab = tabs().get(index);
            tabButtons.get(index).setMessage(tabLabel(tab));
        }
    }

    private Text tabLabel(Tab tab) {
        Text label = Text.translatable(tab.labelKey());
        return tab.id().equals(this.activeTab) ? Text.literal("▸ ").append(label) : label;
    }

    private static List<Tab> tabs() {
        return List.of(
                new Tab("all", "cleanmcmod.settings.tab.all"),
                new Tab("vanilla", "cleanmcmod.category.vanilla"),
                new Tab("loader", "cleanmcmod.category.loader"),
                new Tab("map", "cleanmcmod.category.map"),
                new Tab("mod", "cleanmcmod.category.mod"),
                new Tab("advanced", "cleanmcmod.settings.tab.advanced"));
    }

    private List<Option> options() {
        CleanMcModConfig config = service.config();
        CleanMcModConfig defaults = this.defaults;
        return List.of(
                new Option("logs", "cleanmcmod.setting.logs", "vanilla", "vanilla_logs",
                        defaults.includeLogs, () -> config.includeLogs, value -> config.includeLogs = value),
                new Option("crash_reports", "cleanmcmod.setting.crash_reports", "vanilla", "crash_reports",
                        defaults.includeCrashReports, () -> config.includeCrashReports,
                        value -> config.includeCrashReports = value),
                new Option("debug_dumps", "cleanmcmod.setting.debug_dumps", "vanilla", "debug_dumps",
                        defaults.includeDebugDumps, () -> config.includeDebugDumps,
                        value -> config.includeDebugDumps = value),
                new Option("asset_cache", "cleanmcmod.setting.asset_cache", "advanced", "asset_",
                        defaults.includeAssetCache, () -> config.includeAssetCache,
                        value -> config.includeAssetCache = value),
                new Option("server_resource_packs", "cleanmcmod.setting.server_resource_packs", "mod",
                        "server_resource_packs", defaults.includeServerResourcePacks,
                        () -> config.includeServerResourcePacks,
                        value -> config.includeServerResourcePacks = value),
                new Option("loader_caches", "cleanmcmod.setting.loader_caches", "loader", "fabric_remapped",
                        defaults.includeLoaderCaches, () -> config.includeLoaderCaches,
                        value -> config.includeLoaderCaches = value),
                new Option("map_caches", "cleanmcmod.setting.map_caches", "map", "map_cache",
                        defaults.includeMapCaches, () -> config.includeMapCaches,
                        value -> config.includeMapCaches = value),
                new Option("auto_detect", "cleanmcmod.setting.auto_detect", "mod", "detected_",
                        defaults.autoDetect, () -> config.autoDetect, value -> config.autoDetect = value),
                new Option("dry_run", "cleanmcmod.setting.dry_run", "advanced", null,
                        defaults.dryRun, () -> config.dryRun, value -> config.dryRun = value));
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
