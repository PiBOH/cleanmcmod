package piboh.cleanmc.mod.update;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * One jar in {@code mods/}. {@code modId} and {@code version} are null when the jar has no readable
 * metadata; {@code depends} is the {@code depends} block of its {@code fabric.mod.json}.
 */
public record LocalMod(Path path, String sha1, String modId, String version, Map<String, String> depends) {

    public LocalMod {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(sha1, "sha1");
        depends = depends == null ? Map.of() : Map.copyOf(depends);
    }

    public LocalMod(Path path, String sha1, String modId, String version) {
        this(path, sha1, modId, version, Map.of());
    }

    public String fileName() {
        return path.getFileName().toString();
    }
}
