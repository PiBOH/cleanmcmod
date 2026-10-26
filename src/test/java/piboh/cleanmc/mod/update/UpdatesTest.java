package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdatesTest {

    private static final String GAME_VERSION = "1.21.8";
    private static final List<String> LOADERS = List.of("fabric", "quilt");

    private static ModVersion version(String id, String versionNumber) {
        return new ModVersion(id, "project", versionNumber, "file-" + id + ".jar",
                "https://example.invalid/file.jar", "sha1", List.of(GAME_VERSION), List.of("fabric"));
    }

    private static LocalMod local(String version) {
        return new LocalMod(Path.of("/game/mods/sodium.jar"), "hash", "sodium", version);
    }

    private static Optional<ModUpdate> decide(ModVersion current, ModVersion candidate) {
        return Updates.decide(local("0.5.9"), current, candidate, GAME_VERSION, LOADERS, "Sodium");
    }

    @Test
    void newerVersionIsAnUpdate() {
        Optional<ModUpdate> update = decide(version("v1", "0.5.9"), version("v2", "0.6.0"));

        assertTrue(update.isPresent());
        assertEquals("Sodium", update.get().displayName());
        assertEquals("0.5.9", update.get().installedVersion());
        assertEquals("0.6.0", update.get().availableVersion());
    }

    @Test
    void sameVersionIdIsNotAnUpdate() {
        assertTrue(decide(version("v1", "0.5.9"), version("v1", "0.5.9")).isEmpty());
    }

    @Test
    void olderVersionIsNotOffered() {
        assertTrue(decide(version("v2", "0.6.0"), version("v1", "0.5.9")).isEmpty());
    }

    @Test
    void wrongGameVersionIsRejected() {
        ModVersion candidate = new ModVersion("v2", "project", "0.6.0", "file.jar", "https://example.invalid/f",
                "sha1", List.of("1.21.4"), List.of("fabric"));
        assertTrue(decide(version("v1", "0.5.9"), candidate).isEmpty());
    }

    @Test
    void wrongLoaderIsRejected() {
        ModVersion candidate = new ModVersion("v2", "project", "0.6.0", "file.jar", "https://example.invalid/f",
                "sha1", List.of(GAME_VERSION), List.of("forge"));
        assertTrue(decide(version("v1", "0.5.9"), candidate).isEmpty());
    }

    @Test
    void rebuildOfTheSameVersionIsOffered() {
        assertTrue(decide(version("v1", "0.5.9"), version("v2", "0.5.9")).isPresent());
    }

    @Test
    void unknownProjectIsSkipped() {
        assertTrue(decide(null, version("v2", "0.6.0")).isEmpty());
        assertTrue(decide(version("v1", "0.5.9"), null).isEmpty());
    }

    @Test
    void titleFallsBackToTheFileName() {
        Optional<ModUpdate> update = Updates.decide(local("0.5.9"), version("v1", "0.5.9"),
                version("v2", "0.6.0"), GAME_VERSION, LOADERS, null);
        assertTrue(update.isPresent());
        assertEquals("sodium.jar", update.get().displayName());
    }

    @Test
    void metadataVersionWinsOverTheModrinthVersionNumber() {
        Optional<ModUpdate> update = Updates.decide(local("0.5.9+1.21.8"), version("v1", "0.5.9"),
                version("v2", "0.6.0"), GAME_VERSION, LOADERS, "Sodium");
        assertTrue(update.isPresent());
        assertEquals("0.5.9+1.21.8", update.get().installedVersion());
    }

    @Test
    void planPairsHashesWithModsAndTitles() {
        List<LocalMod> mods = List.of(local("0.5.9"));
        Map<String, ModVersion> current = Map.of("hash", version("v1", "0.5.9"));
        Map<String, ModVersion> latest = Map.of("hash", version("v2", "0.6.0"));

        List<ModUpdate> updates = Updates.plan(mods, current, latest, GAME_VERSION, LOADERS,
                Map.of("project", "Sodium"));

        assertEquals(1, updates.size());
        assertEquals("Sodium", updates.get(0).displayName());
    }
}
