package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The dependency pre-flight, checked against payloads captured from the Modrinth API. */
class CompatibilityTest {

    /** Captured 2026-10-25 from /v2/project/iris/version?game_versions=["1.21.8"]&loaders=["fabric"]. */
    private static final String IRIS_VERSION = """
            {
              "id": "Rhzf61g1",
              "project_id": "YL57xq9U",
              "version_number": "1.9.6+1.21.8-fabric",
              "game_versions": ["1.21.6", "1.21.7", "1.21.8"],
              "loaders": ["fabric"],
              "dependencies": [
                {
                  "version_id": "pJRT8Zxt",
                  "project_id": "AANobbMI",
                  "file_name": null,
                  "dependency_type": "required"
                }
              ],
              "files": [
                {
                  "primary": true,
                  "filename": "iris-fabric-1.9.6+mc1.21.8.jar",
                  "url": "https://cdn.modrinth.com/data/YL57xq9U/versions/Rhzf61g1/iris-fabric-1.9.6%2Bmc1.21.8.jar",
                  "hashes": {"sha1": "70fc97aa5eef2df2085a078ecb17d6bfce6756dd"}
                }
              ]
            }
            """;

    private static final String SODIUM_PROJECT = "AANobbMI";
    private static final String FABRIC_API_PROJECT = "P7dR8mSH";
    private static final Map<String, String> TITLES =
            Map.of(SODIUM_PROJECT, "Sodium", "YL57xq9U", "Iris Shaders");

    private static ModVersion iris() {
        Map<String, ModVersion> parsed = ModrinthClient.parseVersions(
                "{\"70fc97aa5eef2df2085a078ecb17d6bfce6756dd\": " + IRIS_VERSION + "}");
        return parsed.get("70fc97aa5eef2df2085a078ecb17d6bfce6756dd");
    }

    private static LocalMod jar(String modId, String version, Map<String, String> depends) {
        return new LocalMod(Path.of("/tmp/mods/" + modId + ".jar"), "sha-" + modId, modId, version, depends);
    }

    @Test
    void readsRequiredDependencyFromCapturedPayload() {
        assertTrue(iris().dependencies().contains(new Dependency(SODIUM_PROJECT, "pJRT8Zxt", "required")));
    }

    @Test
    void blocksWhenRequiredDependencyIsNotInstalled() {
        LocalMod irisJar = jar("iris", "1.9.5+1.21.8-fabric", Map.of());
        Compatibility.Verdict verdict = Compatibility.check(irisJar, iris(), List.of(irisJar), Map.of(),
                Map.of("iris", "YL57xq9U"), TITLES);

        assertEquals(Compatibility.Status.BLOCK, verdict.status());
        assertTrue(verdict.reason().contains("Sodium"), verdict.reason());
        assertTrue(verdict.reason().contains("not in mods/"), verdict.reason());
    }

    @Test
    void allowsInstallingWhenRequiredDependencyIsPresent() {
        LocalMod irisJar = jar("iris", "1.9.5+1.21.8-fabric", Map.of());
        LocalMod sodiumJar = jar("sodium", "0.6.5+mc1.21.8", Map.of());

        Compatibility.Verdict verdict = Compatibility.check(irisJar, iris(), List.of(irisJar, sodiumJar), Map.of(),
                Map.of("iris", "YL57xq9U", "sodium", SODIUM_PROJECT), TITLES);

        assertEquals(Compatibility.Status.OK, verdict.status());
        assertFalse(verdict.blocks());
    }

    @Test
    void doesNotCountLoaderProvidedDependenciesAsMissing() {
        ModVersion candidate = new ModVersion("v1", "YL57xq9U", "1.9.6", "iris.jar", "https://example.invalid",
                "sha", List.of("1.21.8"), List.of("fabric"),
                List.of(new Dependency(FABRIC_API_PROJECT, null, "required")));
        LocalMod irisJar = jar("iris", "1.9.5", Map.of());

        Compatibility.Verdict verdict = Compatibility.check(irisJar, candidate, List.of(irisJar), Map.of(),
                Map.of("iris", "YL57xq9U"), TITLES);

        assertEquals(Compatibility.Status.OK, verdict.status());
    }

    @Test
    void blocksWhenAnotherModsVersionRangeWouldBreak() {
        LocalMod sodiumJar = jar("sodium", "0.6.5+mc1.21.8", Map.of());
        LocalMod extraJar = jar("sodium-extra", "0.6.0", Map.of("sodium", ">=0.6.0 <0.7.0"));
        ModVersion candidate = new ModVersion("v2", SODIUM_PROJECT, "0.7.0+mc1.21.8", "sodium.jar",
                "https://example.invalid", "sha", List.of("1.21.8"), List.of("fabric"));

        Compatibility.Verdict verdict = Compatibility.check(sodiumJar, candidate, List.of(sodiumJar, extraJar), Map.of(),
                Map.of("sodium", SODIUM_PROJECT), TITLES);

        assertEquals(Compatibility.Status.BLOCK, verdict.status());
        assertTrue(verdict.reason().contains("sodium-extra"), verdict.reason());
        assertTrue(verdict.reason().contains("0.7.0"), verdict.reason());
    }

    @Test
    void allowsAnUpdateInsideTheDeclaredRange() {
        LocalMod sodiumJar = jar("sodium", "0.6.5+mc1.21.8", Map.of());
        LocalMod extraJar = jar("sodium-extra", "0.6.0", Map.of("sodium", ">=0.6.0 <0.7.0"));
        ModVersion candidate = new ModVersion("v3", SODIUM_PROJECT, "0.6.9", "sodium.jar",
                "https://example.invalid", "sha", List.of("1.21.8"), List.of("fabric"));

        Compatibility.Verdict verdict = Compatibility.check(sodiumJar, candidate, List.of(sodiumJar, extraJar), Map.of(),
                Map.of("sodium", SODIUM_PROJECT), TITLES);

        assertEquals(Compatibility.Status.OK, verdict.status());
    }

    @Test
    void blocksIncompatiblePair() {
        LocalMod sodiumJar = jar("sodium", "0.6.5", Map.of());
        LocalMod updatedJar = jar("some-renderer", "1.0", Map.of());
        ModVersion candidate = new ModVersion("v4", "SOMERENDERER", "2.0", "renderer.jar",
                "https://example.invalid", "sha", List.of("1.21.8"), List.of("fabric"),
                List.of(new Dependency(SODIUM_PROJECT, null, "incompatible")));

        Compatibility.Verdict verdict = Compatibility.check(updatedJar, candidate,
                List.of(updatedJar, sodiumJar), Map.of(), Map.of("sodium", SODIUM_PROJECT), TITLES);

        assertEquals(Compatibility.Status.BLOCK, verdict.status());
        assertTrue(verdict.reason().contains("incompatible"), verdict.reason());
    }

    @Test
    void warnsWhenAnotherModsRangeCannotBeRead() {
        LocalMod sodiumJar = jar("sodium", "0.6.5", Map.of());
        LocalMod otherJar = jar("other", "1.0", Map.of("sodium", "weird-syntax"));
        ModVersion candidate = new ModVersion("v5", SODIUM_PROJECT, "0.7.0", "sodium.jar",
                "https://example.invalid", "sha", List.of("1.21.8"), List.of("fabric"));

        Compatibility.Verdict verdict = Compatibility.check(sodiumJar, candidate,
                List.of(sodiumJar, otherJar), Map.of(), Map.of("sodium", SODIUM_PROJECT), TITLES);

        assertEquals(Compatibility.Status.WARN, verdict.status());
        assertFalse(verdict.blocks());
    }

    @Test
    void readsTheVersionPredicatesModsActuallyUse() {
        assertEquals(Boolean.TRUE, VersionRange.satisfies("0.6.5", ">=0.6.0 <0.7.0"));
        assertEquals(Boolean.FALSE, VersionRange.satisfies("0.7.0", ">=0.6.0 <0.7.0"));
        assertEquals(Boolean.TRUE, VersionRange.satisfies("1.2.9", "~1.2.3"));
        assertEquals(Boolean.FALSE, VersionRange.satisfies("1.3.0", "~1.2.3"));
        assertEquals(Boolean.TRUE, VersionRange.satisfies("1.9.6", "^1.2.3"));
        assertEquals(Boolean.TRUE, VersionRange.satisfies("0.136.1", "0.136.x"));
        assertEquals(Boolean.TRUE, VersionRange.satisfies("9.9.9", "*"));
        assertEquals(null, VersionRange.satisfies("1.0", "weird-syntax"));
    }

    /**
     * Iris 1.9.6 pins Sodium to one build (Modrinth {@code pJRT8Zxt}, 0.7.2). An older jar of the
     * same project must not pass the check just because the project id is installed.
     */
    @Test
    void blocksWhenTheInstalledDependencyIsADifferentBuild() {
        LocalMod irisJar = jar("iris", "1.9.5+1.21.8-fabric", Map.of());
        Path sodiumJar = Path.of("/tmp/mods/sodium-0.7.0.jar");
        LocalMod sodium = new LocalMod(sodiumJar, "sha-sodium-old", "sodium", "0.7.0+mc1.21.8");
        ModVersion installed = new ModVersion("24jH02Sf", SODIUM_PROJECT, "mc1.21.8-0.7.0-fabric",
                "sodium.jar", "https://example.invalid/sodium", "sha-sodium-old",
                List.of("1.21.8"), List.of("fabric"));

        Compatibility.Verdict verdict = Compatibility.check(irisJar, iris(), List.of(irisJar, sodium),
                Map.of("sha-sodium-old", installed),
                Map.of("iris", "YL57xq9U", "sodium", SODIUM_PROJECT), TITLES);

        assertEquals(Compatibility.Status.BLOCK, verdict.status());
        assertTrue(verdict.reason().contains("mc1.21.8-0.7.0-fabric"), verdict.reason());
        assertEquals(List.of(SODIUM_PROJECT), verdict.missing());
        assertEquals(Map.of(SODIUM_PROJECT, sodiumJar), verdict.outdated());
    }

    @Test
    void allowsTheBuildTheDependencyPins() {
        LocalMod irisJar = jar("iris", "1.9.5+1.21.8-fabric", Map.of());
        LocalMod sodium = new LocalMod(Path.of("/tmp/mods/sodium-0.7.2.jar"), "sha-sodium-right",
                "sodium", "0.7.2+mc1.21.8");
        ModVersion installed = new ModVersion("pJRT8Zxt", SODIUM_PROJECT, "mc1.21.8-0.7.2-fabric",
                "sodium.jar", "https://example.invalid/sodium", "sha-sodium-right",
                List.of("1.21.8"), List.of("fabric"));

        Compatibility.Verdict verdict = Compatibility.check(irisJar, iris(), List.of(irisJar, sodium),
                Map.of("sha-sodium-right", installed),
                Map.of("iris", "YL57xq9U", "sodium", SODIUM_PROJECT), TITLES);

        assertEquals(Compatibility.Status.OK, verdict.status());
        assertTrue(verdict.outdated().isEmpty());
    }

    /** A jar Modrinth does not know cannot be version-checked, and guessing there would block wrongly. */
    @Test
    void anInstalledDependencyOfUnknownBuildIsOnlyCheckedForPresence() {
        LocalMod irisJar = jar("iris", "1.9.5+1.21.8-fabric", Map.of());
        LocalMod sodium = new LocalMod(Path.of("/tmp/mods/sodium-local.jar"), "sha-unknown", "sodium", "dev");

        Compatibility.Verdict verdict = Compatibility.check(irisJar, iris(), List.of(irisJar, sodium),
                Map.of(), Map.of("iris", "YL57xq9U", "sodium", SODIUM_PROJECT), TITLES);

        assertEquals(Compatibility.Status.OK, verdict.status());
    }
}
