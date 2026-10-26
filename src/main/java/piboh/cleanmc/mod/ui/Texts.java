package piboh.cleanmc.mod.ui;

import piboh.cleanmc.mod.clean.CacheTarget;
import piboh.cleanmc.mod.clean.CleanReport;
import piboh.cleanmc.mod.clean.TargetResult;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Localised labels shared by the screens and the client command. */
public final class Texts {

    public static final int COLOR_OK = 0xFF55FF55;
    public static final int COLOR_WARN = 0xFFFFAA00;
    public static final int COLOR_INFO = 0xFFAAAAAA;
    public static final int COLOR_ERROR = 0xFFFF5555;

    private Texts() {
    }

    public static Component targetName(CacheTarget target) {
        return target.label() != null
                ? Component.literal(target.label())
                : Component.translatable(target.translationKey());
    }

    public static Component status(TargetResult result) {
        return Component.translatable("cleanmcmod.status." + result.status().name().toLowerCase(java.util.Locale.ROOT));
    }

    public static int statusColor(TargetResult result) {
        return switch (result.status()) {
            case CLEANED -> COLOR_OK;
            case PARTIAL -> COLOR_WARN;
            case DRY_RUN -> 0xFF55FFFF;
            case MISSING -> COLOR_INFO;
            case FAILED -> COLOR_ERROR;
        };
    }

    public static MutableComponent bytes(long bytes) {
        return Component.literal(CleanReport.formatBytes(bytes));
    }

}
