package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sources spell the same build differently: AppleSkin is {@code 3.0.10+mc26.3} on one and
 * {@code 26.3-3.0.10} on another. Offering that as an update means asking the installer to replace
 * the jar the game has open, which Windows refuses.
 */
class SameBuildTest {

    @Test
    void theSameBuildSpelledDifferentlyIsNotAnUpdate() {
        assertTrue(VersionComparator.sameBuild("3.0.10+mc26.3", "26.3-3.0.10", "26.3"));
        assertTrue(VersionComparator.sameBuild("3.3.12", "3.3.12-26.3", "26.3"));
        assertTrue(VersionComparator.sameBuild("3.0.1+26.3", "3.0.1", "26.3"));
        assertTrue(VersionComparator.sameBuild("5.3.2-fabric", "5.3.2", "26.3"));
        assertTrue(VersionComparator.sameBuild("0.165.0+26.3", "0.165.0+26.3", "26.3"));
        assertTrue(VersionComparator.sameBuild("8.72", "26.3.0-8.72", "26.3"));
    }

    @Test
    void aNewerBuildIsStillAnUpdate() {
        assertFalse(VersionComparator.sameBuild("8.69", "26.3.0-8.72", "26.3"));
        assertFalse(VersionComparator.sameBuild("5.3.2-26.3-fabric", "5.3.3-beta.1-26.3", "26.3"));
        assertFalse(VersionComparator.sameBuild("1.1.0", "26.3-1.1.1", "26.3"));
    }

    @Test
    void theUpdateListNoLongerOffersARenamedBuild() {
        LocalMod installed = new LocalMod(Path.of("mods", "appleskin.jar"), "aaaa", "appleskin", "3.0.10+mc26.3");
        ModVersion current = version("current", "3.0.10+mc26.3", "aaaa", ModSource.MODRINTH);
        ModVersion candidate = version("candidate", "26.3-3.0.10", null, ModSource.CURSEFORGE);

        Optional<ModUpdate> decided = Updates.decide(installed, current, candidate, "26.3",
                List.of("fabric"), "AppleSkin");

        assertTrue(decided.isEmpty());
    }

    private static ModVersion version(String id, String number, String sha1, ModSource source) {
        return new ModVersion(id, "project", number, "mod-" + number + ".jar", "https://example.invalid/mod.jar",
                sha1, List.of("26.3"), List.of("fabric"), List.of(), source);
    }
}
