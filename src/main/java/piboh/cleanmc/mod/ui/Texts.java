package piboh.cleanmc.mod.ui;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import piboh.cleanmc.mod.update.ModSource;

/** Localised labels shared by the screens and the client command. */
public final class Texts {

    public static final int COLOR_OK = 0xFF55FF55;
    public static final int COLOR_WARN = 0xFFFFAA00;
    public static final int COLOR_INFO = 0xFFAAAAAA;
    public static final int COLOR_ERROR = 0xFFFF5555;

    /** Per-source colours: Modrinth green, GitHub dark violet, CurseForge orange. */
    public static final int COLOR_MODRINTH = 0xFF55FF55;
    public static final int COLOR_GITHUB = 0xFF7A4FBF;
    public static final int COLOR_CURSEFORGE = 0xFFFF9A3C;

    private Texts() {
    }

    public static Component targetName(piboh.cleanmc.mod.clean.CacheTarget target) {
        return target.label() != null
                ? Component.literal(target.label())
                : Component.translatable(target.translationKey());
    }

    public static Component status(piboh.cleanmc.mod.clean.TargetResult result) {
        return Component.translatable("cleanmcmod.status." + result.status().name().toLowerCase(java.util.Locale.ROOT));
    }

    public static int statusColor(piboh.cleanmc.mod.clean.TargetResult result) {
        return switch (result.status()) {
            case CLEANED -> COLOR_OK;
            case PARTIAL -> COLOR_WARN;
            case DRY_RUN -> 0xFF55FFFF;
            case MISSING -> COLOR_INFO;
            case FAILED -> COLOR_ERROR;
        };
    }

    public static MutableComponent bytes(long bytes) {
        return Component.literal(piboh.cleanmc.mod.clean.CleanReport.formatBytes(bytes));
    }

    /** The name of the source an update comes from, for the row tag and its tooltip. */
    public static Component source(ModSource source) {
        return Component.translatable(source.translationKey());
    }

    /** The colour of the source tag, so the row says at a glance where the jar comes from. */
    public static int sourceColor(ModSource source) {
        return switch (source) {
            case MODRINTH -> COLOR_MODRINTH;
            case GITHUB -> COLOR_GITHUB;
            case CURSEFORGE -> COLOR_CURSEFORGE;
        };
    }
}
