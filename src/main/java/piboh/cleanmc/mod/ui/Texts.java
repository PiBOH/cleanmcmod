package piboh.cleanmc.mod.ui;

import piboh.cleanmc.mod.clean.CacheTarget;
import piboh.cleanmc.mod.clean.CleanReport;
import piboh.cleanmc.mod.clean.TargetResult;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/** Localised labels shared by the screens and the client command. */
public final class Texts {

    public static final int COLOR_OK = 0xFF55FF55;
    public static final int COLOR_WARN = 0xFFFFAA00;
    public static final int COLOR_INFO = 0xFFAAAAAA;
    public static final int COLOR_ERROR = 0xFFFF5555;

    private Texts() {
    }

    public static Text targetName(CacheTarget target) {
        return target.label() != null
                ? Text.literal(target.label())
                : Text.translatable(target.translationKey());
    }

    public static Text status(TargetResult result) {
        return Text.translatable("cleanmcmod.status." + result.status().name().toLowerCase(java.util.Locale.ROOT));
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

    public static MutableText bytes(long bytes) {
        return Text.literal(CleanReport.formatBytes(bytes));
    }

}
