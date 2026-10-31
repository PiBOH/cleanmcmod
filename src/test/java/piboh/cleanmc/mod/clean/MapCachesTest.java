package piboh.cleanmc.mod.clean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapCachesTest {

    @TempDir
    Path gameDir;

    private List<String> relatives() {
        return MapCaches.detect(gameDir).stream()
                .map(target -> target.relativePath(gameDir))
                .toList();
    }

    @Test
    void findsTheCachesOfBothMapMods() throws IOException {
        Files.createDirectories(gameDir.resolve("voxelmap/cache/some-world"));
        Files.createDirectories(gameDir.resolve("config/xaero/world-map/MyWorld/cache"));

        List<String> found = relatives();

        assertTrue(found.contains("voxelmap/cache"), found.toString());
        assertTrue(found.stream().anyMatch(path -> path.startsWith("config/xaero")
                && path.endsWith("cache")), found.toString());
    }

    @Test
    void neverOffersWaypointsOrSettings() throws IOException {
        Files.createDirectories(gameDir.resolve("voxelmap/waypoints"));
        Files.createDirectories(gameDir.resolve("XaeroWaypoints/Multiplayer_server"));
        Files.createDirectories(gameDir.resolve("config/xaero/minimap"));

        assertTrue(relatives().isEmpty(), relatives().toString());
    }

    @Test
    void neverLeavesTheMapModFolders() throws IOException {
        Files.createDirectories(gameDir.resolve("cache"));
        Files.createDirectories(gameDir.resolve("logs/cache"));

        assertTrue(relatives().isEmpty(), relatives().toString());
    }

    @Test
    void producesAMapTargetThatTheCleanerMayDelete() throws IOException {
        Files.createDirectories(gameDir.resolve("voxelmap/cache/world"));
        Files.writeString(gameDir.resolve("voxelmap/cache/world/region.zip"), "x");

        CacheTarget target = MapCaches.detect(gameDir).get(0);

        assertEquals(CacheCategory.MAP, target.category());
        assertEquals(CacheSafety.SAFE, target.safety());
        assertTrue(target.label().contains("voxelmap/cache"), target.label());
        assertFalse(target.translationKey().contains(" "));
    }

    @Test
    void missesNothingWhenNoMapModIsInstalled() {
        assertTrue(MapCaches.detect(gameDir).isEmpty());
    }
}
