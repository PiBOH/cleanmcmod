package piboh.cleanmc.mod.clean;

import java.util.List;
import java.util.Locale;

/** Outcome of a clean run: one entry per target plus aggregate totals. */
public record CleanReport(boolean dryRun, long durationMillis, List<TargetResult> results) {

    public CleanReport {
        results = List.copyOf(results);
    }

    /** Files actually removed. Always zero for a dry run. */
    public int filesDeleted() {
        int total = 0;
        for (TargetResult result : results) {
            total += result.deleted().files();
        }
        return total;
    }

    /** Bytes actually freed. Always zero for a dry run. */
    public long bytesFreed() {
        long total = 0L;
        for (TargetResult result : results) {
            total += result.deleted().bytes();
        }
        return total;
    }

    /** Files a dry run would remove; zero when the run deleted for real. */
    public int filesPlanned() {
        int total = 0;
        for (TargetResult result : results) {
            total += result.planned().files();
        }
        return total;
    }

    /** Bytes a dry run would free; zero when the run deleted for real. */
    public long bytesPlanned() {
        long total = 0L;
        for (TargetResult result : results) {
            total += result.planned().bytes();
        }
        return total;
    }

    /** Targets that existed and were cleaned, or that a dry run would clean. */
    public long cleanedTargets() {
        return results.stream().filter(TargetResult::successful).count();
    }

    /** Human readable volume, e.g. {@code 1.4 MiB}. */
    public static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KiB", "MiB", "GiB", "TiB"};
        int unit = -1;
        do {
            value /= 1024.0;
            unit++;
        } while (value >= 1024.0 && unit < units.length - 1);
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }

    public String summary() {
        if (dryRun) {
            return "[dry run] %d targets, %d files would be removed, %s would be freed".formatted(
                    cleanedTargets(), filesPlanned(), formatBytes(bytesPlanned()));
        }
        return "%d targets, %d files, %s".formatted(cleanedTargets(), filesDeleted(), formatBytes(bytesFreed()));
    }
}
