package piboh.cleanmc.mod.ui;

import net.fabricmc.loader.api.FabricLoader;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings in the layout Sodium uses: one scrolling page with a search field on top, a header per
 * group, and one row per option with its control on the right. There is no side column, so nothing
 * in this screen depends on a hit test that a widget could swallow.
 *
 * <p>The bottom group is About, which names the creator and the mods this one took its layout and
 * its update list from, and the Interface group carries the language row. Every change is written to
 * disk immediately through {@link CleanMcModService#saveConfig()}; a row that differs from the
 * default is marked, so a reset is always one click away.</p>
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
        private final ButtonWidget button;
        private int y;
        private boolean visible;

        private Row(Entry entry, ButtonWidget button) {
            this.entry = entry;
            this.button = button;
        }
    }

    private final Screen parent;
    private final CleanMcModService service;
    private final CleanMcModConfig defaults = new CleanMcModConfig();
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Integer> headers = new HashMap<>();

    private TextFieldWidget search;
    private String filter = "";
    private double scroll;
    private int maxScroll;
    private CacheScan scan;
    private Row hovered;

    public CacheSettingsScreen(Screen parent, CleanMcModService service) {
        super(Lang.text("cleanmcmod.screen.settings.title"));
        this.parent = parent;
        this.service = service;
    }

    @Override
    protected void init() {
        this.scan = service.scan();
        this.rows.clear();
        this.headers.clear();

        this.search = new TextFieldWidget(this.textRenderer, MARGIN, CONTENT_TOP - 26,
                Math.min(220, this.width / 2), 20, Lang.text("cleanmcmod.settings.search"));
        this.search.setPlaceholder(Lang.text("cleanmcmod.settings.search"));
        this.search.setText(this.filter);
        this.search.setChangedListener(text -> {
            this.filter = text == null ? "" : text;
            this.scroll = 0;
            layout();
        });
        this.addDrawableChild(this.search);

        for (Entry entry : entries()) {
            ButtonWidget button = switch (entry.kind()) {
                case TOGGLE -> ButtonWidget.builder(stateLabel(entry), pressed -> {
                            entry.setter().accept(!entry.getter().get());
                            service.saveConfig();
                            pressed.setMessage(stateLabel(entry));
                            layout();
                        })
                        .dimensions(this.width - MARGIN - CONTROL_WIDTH, CONTENT_TOP, CONTROL_WIDTH, 18)
                        .build();
                case LANGUAGE -> ButtonWidget.builder(languageLabel(), pressed -> {
                            String next = Lang.next(service.config().language);
                            service.config().language = next;
                            Lang.select(next);
                            service.saveConfig();
                            pressed.setMessage(languageLabel());
                        })
                        .dimensions(this.width - MARGIN - CONTROL_WIDTH, CONTENT_TOP, CONTROL_WIDTH, 18)
                        .build();
                case INFO -> null;
            };
            this.rows.add(new Row(entry, button));
            if (button != null) {
                this.addDrawableChild(button);
            }
        }

        this.addDrawableChild(ButtonWidget.builder(Lang.text("cleanmcmod.settings.reset"),
                        button -> resetAll())
                .dimensions(MARGIN, this.height - 30, 92, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Lang.text("gui.done"), button -> this.close())
                .dimensions(this.width - MARGIN - 92, this.height - 30, 92, 20)
                .build());

        layout();
    }

    /**
     * The label of the language control: the name the chosen language calls itself, or the game
     * language entry. Minecraft has no per-mod language, so this switches our own table only.
     */
    private Text languageLabel() {
        String name = Lang.name(service.config().language);
        return name == null ? Lang.text("cleanmcmod.settings.language.auto") : Text.literal(name);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, 0xFFFFFFFF);

        int contentRight = this.width - MARGIN;
        int bottom = this.height - FOOTER;
        context.fill(MARGIN - 4, CONTENT_TOP - 4, contentRight + 4, bottom, 0x33101010);

        this.hovered = null;
        for (Map.Entry<String, Integer> header : headers.entrySet()) {
            int y = header.getValue();
            if (y < CONTENT_TOP - HEADER_HEIGHT || y >= bottom) {
                continue;
            }
            context.drawText(this.textRenderer, Lang.text(header.getKey()), MARGIN, y + 6,
                    Texts.COLOR_OK, false);
            context.fill(MARGIN, y + 17, contentRight, y + 18, 0x33FFFFFF);
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
                context.fill(MARGIN - 4, y, contentRight, y + ROW_HEIGHT - 2, 0x22FFFFFF);
            }
            if (row.entry.kind() == Kind.INFO) {
                context.drawText(this.textRenderer, info(row.entry), MARGIN + 4, y + 5,
                        Texts.COLOR_INFO, false);
                continue;
            }
            boolean modified = row.entry.kind() == Kind.TOGGLE
                    && row.entry.getter().get() != row.entry.defaultValue();
            Text label = Lang.text(row.entry.labelKey());
            Text name = modified ? Text.literal("● ").append(label) : label;
            context.drawText(this.textRenderer, name, MARGIN + 4, y + 5, 0xFFFFFFFF, false);

            Text detail = row.entry.kind() == Kind.INFO ? null : measure(row.entry);
            if (detail != null) {
                int width = this.textRenderer.getWidth(detail);
                int limit = this.width - MARGIN - CONTROL_WIDTH - 10;
                if (limit - width > MARGIN + 60) {
                    context.drawText(this.textRenderer, detail, limit - width, y + 5, Texts.COLOR_INFO, false);
                }
            }
        }

        if (maxScroll > 0) {
            int trackTop = CONTENT_TOP;
            int trackHeight = bottom - CONTENT_TOP;
            int thumbHeight = Math.max(16, (int) (trackHeight
                    * (trackHeight / (double) (trackHeight + maxScroll))));
            int thumbTop = trackTop + (int) ((trackHeight - thumbHeight) * (scroll / maxScroll));
            context.fill(contentRight + 2, trackTop, contentRight + 4, trackTop + trackHeight, 0x22FFFFFF);
            context.fill(contentRight + 2, thumbTop, contentRight + 4, thumbTop + thumbHeight, 0x88FFFFFF);
        }

        renderExplanation(context);
    }

    private Text info(Entry entry) {
        return Lang.text(entry.labelKey(), version());
    }

    private void renderExplanation(DrawContext context) {
        Text line;
        int color = Texts.COLOR_INFO;
        if (this.hovered == null || this.hovered.entry.kind() == Kind.INFO) {
            line = Lang.text("cleanmcmod.screen.settings.intro");
        } else if (this.hovered.entry.kind() == Kind.LANGUAGE) {
            line = Lang.text("cleanmcmod.settings.language.hint");
        } else {
            Entry option = this.hovered.entry;
            Text label = Lang.text(option.labelKey());
            List<CacheTarget> targets = targetsFor(option);
            if (targets.isEmpty()) {
                line = Lang.text("cleanmcmod.settings.explain", label,
                        Lang.text("cleanmcmod.screen.settings.intro"));
            } else {
                line = Lang.text("cleanmcmod.settings.explain", label,
                        Lang.text(targets.get(0).safety().translationKey()));
                color = Texts.COLOR_OK;
            }
        }
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(this.textRenderer.trimToWidth(line.getString(), this.width - 2 * MARGIN)),
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
                        : languageLabel());
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
        return Lang.text(entry.labelKey()).getString().toLowerCase(Locale.ROOT).contains(needle);
    }

    private Text stateLabel(Entry option) {
        return option.getter().get()
                ? Lang.text("cleanmcmod.settings.on")
                : Lang.text("cleanmcmod.settings.off");
    }

    /** Files and bytes the option would free, from the scan taken when the screen opened. */
    private Text measure(Entry option) {
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
            return Lang.text("cleanmcmod.status.missing");
        }
        return Text.literal(files + " · " + piboh.cleanmc.mod.clean.CleanReport.formatBytes(bytes));
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
        return FabricLoader.getInstance().getModContainer("cleanmcmod")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
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
