package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.network.chat.Component;
import piboh.cleanmc.mod.clean.CacheScan;
import piboh.cleanmc.mod.clean.CacheTarget;
import piboh.cleanmc.mod.clean.CleanMcModService;
import piboh.cleanmc.mod.clean.TargetState;
import piboh.cleanmc.mod.config.CleanMcModConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings in the layout Sodium uses: one scrolling page with a search field on top, a header per
 * group, and one row per option with its control on the right. There is no side column, so nothing
 * in this screen depends on a hit test a widget could swallow.
 *
 * <p>The bottom group is About, which names the creator and the mods this one took its layout and
 * its update list from, and the Interface group carries the language row.</p>
 */
public class CacheSettingsScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_HEIGHT = 22;
    private static final int CONTROL_WIDTH = 78;
    private static final int CONTENT_TOP = 56;
    private static final int FOOTER = 58;

    private enum Kind {
        TOGGLE, LANGUAGE, INFO
    }

    private record Entry(String labelKey, String sectionKey, Kind kind, String targetPrefix,
            boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
    }

    private static final class Row {
        private final Entry entry;
        private final Button button;
        private int y;
        private boolean visible;

        private Row(Entry entry, Button button) {
            this.entry = entry;
            this.button = button;
        }
    }

    private final Screen parent;
    private final CleanMcModService service;
    private final CleanMcModConfig defaults = new CleanMcModConfig();
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Integer> headers = new HashMap<>();

    private EditBox search;
    private String filter = "";
    private double scroll;
    private int maxScroll;
    private CacheScan scan;
    private Row hovered;

    public CacheSettingsScreen(Screen parent, CleanMcModService service) {
        super(Component.translatable("cleanmcmod.screen.settings.title"));
        this.parent = parent;
        this.service = service;
    }

    @Override
    protected void init() {
        this.scan = service.scan();
        this.rows.clear();
        this.headers.clear();

        this.search = new EditBox(this.font, MARGIN, CONTENT_TOP - 26, Math.min(220, this.width / 2), 20,
                Component.translatable("cleanmcmod.settings.search"));
        this.search.setValue(this.filter);
        this.search.setResponder(text -> {
            this.filter = text == null ? "" : text;
            this.scroll = 0;
            layout();
        });
        this.addRenderableWidget(this.search);

        for (Entry entry : entries()) {
            Button button = switch (entry.kind()) {
                case TOGGLE -> Button.builder(stateLabel(entry), pressed -> {
                            entry.setter().accept(!entry.getter().get());
                            service.saveConfig();
                            pressed.setMessage(stateLabel(entry));
                            layout();
                        })
                        .bounds(this.width - MARGIN - CONTROL_WIDTH, CONTENT_TOP, CONTROL_WIDTH, 18)
                        .build();
                case LANGUAGE -> Button.builder(Component.translatable("cleanmcmod.button.change"),
                                pressed -> openLanguages())
                        .bounds(this.width - MARGIN - CONTROL_WIDTH, CONTENT_TOP, CONTROL_WIDTH, 18)
                        .build();
                case INFO -> null;
            };
            this.rows.add(new Row(entry, button));
            if (button != null) {
                this.addRenderableWidget(button);
            }
        }

        this.addRenderableWidget(Button.builder(Component.translatable("cleanmcmod.settings.reset"),
                        button -> resetAll())
                .bounds(MARGIN, this.height - 30, 92, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                        button -> this.onClose())
                .bounds(this.width - MARGIN - 92, this.height - 30, 92, 20)
                .build());

        layout();
    }

    /** The game's own language selector: it already owns options.txt and the resource reload. */
    private void openLanguages() {
        this.minecraft.setScreenAndShow(new LanguageSelectScreen(this, this.minecraft.options,
                this.minecraft.getLanguageManager()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);

        int contentRight = this.width - MARGIN;
        int bottom = this.height - FOOTER;
        graphics.fill(MARGIN - 4, CONTENT_TOP - 4, contentRight + 4, bottom, 0x33101010);

        this.hovered = null;
        for (Map.Entry<String, Integer> header : headers.entrySet()) {
            int y = header.getValue();
            if (y < CONTENT_TOP - HEADER_HEIGHT || y >= bottom) {
                continue;
            }
            graphics.text(this.font, Component.translatable(header.getKey()), MARGIN, y + 6,
                    Texts.COLOR_OK, false);
            graphics.fill(MARGIN, y + 17, contentRight, y + 18, 0x33FFFFFF);
        }

        for (Row row : rows) {
            if (!row.visible) {
                continue;
            }
            int y = row.y;
            boolean isHovered = mouseX >= MARGIN && mouseX <= contentRight
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (isHovered) {
                this.hovered = row;
                graphics.fill(MARGIN - 4, y, contentRight, y + ROW_HEIGHT - 2, 0x22FFFFFF);
            }
            if (row.entry.kind() == Kind.INFO) {
                graphics.text(this.font, info(row.entry), MARGIN + 4, y + 5, Texts.COLOR_INFO, false);
                continue;
            }
            boolean modified = row.entry.kind() == Kind.TOGGLE
                    && row.entry.getter().get() != row.entry.defaultValue();
            Component label = Component.translatable(row.entry.labelKey());
            Component name = modified ? Component.literal("● ").append(label) : label;
            graphics.text(this.font, name, MARGIN + 4, y + 5, 0xFFFFFFFF, false);

            Component detail = row.entry.kind() == Kind.LANGUAGE
                    ? Component.translatable("language.name")
                    : measure(row.entry);
            if (detail != null) {
                int width = this.font.width(detail);
                int limit = this.width - MARGIN - CONTROL_WIDTH - 10;
                if (limit - width > MARGIN + 60) {
                    graphics.text(this.font, detail, limit - width, y + 5, Texts.COLOR_INFO, false);
                }
            }
        }

        if (maxScroll > 0) {
            int trackTop = CONTENT_TOP;
            int trackHeight = bottom - CONTENT_TOP;
            int thumbHeight = Math.max(16,
                    (int) (trackHeight * (trackHeight / (double) (trackHeight + maxScroll))));
            int thumbTop = trackTop + (int) ((trackHeight - thumbHeight) * (scroll / maxScroll));
            graphics.fill(contentRight + 2, trackTop, contentRight + 4, trackTop + trackHeight, 0x22FFFFFF);
            graphics.fill(contentRight + 2, thumbTop, contentRight + 4, thumbTop + thumbHeight, 0x88FFFFFF);
        }

        renderExplanationValue(graphics);
    }

    private Component info(Entry entry) {
        return Component.translatable(entry.labelKey(), version());
    }

    private void renderExplanationValue(GuiGraphicsExtractor graphics) {
        Component line;
        int color = Texts.COLOR_INFO;
        if (this.hovered == null || this.hovered.entry.kind() == Kind.INFO) {
            line = Component.translatable("cleanmcmod.screen.settings.intro");
        } else if (this.hovered.entry.kind() == Kind.LANGUAGE) {
            line = Component.translatable("cleanmcmod.settings.language.hint");
        } else {
            Entry option = this.hovered.entry;
            Component label = Component.translatable(option.labelKey());
            List<CacheTarget> targets = targetsFor(option);
            if (targets.isEmpty()) {
                line = Component.translatable("cleanmcmod.settings.explain", label,
                        Component.translatable("cleanmcmod.screen.settings.intro"));
            } else {
                line = Component.translatable("cleanmcmod.settings.explain", label,
                        Component.translatable(targets.get(0).safety().translationKey()));
                color = Texts.COLOR_OK;
            }
        }
        graphics.centeredText(this.font,
                Component.literal(this.font.plainSubstrByWidth(line.getString(), this.width - 2 * MARGIN)),
                this.width / 2, this.height - 50, color);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
            double verticalAmount) {
        if (maxScroll > 0) {
            this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - verticalAmount * ROW_HEIGHT));
            layout();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /** Places every row under its group header, keeping the ones scrolled out of view hidden. */
    private void layout() {
        int bottom = this.height - FOOTER;
        int y = CONTENT_TOP - (int) scroll;
        String section = null;
        headers.clear();
        for (Row row : rows) {
            row.visible = false;
            if (row.button != null) {
                row.button.visible = false;
            }
            if (!matches(row.entry)) {
                continue;
            }
            if (!row.entry.sectionKey().equals(section)) {
                section = row.entry.sectionKey();
                headers.put(section, y);
                y += HEADER_HEIGHT;
            }
            row.y = y;
            row.visible = y + ROW_HEIGHT > CONTENT_TOP && y < bottom;
            if (row.button != null) {
                row.button.setX(this.width - MARGIN - CONTROL_WIDTH);
                row.button.setY(y + 2);
                row.button.setMessage(row.entry.kind() == Kind.TOGGLE
                        ? stateLabel(row.entry)
                        : Component.translatable("cleanmcmod.button.change"));
                row.button.visible = row.visible;
            }
            y += ROW_HEIGHT;
        }
        int contentHeight = y + (int) scroll - CONTENT_TOP;
        this.maxScroll = Math.max(0, contentHeight - (bottom - CONTENT_TOP));
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
    }

    private boolean matches(Entry entry) {
        if (this.filter.isBlank()) {
            return true;
        }
        String needle = this.filter.trim().toLowerCase(Locale.ROOT);
        return Component.translatable(entry.labelKey()).getString().toLowerCase(Locale.ROOT).contains(needle);
    }

    private Component stateLabel(Entry option) {
        return option.getter().get()
                ? Component.translatable("cleanmcmod.settings.on")
                : Component.translatable("cleanmcmod.settings.off");
    }

    /** Files and bytes the option would free, from the scan taken when the screen opened. */
    private Component measure(Entry option) {
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

    private List<CacheTarget> targetsFor(Entry option) {
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
        for (Entry option : entries()) {
            if (option.kind() == Kind.TOGGLE && option.setter() != null) {
                option.setter().accept(option.defaultValue());
            }
        }
        service.saveConfig();
        this.scan = service.scan();
        layout();
        piboh.cleanmc.mod.CleanMcMod.LOGGER.info("Cache settings reset to defaults");
    }

    private List<Entry> entries() {
        CleanMcModConfig config = service.config();
        CleanMcModConfig defaults = this.defaults;
        return List.of(
                new Entry("cleanmcmod.setting.logs", "cleanmcmod.category.vanilla", Kind.TOGGLE,
                        "vanilla_logs", defaults.includeLogs, () -> config.includeLogs,
                        value -> config.includeLogs = value),
                new Entry("cleanmcmod.setting.crash_reports", "cleanmcmod.category.vanilla", Kind.TOGGLE,
                        "crash_reports", defaults.includeCrashReports, () -> config.includeCrashReports,
                        value -> config.includeCrashReports = value),
                new Entry("cleanmcmod.setting.debug_dumps", "cleanmcmod.category.vanilla", Kind.TOGGLE,
                        "debug_dumps", defaults.includeDebugDumps, () -> config.includeDebugDumps,
                        value -> config.includeDebugDumps = value),
                new Entry("cleanmcmod.setting.server_resource_packs", "cleanmcmod.category.vanilla",
                        Kind.TOGGLE, "server_resource_packs", defaults.includeServerResourcePacks,
                        () -> config.includeServerResourcePacks,
                        value -> config.includeServerResourcePacks = value),
                new Entry("cleanmcmod.setting.loader_caches", "cleanmcmod.category.loader", Kind.TOGGLE,
                        "fabric_remapped", defaults.includeLoaderCaches, () -> config.includeLoaderCaches,
                        value -> config.includeLoaderCaches = value),
                new Entry("cleanmcmod.setting.map_caches", "cleanmcmod.category.map", Kind.TOGGLE,
                        "map_cache", defaults.includeMapCaches, () -> config.includeMapCaches,
                        value -> config.includeMapCaches = value),
                new Entry("cleanmcmod.setting.auto_detect", "cleanmcmod.category.mod", Kind.TOGGLE,
                        "detected_", defaults.autoDetect, () -> config.autoDetect,
                        value -> config.autoDetect = value),
                new Entry("cleanmcmod.setting.asset_cache", "cleanmcmod.settings.tab.advanced", Kind.TOGGLE,
                        "asset_", defaults.includeAssetCache, () -> config.includeAssetCache,
                        value -> config.includeAssetCache = value),
                new Entry("cleanmcmod.setting.dry_run", "cleanmcmod.settings.tab.advanced", Kind.TOGGLE,
                        null, defaults.dryRun, () -> config.dryRun, value -> config.dryRun = value),
                new Entry("cleanmcmod.settings.language", "cleanmcmod.settings.section.interface",
                        Kind.LANGUAGE, null, false, () -> false, null),
                new Entry("cleanmcmod.about.version", "cleanmcmod.settings.section.about", Kind.INFO,
                        null, false, () -> false, null),
                new Entry("cleanmcmod.about.creator", "cleanmcmod.settings.section.about", Kind.INFO,
                        null, false, () -> false, null),
                new Entry("cleanmcmod.about.inspired", "cleanmcmod.settings.section.about", Kind.INFO,
                        null, false, () -> false, null),
                new Entry("cleanmcmod.about.links", "cleanmcmod.settings.section.about", Kind.INFO,
                        null, false, () -> false, null));
    }

    private static String version() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("cleanmcmod")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
