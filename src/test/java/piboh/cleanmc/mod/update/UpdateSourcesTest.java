package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateSourcesTest {

    private static final List<LocalMod> MODS = List.of(
            new LocalMod(Path.of("/game/mods/a.jar"), "sha-a", "a", "1.0"));

    private static ModUpdate update(String version, ModSource source) {
        LocalMod local = MODS.get(0);
        ModVersion current = new ModVersion("current", "a", "1.0", "a.jar",
                "https://example.invalid/a-1.0.jar", "sha-current", List.of("1.21.8"), List.of("fabric"));
        ModVersion candidate = new ModVersion("candidate-" + version + "-" + source, "a", version,
                "a-" + version + ".jar", "https://example.invalid/a-" + version + ".jar",
                source.publishesChecksum() ? "sha-" + version : null, List.of("1.21.8"),
                List.of("fabric"), List.of(), source);
        return new ModUpdate(local, current, candidate, "A");
    }

    private static UpdateSources.Source source(ModSource id, List<ModUpdate> updates) {
        return new UpdateSources.Source(id, mods -> updates);
    }

    @Test
    void keepsTheHighestVersionAcrossSources() throws Exception {
        UpdateSources sources = new UpdateSources(List.of(
                source(ModSource.MODRINTH, List.of(update("1.1", ModSource.MODRINTH))),
                source(ModSource.GITHUB, List.of(update("1.2", ModSource.GITHUB)))));

        List<ModUpdate> found = sources.check(MODS);

        assertEquals(1, found.size());
        assertEquals("1.2", found.get(0).availableVersion());
        assertEquals(ModSource.GITHUB, found.get(0).candidate().source());
    }

    @Test
    void prefersTheSourceThatPublishesAChecksumAtTheSameVersion() throws Exception {
        UpdateSources sources = new UpdateSources(List.of(
                source(ModSource.CURSEFORGE, List.of(update("1.2", ModSource.CURSEFORGE))),
                source(ModSource.MODRINTH, List.of(update("1.2", ModSource.MODRINTH)))));

        List<ModUpdate> found = sources.check(MODS);

        assertEquals(ModSource.MODRINTH, found.get(0).candidate().source());
        assertTrue(found.get(0).candidate().checksummed());
    }

    @Test
    void oneFailingSourceDoesNotHideTheOthers() throws Exception {
        UpdateSources sources = new UpdateSources(List.of(
                source(ModSource.MODRINTH, List.of(update("1.1", ModSource.MODRINTH))),
                new UpdateSources.Source(ModSource.GITHUB, mods -> {
                    throw new IOException("rate limited");
                })));

        List<ModUpdate> found = sources.check(MODS);

        assertEquals(1, found.size());
        assertEquals(1, sources.failures().size());
        assertEquals(ModSource.GITHUB, sources.failures().get(0).source());
    }

    @Test
    void everySourceFailingFailsTheCheck() {
        UpdateSources sources = new UpdateSources(List.of(
                new UpdateSources.Source(ModSource.MODRINTH, mods -> {
                    throw new IOException("Modrinth down");
                }),
                new UpdateSources.Source(ModSource.GITHUB, mods -> {
                    throw new IOException("GitHub down");
                })));

        IOException error = assertThrows(IOException.class, () -> sources.check(MODS));
        assertTrue(error.getMessage().contains("down"), error.getMessage());
    }

    @Test
    void anOlderBuildIsNeverOffered() throws Exception {
        UpdateSources sources = new UpdateSources(List.of(
                source(ModSource.CURSEFORGE, List.of())));

        assertTrue(sources.check(MODS).isEmpty());
    }
}
