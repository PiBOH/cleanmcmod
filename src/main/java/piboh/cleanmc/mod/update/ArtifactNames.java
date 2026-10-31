package piboh.cleanmc.mod.update;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Reads the game version and the loader out of a published file name.
 *
 * <p>GitHub releases and CurseForge files carry no structured metadata a client mod can trust, so
 * the only evidence that a jar is meant for this game is its name. The rules are deliberately
 * strict: a file that names a different loader is never offered, and a file that names no game
 * version is not offered either.</p>
 */
public final class ArtifactNames {

    /** Loader tokens that can appear in a jar name. */
    private static final List<String> KNOWN_LOADERS = List.of("fabric", "quilt", "neoforge", "forge");

    private ArtifactNames() {
    }

    public static boolean looksLikeJar(String fileName) {
        if (fileName == null) {
            return false;
        }
        String name = fileName.toLowerCase(Locale.ROOT);
        if (!name.endsWith(".jar")) {
            return false;
        }
        return !name.contains("-sources") && !name.contains("-javadoc") && !name.contains("-dev");
    }

    /** True when the name does not contradict the loaders the game runs. */
    public static boolean namesLoader(String text, Collection<String> allowed) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        boolean namesAny = false;
        for (String loader : KNOWN_LOADERS) {
            if (!lower.contains(loader)) {
                continue;
            }
            namesAny = true;
            if (containsLoader(allowed, loader)) {
                return true;
            }
        }
        return !namesAny;
    }

    private static boolean containsLoader(Collection<String> allowed, String loader) {
        for (String candidate : allowed) {
            String lower = candidate.toLowerCase(Locale.ROOT);
            if (lower.equals(loader) || (loader.equals("forge") && lower.equals("neoforge"))) {
                return true;
            }
        }
        return false;
    }

    /** True when the text names the running game version, in any of the shapes mods use. */
    public static boolean namesGameVersion(String text, String gameVersion) {
        if (text == null || gameVersion == null || gameVersion.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String version = gameVersion.toLowerCase(Locale.ROOT);
        if (lower.contains(version)) {
            return true;
        }
        // "sodium-fabric-0.6.13+mc1.21.8" also writes the version without the leading 1.
        return version.startsWith("1.") && lower.contains("mc" + version);
    }
}
