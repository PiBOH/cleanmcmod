package piboh.cleanmc.mod.clean;

import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Finds the render caches of the two big minimap mods, VoxelMap and Xaero's Minimap/World Map.
 *
 * <p>Both mods keep two very different kinds of data on disk:</p>
 * <ul>
 *   <li>the map itself and the waypoints — the player's work, which this cleaner must never
 *       touch ({@code voxelmap/<world>}, {@code *.points}, {@code waypoints.txt},
 *       {@code config/xaero/...} region folders);</li>
 *   <li>caches the mod rebuilds from the map on its own — the region/chunk cache folders that
 *       go stale after a texture-pack or mod change and then draw mobs and blocks with the wrong
 *       sprites until the player deletes them by hand.</li>
 * </ul>
 *
 * <p>Only folders named {@code cache}/{@code caches}/{@code *cache} inside a known map-mod root
 * are offered, so a waypoint folder can never be matched by accident. When a mod is not installed
 * nothing is found and nothing is offered. Symbols links are never followed.</p>
 */
public final class MapCaches {

    /** Folders owned by VoxelMap, relative to the game directory. */
    private static final List<String> VOXELMAP_ROOTS = List.of("voxelmap");

    /** Folders owned by Xaero's Minimap and World Map, old and current layouts. */
    private static final List<String> XAERO_ROOTS = List.of(
            "config/xaero", "xaero", "XaeroWaypoints", "XaeroWorldMap", "XaeroMinimap");

    private static final Set<String> CACHE_NAMES = Set.of(
            "cache", "caches", ".cache", "texturecache", "texture-cache", "imgcache", "imagecache");

    /** Depth limit inside a map-mod root; the deepest real layout is {@code config/xaero/<mod>/<world>/cache}. */
    private static final int MAX_DEPTH = 5;

    private MapCaches() {
    }

    /** Every cache folder of an installed minimap mod, as a cleanable target. */
    public static List<CacheTarget> detect(Path gameDir) {
        Path root = gameDir.toAbsolutePath().normalize();
        List<CacheTarget> found = new ArrayList<>();
        collect(root, root, VOXELMAP_ROOTS, "voxelmap", "VoxelMap", found);
        collect(root, root, XAERO_ROOTS, "xaero", "Xaero", found);
        return List.copyOf(found);
    }

    private static void collect(Path root, Path dir, List<String> relatives, String idPrefix,
            String label, List<CacheTarget> found) {
        for (String relative : relatives) {
            Path start = root.resolve(relative);
            if (!Files.isDirectory(start)) {
                continue;
            }
            // config/xaero holds settings; its caches live deeper, under a world folder.
            walk(root, start, idPrefix, label, found);
        }
    }

    private static void walk(Path root, Path start, String idPrefix, String label, List<CacheTarget> found) {
        try {
            Files.walkFileTree(start, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(start)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String name = dir.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (CACHE_NAMES.contains(name)) {
                        found.add(target(root, dir, idPrefix, label));
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (start.relativize(dir).getNameCount() >= MAX_DEPTH) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            CleanMcMod.LOGGER.debug("Could not inspect {}: {}", start, e.toString());
        }
    }

    private static CacheTarget target(Path root, Path path, String idPrefix, String label) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        return CacheTarget.detected(
                idPrefix + "_map_cache_" + relative.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_"),
                CacheCategory.MAP,
                CacheSafety.SAFE,
                path,
                label + " cache: " + relative);
    }
}
