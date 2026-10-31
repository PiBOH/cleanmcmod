package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import piboh.cleanmc.mod.CleanMcMod;
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
 * Settings, laid out like Reese's Sodium Options: a column of category tabs on the left, one row per
 * option with its toggle on the right, and a panel at the bottom that explains the option under the
 * cursor together with how much it would free.
 *
 * <p>Rows are drawn and hit-tested by this screen instead of being widgets: the 26.x widget API
 * offers no text field this code can rely on, so the list stays one clickable surface and the only
 * widgets are the three buttons. A row whose value differs from the default is marked, and a reset is
 * always one click away.</p>
 */
public class CacheSettingsScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int TAB_WIDTH = 92;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_GAP = 4;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 50;
    private static final int FOOTER = 54;

    /** One row of the list. {@code tab} is the tab it belongs to, the label key its name. */
    private record Option(String id, String labelKey, String tab, String targetPrefix,
            boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
    }

    private record Tab(String id, String labelKey) {
    }

    private final Screen parent;
    private final CleanMcModService service;
    private final CleanMcModConfig defaults = new CleanMcModConfig();

    private String activeTab = "all";
    private double scroll;
    private int maxScroll;
    private CacheScan scan;
    private Option hovered;

    public CacheSettingsScreen(Screen parent, CleanMcModService service) {
        super(Component.translatable("cleanmcmod.screen.settings.title"));
        this.parent = parent;
        this.service = service;
    }

    @Override
    protected void init() {
        this.scan = service.scan();
        this.addRenderableWidget(Button.builder(Component.translatable("cleanmcmod.settings.reset"),
                        button -> resetAll())
                .bounds(MARGIN, this.height - 30, 92, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width - MARGIN - 92, this.height - 30, 92, 20)
                .build());
        updateScroll();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);

        renderTabs(graphics);
        renderRows(graphics, mouseX, mouseY);

        Component line;
        int color = Texts.COLOR_INFO;
        if (this.hovered == null) {
            line = Component.translatable("cleanmcmod.screen.settings.intro");
        } else {
            Option option = this.hovered;
            List<CacheTarget> targets = targetsFor(option);
            if (targets.isEmpty()) {
                line = Component.translatable("cleanmcmod.settings.explain",
                        Component.translatable(option.labelKey()),
                        Component.translatable("cleanmcmod.screen.settings.intro"));
            } else {
                line = Component.translatable("cleanmcmod.settings.explain",
                        Component.translatable(option.labelKey()),
                        Component.translatable(targets.get(0).safety().translationKey()));
                color = Texts.COLOR_OK;
            }
        }
        graphics.centeredText(this.font,
                Component.literal(this.font.plainSubstrByWidth(line.getString(), this.width - 2 * MARGIN)),
                this.width / 2, this.height - 48, color);
    }

    private void renderTabs(GuiGraphicsExtractor graphics) {
        List<Tab> tabs = tabs();
        for (int index = 0; index < tabs.size(); index++) {
            Tab tab = tabs.get(index);
            int y = LIST_TOP + index * (TAB_HEIGHT + TAB_GAP);
            Component label = Component.translatable(tab.labelKey());
            String prefix = tab.id().equals(this.activeTab) ? "▸ " : "  ";
            graphics.text(this.font, Component.literal(prefix + label.getString()), MARGIN, y + 6,
                    tab.id().equals(this.activeTab) ? Texts.COLOR_OK : 0xFFFFFFFF, false);
        }
    }

    private void renderRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int contentLeft = MARGIN + TAB_WIDTH + 8;
        int contentRight = this.width - MARGIN;
        int listBottom = this.height - FOOTER;
        int maxRows = Math.max(1, (listBottom - LIST_TOP) / ROW_HEIGHT);
        List<Option> visible = visibleRows();
        int first = (int) (scroll / ROW_HEIGHT);

        this.hovered = null;
        for (int index = first; index < visible.size() && index < first + maxRows; index++) {
            Option option = visible.get(index);
            int y = LIST_TOP + (index - first) * ROW_HEIGHT;
            boolean isHovered = mouseX >= contentLeft && mouseX <= contentRight
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (isHovered) {
                this.hovered = option;
                graphics.fill(contentLeft - 4, y, contentRight, y + ROW_HEIGHT - 2, 0x22FFFFFF);
            }
            boolean modified = option.getter().get() != option.defaultValue();
            Component label = Component.translatable(option.labelKey());
            Component name = modified ? Component.literal("● ").append(label) : label;
            graphics.text(this.font, name, contentLeft, y + 5, 0xFFFFFFFF, false);

            Component state = Component.translatable(option.getter().get()
                    ? "cleanmcmod.settings.on"
                    : "cleanmcmod.settings.off");
            int stateWidth = this.font.width(state);
            graphics.text(this.font, state, contentRight - stateWidth, y + 5,
                    option.getter().get() ? Texts.COLOR_OK : Texts.COLOR_INFO, false);

            Component measured = measure(option);
            if (measured != null) {
                int width = this.font.width(measured);
                graphics.text(this.font, measured, contentRight - stateWidth - 8 - width, y + 5,
                        Texts.COLOR_INFO, false);
            }
        }

        if (maxScroll > 0 && this.hovered == null) {
            graphics.centeredText(this.font, Component.translatable("cleanmcmod.settings.scroll"),
                    (contentLeft + contentRight) / 2, listBottom + 2, Texts.COLOR_INFO);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        }
        List<Tab> tabs = tabs();
        for (int index = 0; index < tabs.size(); index++) {
            int y = LIST_TOP + index * (TAB_HEIGHT + TAB_GAP);
            if (event.x() >= MARGIN && event.x() <= MARGIN + TAB_WIDTH
                    && event.y() >= y && event.y() < y + TAB_HEIGHT) {
                this.activeTab = tabs.get(index).id();
                this.scroll = 0;
                updateScroll();
                return true;
            }
        }

        int contentLeft = MARGIN + TAB_WIDTH + 8;
        int contentRight = this.width - MARGIN;
        int listBottom = this.height - FOOTER;
        int maxRows = Math.max(1, (listBottom - LIST_TOP) / ROW_HEIGHT);
        List<Option> visible = visibleRows();
        int first = (int) (scroll / ROW_HEIGHT);
        if (event.x() >= contentLeft && event.x() <= contentRight
                && event.y() >= LIST_TOP && event.y() < listBottom) {
            int index = first + (int) ((event.y() - LIST_TOP) / ROW_HEIGHT);
            if (index >= 0 && index < visible.size() && index < first + maxRows) {
                Option option = visible.get(index);
                option.setter().accept(!option.getter().get());
                service.saveConfig();
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - verticalAmount * ROW_HEIGHT));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateScroll() {
        int listBottom = this.height - FOOTER;
        int rows = (listBottom - LIST_TOP) / ROW_HEIGHT;
        this.maxScroll = Math.max(0, visibleRows().size() * ROW_HEIGHT - rows * ROW_HEIGHT);
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
    }

    /** Files and bytes the option would free, from the scan taken when the screen opened. */
    private Component measure(Option option) {
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
            return Component.translatable("cleanmcmod.status.missing");
        }
        return Component.literal(files + " · " + piboh.cleanmc.mod.clean.CleanReport.formatBytes(bytes));
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
        updateScroll();
        CleanMcMod.LOGGER.info("Cache settings reset to defaults");
    }

    private List<Option> visibleRows() {
        List<Option> visible = new ArrayList<>();
        for (Option option : options()) {
            if (!"all".equals(this.activeTab) && !option.tab().equals(this.activeTab)) {
                continue;
            }
            visible.add(option);
        }
        return visible;
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
