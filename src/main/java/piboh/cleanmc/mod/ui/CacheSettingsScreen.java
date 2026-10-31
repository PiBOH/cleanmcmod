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
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings in two panes: the sections on the left, the rows of the chosen one on the right, and
 * About as a page of its own instead of four lines at the bottom of a list.
 *
 * <p>The section list and the About page are drawn and hit tested here rather than built as
 * widgets: a widget cannot show the selected background or the per-section count, and every row
 * that does carry a control still has one, so nothing about a toggle stops being a normal button.</p>
 *
 * <p>Every change is written to disk immediately through {@link CleanMcModService#saveConfig()}; a
 * row that differs from the default is marked, so a reset is always one click away.</p>
 */
public class CacheSettingsScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int NAV_WIDTH = 138;
    private static final int NAV_TOP = 52;
    private static final int NAV_ROW = 20;
    private static final int ROW_HEIGHT = 22;
    private static final int CONTROL_WIDTH = 78;
    private static final int CONTENT_TOP = 56;
    private static final int FOOTER = 58;

    /** The sections in the order they are listed, which is also the order of the rows. */
    private static final List<String> SECTIONS = List.of(
            "cleanmcmod.category.vanilla",
            "cleanmcmod.category.loader",
            "cleanmcmod.category.map",
            "cleanmcmod.category.mod",
            "cleanmcmod.settings.tab.advanced",
            "cleanmcmod.settings.section.interface",
            "cleanmcmod.settings.section.about");

    private static final String ABOUT = "cleanmcmod.settings.section.about";

    private enum Kind {
        TOGGLE, LANGUAGE
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

    private TextFieldWidget search;
    private String filter = "";
    private String section = SECTIONS.get(0);
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

        int searchWidth = Math.min(180, Math.max(120, this.width / 4));
        this.search = new TextFieldWidget(this.textRenderer, this.width - MARGIN - searchWidth,
                CONTENT_TOP - 30, searchWidth, 18, Lang.text("cleanmcmod.settings.search"));
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
            };
            this.rows.add(new Row(entry, button));
            this.addDrawableChild(button);
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
        context.drawText(this.textRenderer, this.title, MARGIN, 16, 0xFFFFFFFF, false);

        int bottom = this.height - FOOTER;
        context.fill(MARGIN, CONTENT_TOP - 6, this.width - MARGIN, bottom + 4, 0x22101010);
        renderNav(context);

        if (this.section.equals(ABOUT) && this.filter.isBlank()) {
            renderAbout(context);
        } else {
            renderRows(context, mouseX, mouseY, bottom);
        }

        renderExplanation(context);
    }

    /** The section list: selected one lit, with how many options it holds. */
    private void renderNav(DrawContext context) {
        int x = MARGIN;
        int y = NAV_TOP;
        for (String key : SECTIONS) {
            boolean active = key.equals(this.section) && this.filter.isBlank();
            if (active) {
                context.fill(x - 4, y - 2, x + NAV_WIDTH, y + NAV_ROW - 4, 0x552F6B3D);
                context.fill(x - 4, y - 2, x - 2, y + NAV_ROW - 4, 0xFF7FE07F);
            }
            context.drawText(this.textRenderer, Lang.text(key), x,
                    y + 3, active ? 0xFFDCFFE3 : 0xFFB7BFCC, false);
            int count = count(key);
            if (count > 0) {
                String number = String.valueOf(count);
                context.drawText(this.textRenderer, Text.literal(number),
                        x + NAV_WIDTH - 10 - this.textRenderer.getWidth(number), y + 3,
                        Texts.COLOR_INFO, false);
            }
            y += NAV_ROW;
        }
    }

    private void renderRows(DrawContext context, int mouseX, int mouseY, int bottom) {
        int left = paneLeft();
        int contentRight = this.width - MARGIN;
        this.hovered = null;

        String shownSection = null;
        for (Row row : rows) {
            if (!row.visible) {
                continue;
            }
            int y = row.y;
            if (!row.entry.sectionKey().equals(shownSection)) {
                shownSection = row.entry.sectionKey();
                context.drawText(this.textRenderer, Lang.text(shownSection), left, y - 12,
                        Texts.COLOR_OK, false);
                context.fill(left, y - 4, contentRight, y - 3, 0x33FFFFFF);
            }
            boolean isHovered = mouseX >= left && mouseX <= contentRight
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (isHovered) {
                this.hovered = row;
                context.fill(left - 4, y, contentRight, y + ROW_HEIGHT - 2, 0x22FFFFFF);
            }
            boolean modified = row.entry.kind() == Kind.TOGGLE
                    && row.entry.getter().get() != row.entry.defaultValue();
            Text label = Lang.text(row.entry.labelKey());
            Text name = modified ? Text.literal("● ").append(label) : label;
            context.drawText(this.textRenderer, name, left, y + 5, 0xFFFFFFFF, false);

            Text detail = measure(row.entry);
            if (detail != null) {
                int width = this.textRenderer.getWidth(detail);
                int limit = contentRight - CONTROL_WIDTH - 10;
                if (limit - width > left + 60) {
                    context.drawText(this.textRenderer, detail, limit - width, y + 5,
                            Texts.COLOR_INFO, false);
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
    }

    /** About as a page: the mod, its version, who writes it and where it comes from. */
    private void renderAbout(DrawContext context) {
        int left = paneLeft();
        int right = this.width - MARGIN;
        int top = CONTENT_TOP;
        int cardBottom = top + 84;
        context.fill(left - 4, top - 6, right, cardBottom, 0xB0101418);
        context.fill(left - 4, top - 6, right, top - 5, 0x557FE07F);

        drawBroom(context, left + 4, top + 4);
        int textLeft = left + 54;
        context.drawText(this.textRenderer, Text.literal("CleanMCMod"), textLeft, top + 4,
                0xFFFFFFFF, false);
        context.drawText(this.textRenderer, Lang.text("cleanmcmod.about.version", version()),
                textLeft, top + 17, Texts.COLOR_OK, false);
        context.drawText(this.textRenderer, Lang.text("cleanmcmod.about.creator"),
                textLeft, top + 32, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, trim(Lang.text("cleanmcmod.about.inspired"),
                right - textLeft - 6), textLeft, top + 44, Texts.COLOR_INFO, false);

        int y = cardBottom + 10;
        int width = right - left;
        for (String key : List.of("cleanmcmod.about.tagline", "cleanmcmod.about.client_only",
                "cleanmcmod.about.links")) {
            context.drawText(this.textRenderer, trim(Lang.text(key), width), left, y,
                    key.endsWith("links") ? Texts.COLOR_INFO : 0xFFE8EDF4, false);
            y += 12;
        }
    }

    /** A broom, drawn from blocks so the About page needs no texture of its own. */
    private void drawBroom(DrawContext context, int x, int y) {
        context.fill(x, y, x + 42, y + 42, 0xFF16301D);
        context.fill(x, y, x + 42, y + 1, 0x667FE07F);
        context.fill(x, y + 41, x + 42, y + 42, 0x667FE07F);
        for (int step = 0; step < 7; step++) {
            context.fill(x + 27 - step * 2, y + 7 + step * 2, x + 30 - step * 2, y + 10 + step * 2,
                    0xFFB8F3C4);
        }
        context.fill(x + 14, y + 21, x + 23, y + 25, 0xFF7FE07F);
        for (int bristle = 0; bristle < 5; bristle++) {
            context.fill(x + 11 + bristle * 4, y + 25, x + 14 + bristle * 4, y + 37, 0xFFB8F3C4);
        }
    }

    private Text trim(Text text, int width) {
        return Text.literal(this.textRenderer.trimToWidth(text.getString(), width));
    }

    private void renderExplanation(DrawContext context) {
        Text line;
        int color = Texts.COLOR_INFO;
        if (this.hovered == null) {
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
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0 && this.filter.isBlank()
                && mouseX >= MARGIN && mouseX < MARGIN + NAV_WIDTH) {
            int y = NAV_TOP;
            for (String key : SECTIONS) {
                if (mouseY >= y - 2 && mouseY < y + NAV_ROW - 4) {
                    if (!key.equals(this.section)) {
                        this.section = key;
                        this.scroll = 0;
                        layout();
                    }
                    return true;
                }
                y += NAV_ROW;
            }
        }
        return super.mouseClicked(click, doubled);
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

    /** Places the rows of the chosen section, hiding the ones scrolled out of view. */
    private void layout() {
        int bottom = this.height - FOOTER;
        int y = CONTENT_TOP - (int) scroll;
        for (Row row : rows) {
            row.visible = false;
            row.button.visible = false;
            if (!shown(row.entry)) {
                continue;
            }
            row.y = y;
            row.visible = y + ROW_HEIGHT > CONTENT_TOP && y < bottom;
            row.button.setX(this.width - MARGIN - CONTROL_WIDTH);
            row.button.setY(y + 2);
            row.button.setMessage(row.entry.kind() == Kind.TOGGLE
                    ? stateLabel(row.entry)
                    : languageLabel());
            row.button.visible = row.visible;
            y += ROW_HEIGHT;
        }
        int contentHeight = y + (int) scroll - CONTENT_TOP;
        this.maxScroll = Math.max(0, contentHeight - (bottom - CONTENT_TOP));
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
    }

    /**
     * A row is shown when it belongs to the open section, or when the search matches it: the search
     * looks through every section, because hiding a match behind the wrong tab is worse than a list
     * that ignores the navigation for as long as it has a query.
     */
    private boolean shown(Entry entry) {
        if (!this.filter.isBlank()) {
            return matches(entry);
        }
        return entry.sectionKey().equals(this.section) && !this.section.equals(ABOUT);
    }

    private boolean matches(Entry entry) {
        String needle = this.filter.trim().toLowerCase(Locale.ROOT);
        return Lang.text(entry.labelKey()).getString().toLowerCase(Locale.ROOT).contains(needle);
    }

    private int count(String sectionKey) {
        int count = 0;
        for (Entry entry : entries()) {
            if (entry.sectionKey().equals(sectionKey)) {
                count++;
            }
        }
        return count;
    }

    private int paneLeft() {
        return MARGIN + NAV_WIDTH + 12;
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
                        Kind.LANGUAGE, null, false, () -> false, null));
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer("cleanmcmod")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    /** Darkens the world instead of blurring it: see {@link ScreenBackdrop}. */
    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY,
            float delta) {
        if (this.client != null && this.client.world == null) {
            this.renderPanoramaBackground(context, delta);
        }
        ScreenBackdrop.dim(context, this.width, this.height);
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
