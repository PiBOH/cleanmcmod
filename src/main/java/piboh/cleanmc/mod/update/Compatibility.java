package piboh.cleanmc.mod.update;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The pre-flight a mod update has to pass before it may touch {@code mods/}.
 *
 * <p>Pure: it reads the candidate's Modrinth dependencies, the jars that are actually installed and
 * the requirements those jars declare in their own {@code fabric.mod.json}, and answers
 * install / warn / block with a reason a player can read.</p>
 */
public final class Compatibility {

    /**
     * Modrinth project ids of the pieces the launcher/loader installs for every modded game.
     * Fabric API is the one required dependency almost every mod declares; a jar that is missing
     * from {@code mods/} but providable by the launcher must not block an update.
     */
    private static final Set<String> PLATFORM_PROJECTS = Set.of("P7dR8mSH");

    public enum Status {
        OK,
        WARN,
        BLOCK
    }

    /**
     * @param reason   null when there is nothing to say
     * @param missing  Modrinth project ids of required dependencies that could be fetched
     * @param outdated installed dependency jar per project id, replaced only if that project is
     *                 actually downloaded again
     */
    public record Verdict(Status status, String reason, List<String> missing,
            Map<String, Path> outdated) {

        public static final Verdict OK = new Verdict(Status.OK, null, List.of(), Map.of());

        public Verdict {
            missing = List.copyOf(missing == null ? List.of() : missing);
            outdated = Map.copyOf(outdated == null ? Map.of() : outdated);
        }

        public static Verdict block(String reason) {
            return new Verdict(Status.BLOCK, reason, List.of(), Map.of());
        }

        public static Verdict block(String reason, List<String> missing) {
            return new Verdict(Status.BLOCK, reason, missing, Map.of());
        }

        public static Verdict block(String reason, List<String> missing, Map<String, Path> outdated) {
            return new Verdict(Status.BLOCK, reason, missing, outdated);
        }

        public static Verdict warn(String reason) {
            return new Verdict(Status.WARN, reason, List.of(), Map.of());
        }

        public boolean blocks() {
            return status == Status.BLOCK;
        }
    }

    private Compatibility() {
    }

    /**
     * @param updated            the installed jar that would be replaced
     * @param candidate          the version that would replace it
     * @param installed          every jar in {@code mods/}, the updated one included
     * @param installedVersions  Modrinth version of each installed jar, by SHA-1
     * @param projectIdByModId   Modrinth project id of each installed mod, when known
     * @param titles             project id to display name, from Modrinth
     */
    public static Verdict check(
            LocalMod updated,
            ModVersion candidate,
            List<LocalMod> installed,
            Map<String, ModVersion> installedVersions,
            Map<String, String> projectIdByModId,
            Map<String, String> titles) {

        Map<String, LocalMod> installedByProject = new HashMap<>();
        for (LocalMod mod : installed) {
            String projectId = mod.modId() == null ? null : projectIdByModId.get(mod.modId());
            if (projectId != null) {
                installedByProject.put(projectId, mod);
            }
        }

        for (Dependency dependency : candidate.dependencies()) {
            if (dependency.incompatible() && installedByProject.containsKey(dependency.projectId())) {
                return Verdict.block("it is incompatible with " + name(dependency.projectId(), titles)
                        + ", which is installed");
            }
        }

        for (Dependency dependency : candidate.dependencies()) {
            if (!dependency.required() || PLATFORM_PROJECTS.contains(dependency.projectId())) {
                continue;
            }
            LocalMod present = installedByProject.get(dependency.projectId());
            if (present == null) {
                return Verdict.block("it needs " + name(dependency.projectId(), titles)
                        + ", which is not in mods/",
                        List.of(dependency.projectId()));
            }
            // Modrinth pins a dependency to one build. Presence alone is not enough: an older jar
            // of the same project (a Sodium from before a rewrite) passes that test silently.
            // Builds are compared by their Modrinth id, not by their version string: a project can
            // change numbering scheme mid-history (0.5.8+mc1.20.1 vs mc1.21.8-0.7.2-fabric), and
            // then any string ordering answers nonsense.
            ModVersion installedVersion = installedVersions.get(present.sha1());
            if (dependency.versionId() != null && installedVersion != null
                    && !dependency.versionId().equals(installedVersion.id())) {
                return Verdict.block("it asks for a different " + name(dependency.projectId(), titles)
                        + " build than the installed " + installedVersion.versionNumber(),
                        List.of(dependency.projectId()),
                        Map.of(dependency.projectId(), present.path()));
            }
        }

        for (LocalMod other : installed) {
            if (other.modId() == null || other.modId().equals(updated.modId())) {
                continue;
            }
            String range = other.depends().get(updated.modId());
            if (range == null) {
                continue;
            }
            Boolean satisfied = VersionRange.satisfies(candidate.versionNumber(), range);
            if (satisfied == null) {
                return Verdict.warn(other.modId() + " asks for " + updated.modId() + " " + range
                        + ", which cannot be checked");
            }
            if (!satisfied) {
                return Verdict.block(other.modId() + " needs " + updated.modId() + " " + range
                        + ", so " + candidate.versionNumber() + " would break it");
            }
        }
        return Verdict.OK;
    }

    /** True for the projects the launcher/loader provides, which never need a jar from us. */
    public static boolean platformProvided(String projectId) {
        return PLATFORM_PROJECTS.contains(projectId);
    }

    private static String name(String projectId, Map<String, String> titles) {
        String title = titles == null ? null : titles.get(projectId);
        return title == null || title.isBlank() ? projectId : title;
    }
}
