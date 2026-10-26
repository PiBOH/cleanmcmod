package piboh.cleanmc.mod.clean;

import java.nio.file.Path;
import java.util.Objects;

/**
 * One thing that can be scanned and cleaned.
 *
 * @param id       stable identifier, also the suffix of the {@code cleanmcmod.target.<id>} lang key
 * @param category where the target comes from
 * @param safety   whether deleting it forces a re-download
 * @param path     absolute, normalised path inside the game directory
 * @param label    literal display name for detected/custom targets, or {@code null} for built-ins
 */
public record CacheTarget(String id, CacheCategory category, CacheSafety safety, Path path, String label) {

    public CacheTarget {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(safety, "safety");
        Objects.requireNonNull(path, "path");
        path = path.toAbsolutePath().normalize();
    }

    public static CacheTarget builtin(String id, CacheCategory category, CacheSafety safety, Path path) {
        return new CacheTarget(id, category, safety, path, null);
    }

    public static CacheTarget detected(String id, CacheCategory category, CacheSafety safety, Path path, String label) {
        return new CacheTarget(id, category, safety, path, label);
    }

    public String translationKey() {
        return "cleanmcmod.target." + id;
    }

    /** Path relative to {@code gameDir}, with forward slashes, for logs and labels. */
    public String relativePath(Path gameDir) {
        Path root = gameDir.toAbsolutePath().normalize();
        Path rel = path.startsWith(root) ? root.relativize(path) : path;
        String text = rel.toString().replace('\\', '/');
        return text.isEmpty() ? "." : text;
    }
}
