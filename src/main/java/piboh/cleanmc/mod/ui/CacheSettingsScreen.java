package piboh.cleanmc.mod.ui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import piboh.cleanmc.mod.clean.CacheScan;
import piboh.cleanmc.mod.clean.CacheTarget;
import piboh.cleanmc.mod.clean.CleanMcModService;
import piboh.cleanmc.mod.clean.CleanReport;
import piboh.cleanmc.mod.clean.TargetState;
import piboh.cleanmc.mod.config.CleanMcModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings as one page: the mod card on top, the options in two compact columns under it.
 *
 * <p>There is no side column and nothing is hit tested by hand: every control is a widget, which is
 * the only shape that survives the input changes between 1.21.8, 1.21.9 and 26.x without a copy of
 * this screen per API. The card carries what About used to be a tab for: name, version, what the mod
 * does, who writes it, the size it can free and the two links.</p>
 *
 * <p>Every change is written to disk immediately through {@link CleanMcModService#saveConfig()}; a
 * row that differs from the default is marked, so a reset is always one click away.</p>
 */
public class CacheSettingsScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int TITLE_Y = 12;
    private static final int HERO_TOP = 28;
    private static final int HERO_HEIGHT = 66;
    private static final int ROW = 18;
    private static final int HEADER = 16;
    private static final int CONTROL = 60;
    private static final int FOOTER = 54;

    /** The sections in the order they are listed. The two halves below split them across columns. */
    private static final List<String> LEFT_SECTIONS = List.of(
            "cleanmcmod.category.vanilla",
            "cleanmcmod.category.loader");
    private static final List<String> RIGHT_SECTIONS = List.of(
            "cleanmcmod.category.map",
            "cleanmcmod.category.mod",
            "cleanmcmod.settings.tab.advanced",
            "cleanmcmod.settings.section.interface");

    private enum Kind {
        TOGGLE, LANGUAGE
    }

    private record Entry(String labelKey, String sectionKey, Kind kind, String targetPrefix,
            boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
    }

    private record Header(String key, int x, int y) {
    }

    private static final class Row {
        private final Entry entry;
        private final Button button;
        private int x;
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
    private final List<Header> headers = new ArrayList<>();

    private EditBox search;
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

        int searchWidth = Math.min(170, Math.max(110, this.width / 4));
        this.search = new EditBox(this.font, this.width - MARGIN - searchWidth,
                TITLE_Y - 3, searchWidth, 16, Lang.text("cleanmcmod.settings.search"));
        this.search.setHint(Lang.text("cleanmcmod.settings.search"));
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
                        .bounds(MARGIN, HERO_TOP, CONTROL, 16)
                        .build();
                case LANGUAGE -> Button.builder(languageLabel(), pressed -> {
                            String next = Lang.next(service.config().language);
                            service.config().language = next;
                            Lang.select(next);
                            service.saveConfig();
                            pressed.setMessage(languageLabel());
                        })
                        .bounds(MARGIN, HERO_TOP, CONTROL, 16)
                        .build();
            };
            this.rows.add(new Row(entry, button));
            this.addRenderableWidget(button);
        }

        this.addRenderableWidget(Button.builder(Lang.text("cleanmcmod.settings.reset"),
                        button -> resetAll())
                .bounds(MARGIN, this.height - 30, 92, 20)
                .build());
        this.addRenderableWidget(Button.builder(Lang.text("gui.done"), button -> this.onClose())
                .bounds(this.width - MARGIN - 92, this.height - 30, 92, 20)
                .build());

        layout();
    }

    /**
     * The label of the language control: the name the chosen language calls itself, or the game
     * language entry. Minecraft has no per-mod language, so this switches our own table only.
     */
    private Component languageLabel() {
        String name = Lang.name(service.config().language);
        return name == null ? Lang.text("cleanmcmod.settings.language.auto") : Component.literal(name);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, this.title, MARGIN, TITLE_Y, 0xFFFFFFFF, false);
        renderHero(graphics);
        renderColumns(graphics, mouseX, mouseY);
        renderExplanation(graphics);
    }

    /** The card: the mod, its version, what it frees right now, and where it comes from. */
    private void renderHero(GuiGraphicsExtractor graphics) {
        int left = MARGIN;
        int right = this.width - MARGIN;
        int top = HERO_TOP;
        graphics.fill(left, top, right, top + HERO_HEIGHT, 0x55101418);
        graphics.fill(left, top, right, top + 1, 0x667FE07F);
        graphics.fill(left, top, left + 1, top + HERO_HEIGHT, 0x337FE07F);

        drawIcon(graphics, left + 8, top + 13);
        int textLeft = left + 58;
        int textRight = right - 6;

        Component name = Component.literal("CleanMCMod " + version());
        graphics.text(this.font, name, textLeft, top + 8, 0xFFFFFFFF, false);
        Component stats = Lang.text("cleanmcmod.about.stats", found(), presentCategories(),
                Lang.SHIPPED.size());
        int statsWidth = this.font.width(stats);
        if (textLeft + this.font.width(name) + 12 < textRight - statsWidth) {
            graphics.text(this.font, stats, textRight - statsWidth, top + 8,
                    Texts.COLOR_INFO, false);
        }
        graphics.text(this.font, trim(Lang.text("cleanmcmod.about.tagline"),
                textRight - textLeft), textLeft, top + 22, Texts.COLOR_INFO, false);
        graphics.text(this.font, trim(Lang.text("cleanmcmod.about.creator"),
                textRight - textLeft), textLeft, top + 36, 0xFFFFFFFF, false);
        graphics.text(this.font, trim(Lang.text("cleanmcmod.about.links"),
                textRight - textLeft), textLeft, top + 50, Texts.COLOR_OK, false);
    }

    /**
     * The card slot: the mod's own icon in the frame the drawn broom used to sit in. The texture is
     * registered once by {@link ModIcons#own()} from the jar itself, so the card and the mod list show
     * the same image.
     */
    private void drawIcon(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 40, y + 40, 0xAA16301D);
        graphics.fill(x, y, x + 40, y + 1, 0x667FE07F);
        graphics.fill(x, y + 39, x + 40, y + 40, 0x667FE07F);
        Identifier icon = ModIcons.own();
        if (icon != null) {
            int size = ModIcons.cardSize();
            graphics.blit(icon, x + 4, y + 4, 0, 0, size, size, size, size);
        }
    }

    private void renderColumns(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int bottom = this.height - FOOTER;
        this.hovered = null;
        for (Header header : headers) {
            if (header.y() < HERO_TOP + HERO_HEIGHT || header.y() >= bottom) {
                continue;
            }
            graphics.text(this.font, Lang.text(header.key()), header.x(), header.y() + 2,
                    Texts.COLOR_OK, false);
        }
        for (Row row : rows) {
            if (!row.visible) {
                continue;
            }
            int y = row.y;
            boolean isHovered = mouseX >= row.x && mouseX <= row.x + columnWidth()
                    && mouseY >= y && mouseY < y + ROW;
            if (isHovered) {
                this.hovered = row;
                graphics.fill(row.x - 2, y - 1, row.x + columnWidth(), y + ROW - 2, 0x22FFFFFF);
            }
            boolean modified = row.entry.kind() == Kind.TOGGLE
                    && row.entry.getter().get() != row.entry.defaultValue();
            Component label = Lang.text(row.entry.labelKey());
            Component name = modified ? Component.literal("● ").append(label) : label;
            int room = columnWidth() - CONTROL - 8;
            graphics.text(this.font,
                    Component.literal(this.font.plainSubstrByWidth(name.getString(), room)),
                    row.x, y + 4, 0xFFFFFFFF, false);

            Component detail = measure(row.entry);
            if (detail != null) {
                int width = this.font.width(detail);
                int limit = row.x + columnWidth() - CONTROL - 6;
                if (limit - width > row.x + 40) {
                    graphics.text(this.font, detail, limit - width, y + 4,
                            Texts.COLOR_INFO, false);
                }
            }
        }

        if (maxScroll > 0) {
            int trackTop = HERO_TOP + HERO_HEIGHT + 6;
            int trackHeight = Math.max(10, bottom - trackTop);
            int thumbHeight = Math.max(14, (int) (trackHeight
                    * (trackHeight / (double) (trackHeight + maxScroll))));
            int thumbTop = trackTop + (int) ((trackHeight - thumbHeight) * (scroll / maxScroll));
            int x = this.width - MARGIN + 2;
            graphics.fill(x, trackTop, x + 2, trackTop + trackHeight, 0x22FFFFFF);
            graphics.fill(x, thumbTop, x + 2, thumbTop + thumbHeight, 0x88FFFFFF);
        }
    }

    private void renderExplanation(GuiGraphicsExtractor graphics) {
        Component line;
        int color = Texts.COLOR_INFO;
        if (this.hovered == null) {
            line = Lang.text("cleanmcmod.screen.settings.intro");
        } else if (this.hovered.entry.kind() == Kind.LANGUAGE) {
            line = Lang.text("cleanmcmod.settings.language.hint");
        } else {
            Entry option = this.hovered.entry;
            Component label = Lang.text(option.labelKey());
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
        graphics.centeredText(this.font,
                Component.literal(this.font.plainSubstrByWidth(line.getString(), this.width - 2 * MARGIN)),
                this.width / 2, this.height - 46, color);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
            double verticalAmount) {
        if (maxScroll > 0) {
            this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - verticalAmount * ROW));
            layout();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /** Places the rows in one or two columns, hiding the ones scrolled out of view. */
    private void layout() {
        for (Row row : rows) {
            row.visible = false;
            row.button.visible = false;
        }
        headers.clear();
        int top = HERO_TOP + HERO_HEIGHT + 8;
        int width = this.width - 2 * MARGIN;
        boolean twoColumns = width >= 380 && this.filter.isBlank();
        int column = columnWidth();
        int leftEnd = place(LEFT_SECTIONS, MARGIN, column, top);
        int rightEnd = twoColumns
                ? place(RIGHT_SECTIONS, MARGIN + column + 10, column, top)
                : place(RIGHT_SECTIONS, MARGIN, column, leftEnd);
        int contentBottom = Math.max(leftEnd, rightEnd);
        int viewport = Math.max(10, (this.height - FOOTER) - top);
        this.maxScroll = Math.max(0, (contentBottom - top) - viewport);
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
    }

    /** Lays one column out from an unscrolled y and returns the unscrolled y it ends at. */
    private int place(List<String> sections, int x, int width, int startY) {
        int y = startY - (int) scroll;
        for (String section : sections) {
            boolean titled = false;
            for (Row row : rows) {
                if (!row.entry.sectionKey().equals(section) || !matches(row.entry)) {
                    continue;
                }
                if (!titled) {
                    headers.add(new Header(section, x, y));
                    titled = true;
                    y += HEADER;
                }
                row.x = x;
                row.y = y;
                row.visible = y + ROW > HERO_TOP + HERO_HEIGHT && y < this.height - FOOTER;
                row.button.setX(x + width - CONTROL);
                row.button.setY(y);
                row.button.setMessage(row.entry.kind() == Kind.TOGGLE
                        ? stateLabel(row.entry)
                        : languageLabel());
                row.button.visible = row.visible;
                y += ROW;
            }
        }
        return y + (int) scroll;
    }

    /** How wide a row is: half the page on a wide window, the whole page on a narrow one. */
    private int columnWidth() {
        int width = this.width - 2 * MARGIN;
        return width >= 380 && this.filter.isBlank() ? (width - 10) / 2 : width;
    }

    private boolean matches(Entry entry) {
        if (this.filter.isBlank()) {
            return true;
        }
        String needle = this.filter.trim().toLowerCase(Locale.ROOT);
        return Lang.text(entry.labelKey()).getString().toLowerCase(Locale.ROOT).contains(needle);
    }

    private Component stateLabel(Entry option) {
        return option.getter().get()
                ? Lang.text("cleanmcmod.settings.on")
                : Lang.text("cleanmcmod.settings.off");
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
            return Lang.text("cleanmcmod.status.missing");
        }
        return Component.literal(files + " · " + CleanReport.formatBytes(bytes));
    }

    /** What the last scan found, for the card: the whole answer at a glance. */
    private String found() {
        return this.scan == null ? CleanReport.formatBytes(0) : CleanReport.formatBytes(this.scan.bytes());
    }

    private int presentCategories() {
        return this.scan == null ? 0 : this.scan.presentCount();
    }

    private Component trim(Component text, int width) {
        return Component.literal(this.font.plainSubstrByWidth(text.getString(), Math.max(20, width)));
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
                        Kind.LANGUAGE, null, false, () -> false, null));
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer("cleanmcmod")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    /** Skips the game's full-screen blur: see {@link ScreenBackdrop}. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level == null) {
            this.extractMenuBackground(graphics);
        }
        this.extractTransparentBackground(graphics);
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
