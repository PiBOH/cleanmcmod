package piboh.cleanmc.mod.update;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * One jar in {@code mods/}. {@code modId}, {@code version} and {@code name} are null when the jar
 * has no readable metadata; {@code depends} is the {@code depends} block of its
 * {@code fabric.mod.json}; {@code githubRepo} is the {@code owner/repo} the mod itself names, when
 * it names one.
 */
public record LocalMod(Path path, String sha1, String modId, String version, Map<String, String> depends,
        String githubRepo, String name) {

    public LocalMod {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(sha1, "sha1");
        depends = depends == null ? Map.of() : Map.copyOf(depends);
    }

    public LocalMod(Path path, String sha1, String modId, String version, Map<String, String> depends) {
        this(path, sha1, modId, version, depends, null, null);
    }

    public LocalMod(Path path, String sha1, String modId, String version) {
        this(path, sha1, modId, version, Map.of(), null, null);
    }

    public String fileName() {
        return path.getFileName().toString();
    }

    public boolean hasGithubRepo() {
        return githubRepo != null && !githubRepo.isBlank();
    }

    /** The name to show in a list: metadata name, then mod id, then the file name. */
    public String displayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (modId != null && !modId.isBlank()) {
            return modId;
        }
        return fileName();
    }

    /** The CurseForge project slug a mod id most likely has ({@code xaeros_minimap} → {@code xaeros-minimap}). */
    public String curseSlug() {
        String id = modId == null ? null : modId.replace('_', '-').toLowerCase(Locale.ROOT);
        return id == null || id.isBlank() ? null : id;
    }
}
