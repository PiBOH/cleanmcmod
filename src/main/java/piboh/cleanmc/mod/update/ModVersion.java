package piboh.cleanmc.mod.update;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** A version entry returned by Modrinth. */
public record ModVersion(
        String id,
        String projectId,
        String versionNumber,
        String fileName,
        String downloadUrl,
        String sha1,
        List<String> gameVersions,
        List<String> loaders,
        List<Dependency> dependencies) {

    public ModVersion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(versionNumber, "versionNumber");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(downloadUrl, "downloadUrl");
        gameVersions = List.copyOf(gameVersions);
        loaders = List.copyOf(loaders);
        dependencies = List.copyOf(dependencies);
    }

    /** A version whose dependency list is not known. */
    public ModVersion(String id, String projectId, String versionNumber, String fileName,
            String downloadUrl, String sha1, List<String> gameVersions, List<String> loaders) {
        this(id, projectId, versionNumber, fileName, downloadUrl, sha1, gameVersions, loaders, List.of());
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
}
