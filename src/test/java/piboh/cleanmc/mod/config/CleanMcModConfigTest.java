package piboh.cleanmc.mod.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanMcModConfigTest {

    @TempDir
    Path configDir;

    @Test
    void defaultsAreSafeAndReadable() {
        CleanMcModConfig config = new CleanMcModConfig();

        assertTrue(config.includeLogs);
        assertTrue(config.includeServerResourcePacks);
        assertFalse(config.includeAssetCache, "the asset cache forces a re-download, so it is opt-in");
        assertFalse(config.dryRun);
        assertEquals(List.of(), config.extraPaths());
    }

    @Test
    void savesAndReloadsSettings() {
        Path file = configDir.resolve("cleanmcmod.json");
        CleanMcModConfig config = new CleanMcModConfig();
        config.includeAssetCache = true;
        config.dryRun = true;
        config.extraPaths = List.of("config/some-mod/cache");

        CleanMcModConfig.save(config, file);
        assertTrue(Files.isRegularFile(file));

        CleanMcModConfig loaded = CleanMcModConfig.load(file);

        assertTrue(loaded.includeAssetCache);
        assertTrue(loaded.dryRun);
        assertEquals(List.of("config/some-mod/cache"), loaded.extraPaths());
    }

    @Test
    void missingOrBrokenFilesFallBackToDefaults() throws Exception {
        Path missing = configDir.resolve("does-not-exist.json");
        assertTrue(CleanMcModConfig.load(missing).includeLogs);

        Path broken = configDir.resolve("broken.json");
        Files.writeString(broken, "{ not json");
        assertTrue(CleanMcModConfig.load(broken).includeLogs);
    }
}
