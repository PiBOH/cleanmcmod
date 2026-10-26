package piboh.cleanmc.mod.clean;

import java.util.Objects;

/** What happened to a single cache target during a clean run. */
public record TargetResult(
        String targetId,
        TargetStatus status,
        Counts deleted,
        Counts planned,
        int filesSkipped,
        String message) {

    public static final String NOT_PRESENT = "not present";

    public TargetResult {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(deleted, "deleted");
        Objects.requireNonNull(planned, "planned");
        filesSkipped = Math.max(0, filesSkipped);
    }

    /**
     * A file/volume pair. {@code deleted} is what actually left the disk; {@code planned} is what a
     * dry run would remove. Exactly one of them is non-zero, and a dry run always reports
     * {@link #filesDeleted()} as zero.
     */
    public record Counts(int files, long bytes) {
        public static final Counts NONE = new Counts(0, 0L);

        public Counts {
            files = Math.max(0, files);
            bytes = Math.max(0L, bytes);
        }
    }

    public static TargetResult missing(String targetId) {
        return new TargetResult(targetId, TargetStatus.MISSING, Counts.NONE, Counts.NONE, 0, NOT_PRESENT);
    }

    public static TargetResult failed(String targetId, String message) {
        return new TargetResult(targetId, TargetStatus.FAILED, Counts.NONE, Counts.NONE, 0, message);
    }

    public static TargetResult wouldRemove(String targetId, int files, long bytes) {
        return new TargetResult(targetId, TargetStatus.DRY_RUN, Counts.NONE, new Counts(files, bytes), 0, null);
    }

    public static TargetResult cleaned(String targetId, int files, long bytes) {
        return new TargetResult(targetId, TargetStatus.CLEANED, new Counts(files, bytes), Counts.NONE, 0, null);
    }

    public static TargetResult partial(String targetId, int files, long bytes, int filesSkipped, String message) {
        return new TargetResult(targetId, TargetStatus.PARTIAL, new Counts(files, bytes), Counts.NONE,
                filesSkipped, message);
    }

    public boolean successful() {
        return status == TargetStatus.CLEANED || status == TargetStatus.PARTIAL || status == TargetStatus.DRY_RUN;
    }

    /** One log/CLI line, English only — in-game text is built from the lang files. */
    public String describe() {
        return switch (status) {
            case CLEANED -> "%s: %d files, %s".formatted(
                    targetId, deleted.files(), CleanReport.formatBytes(deleted.bytes()));
            case PARTIAL -> "%s: %d files, %s (%d skipped: %s)".formatted(
                    targetId, deleted.files(), CleanReport.formatBytes(deleted.bytes()), filesSkipped, message);
            case DRY_RUN -> "%s: would delete %d files, %s".formatted(
                    targetId, planned.files(), CleanReport.formatBytes(planned.bytes()));
            case MISSING -> targetId + ": " + NOT_PRESENT;
            case FAILED -> "%s: failed (%s)".formatted(targetId, message);
        };
    }
}
