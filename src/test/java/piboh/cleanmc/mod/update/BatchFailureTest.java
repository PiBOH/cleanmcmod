package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A batch of downloads must not be all or nothing per mod: the jar the player asked for that cannot
 * be fetched fails alone, but a dependency it needs is different.
 */
class BatchFailureTest {

    private static ModUpdate update(Path local, String modId, ModSource source, String sha1) {
        LocalMod installed = new LocalMod(local, "old-sha1", modId, "1.0");
        ModVersion current = new ModVersion("project-" + modId, modId, "1.0",
                local.getFileName().toString(), "https://example.invalid/" + modId + "-1.0.jar", "old-sha1",
                List.of("1.21.8"), List.of("fabric"), List.of(), source);
        ModVersion candidate = new ModVersion("project-" + modId, modId, "1.1", modId + "-1.1.jar",
                "https://example.invalid/" + modId + "-1.1.jar", sha1, List.of("1.21.8"),
                List.of("fabric"), List.of(), source);
        return new ModUpdate(installed, current, candidate, modId.toUpperCase());
    }

    @Test
    void oneJarThatCannotBeVerifiedDoesNotBlockTheOthers() throws IOException {
        Path mods = Files.createDirectories(Files.createTempDirectory("cleanmcmod-batch").resolve("mods"));
        Files.writeString(mods.resolve("a-1.0.jar"), "a");
        Files.writeString(mods.resolve("b-1.0.jar"), "b");
        ModUpdate brokenJar = update(mods.resolve("a-1.0.jar"), "a", ModSource.MODRINTH, "0123456789abcdef");
        ModUpdate goodJar = update(mods.resolve("b-1.0.jar"), "b", ModSource.CURSEFORGE, null);

        List<ModInstaller.Outcome> outcomes = new ModInstaller(mods)
                .installTogether(List.of(brokenJar, goodJar), List.of(), (url, target) -> {
                    Files.writeString(target, "downloaded");
                });

        assertEquals(2, outcomes.size());
        assertFalse(outcomes.get(0).success());
        assertTrue(outcomes.get(0).detail().contains("a-1.1.jar"), outcomes.get(0).detail());
        assertTrue(outcomes.get(1).success(), outcomes.get(1).detail());
        assertFalse(Files.exists(mods.resolve("a-1.1.jar")));
        assertTrue(Files.exists(mods.resolve("b-1.1.jar")));
        assertTrue(Files.exists(mods.resolve("a-1.0.jar")));
        try (var entries = Files.list(mods)) {
            assertEquals(0, entries.filter(path -> path.getFileName().toString().endsWith(".tmp")).count());
        }
    }

    @Test
    void aMissingDependencyCancelsTheWholeBatch() throws IOException {
        Path mods = Files.createDirectories(Files.createTempDirectory("cleanmcmod-batch").resolve("mods"));
        Files.writeString(mods.resolve("a-1.0.jar"), "a");
        ModUpdate wanted = update(mods.resolve("a-1.0.jar"), "a", ModSource.MODRINTH, "0123456789abcdef");
        ModVersion dependency = new ModVersion("project-lib", "lib", "2.0", "lib-2.0.jar",
                "https://example.invalid/lib-2.0.jar", "fedcba9876543210", List.of("1.21.8"),
                List.of("fabric"), List.of(), ModSource.MODRINTH);

        List<ModInstaller.Outcome> outcomes = new ModInstaller(mods)
                .installTogether(List.of(wanted), List.of(dependency), (url, target) -> {
                    Files.writeString(target, url.contains("lib") ? "wrong library" : "downloaded");
                });

        assertEquals(1, outcomes.size());
        assertFalse(outcomes.get(0).success());
        assertTrue(outcomes.get(0).detail().contains("lib-2.0.jar"), outcomes.get(0).detail());
        assertFalse(Files.exists(mods.resolve("a-1.1.jar")));
        assertFalse(Files.exists(mods.resolve("lib-2.0.jar")));
    }
}
