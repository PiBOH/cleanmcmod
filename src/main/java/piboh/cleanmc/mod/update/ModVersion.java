package piboh.cleanmc.mod.update;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * One version of a mod, as a download source publishes it.
 *
 * <p>{@code sha1} is null for CurseForge and GitHub, which publish no hash per file: those
 * downloads cannot be verified and the installer says so instead of pretending to check.</p>
 */
public record ModVersion(
        String id,
        String projectId,
        String versionNumber,
        String fileName,
        String downloadUrl,
        String sha1,
        List<String> gameVersions,
        List<String> loaders,
        List<Dependency> dependencies,
        ModSource source) {

    public ModVersion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(versionNumber, "versionNumber");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(downloadUrl, "downloadUrl");
        gameVersions = List.copyOf(gameVersions);
        loaders = List.copyOf(loaders);
        dependencies = List.copyOf(dependencies);
        source = source == null ? ModSource.MODRINTH : source;
    }

    /** A Modrinth version whose dependency list is not known. */
    public ModVersion(String id, String projectId, String versionNumber, String fileName,
            String downloadUrl, String sha1, List<String> gameVersions, List<String> loaders) {
        this(id, projectId, versionNumber, fileName, downloadUrl, sha1, gameVersions, loaders,
                List.of(), ModSource.MODRINTH);
    }

    /** A Modrinth version. */
    public ModVersion(String id, String projectId, String versionNumber, String fileName,
            String downloadUrl, String sha1, List<String> gameVersions, List<String> loaders,
            List<Dependency> dependencies) {
        this(id, projectId, versionNumber, fileName, downloadUrl, sha1, gameVersions, loaders,
                dependencies, ModSource.MODRINTH);
    }

    /** True when this version is published for the running game version and one of our loaders. */
    public boolean matches(String gameVersion, Collection<String> allowedLoaders) {
        if (gameVersion != null && !gameVersion.isBlank() && !gameVersions.contains(gameVersion)) {
            return false;
        }
        if (allowedLoaders.isEmpty()) {
            return true;
        }
        for (String loader : loaders) {
            if (allowedLoaders.contains(loader)) {
                return true;
            }
        }
        return false;
    }

    /** True when a hash exists to verify the download against. */
    public boolean checksummed() {
        return sha1 != null && !sha1.isBlank();
    }

    public ModVersion withSource(ModSource other) {
        return new ModVersion(id, projectId, versionNumber, fileName, downloadUrl, sha1, gameVersions,
                loaders, dependencies, other);
    }

    /** Identity of the file, for de-duplicating a batch that downloads the same jar twice. */
    public String fileKey() {
        return checksummed() ? "sha1:" + sha1 : "url:" + downloadUrl;
    }
}
