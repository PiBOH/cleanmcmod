package piboh.cleanmc.mod.update;

import java.util.Objects;

/**
 * A local mod with a newer Modrinth version available, together with the dependency pre-flight
 * verdict for that candidate.
 */
public record ModUpdate(LocalMod local, ModVersion current, ModVersion candidate, String title,
        Compatibility.Verdict verdict) {

    public ModUpdate {
        Objects.requireNonNull(local, "local");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(candidate, "candidate");
        verdict = verdict == null ? Compatibility.Verdict.OK : verdict;
    }

    public ModUpdate(LocalMod local, ModVersion current, ModVersion candidate, String title) {
        this(local, current, candidate, title, Compatibility.Verdict.OK);
    }

    /** True when installing this update would break the installed set. */
    public boolean blocked() {
        return verdict.blocks();
    }

    /**
     * True when the block is only a missing or out-of-date dependency the updater fetches itself.
     * The screen marks these differently from a block the updater cannot fix.
     */
    public boolean resolvable() {
        return blocked() && !verdict.missing().isEmpty();
    }

    /** What the pre-flight wants to say about this update, or null when it has nothing to say. */
    public String reason() {
        return verdict.reason();
    }

    /** Project title when Modrinth knows it, otherwise the jar file name. */
    public String displayName() {
        return title == null || title.isBlank() ? local.fileName() : title;
    }

    /** The version the user sees: mod metadata when present, else the Modrinth version number. */
    public String installedVersion() {
        return local.version() == null || local.version().isBlank()
                ? current.versionNumber()
                : local.version();
    }

    public String availableVersion() {
        return candidate.versionNumber();
    }
}
