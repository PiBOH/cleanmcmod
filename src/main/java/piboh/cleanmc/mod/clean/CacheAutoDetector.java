package piboh.cleanmc.mod.clean;

import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Finds caches that mods create without registering them anywhere.
 *
 * <p>Mods are free to write wherever they like, so the only portable rule is "a folder called
 * cache/caches/.cache, or a file ending in .cache, is a cache". User data folders are excluded,
 * because a mod's save data must never be deleted by a cache cleaner. Installed resource packs and
 * shader packs are excluded as well: they are user content, not caches.</p>
 *
 * <p>The walk is unbounded and prunes itself: directories are therefore always delivered to
 * {@link SimpleFileVisitor#preVisitDirectory}, never to {@code visitFile} as a "file" of unknown
 * type. That distinction used to hide the most common layout, {@code config/<mod>/cache}. Outside
 * {@code config/} the walk stops at {@link #MAX_DEPTH} levels, which keeps a full game directory
 * cheap to scan; {@code config/} is small and is walked to any depth, because that is where mods
 * nest their caches.</p>
 */
public final class CacheAutoDetector {

    private static final Set<String> CACHE_DIR_NAMES = Set.of("cache", "caches", ".cache");
    private static final Set<String> EXCLUDED_DIRS = Set.of(
            "saves", "resourcepacks", "shaderpacks", "mods", "libraries", "versions",
            "assets", "screenshots", "backups", ".git", ".freebuff", ".ignore");
    private static final String CONFIG_DIR = "config";
    private static final int MAX_DEPTH = 4;
    private static final int MAX_TARGETS = 24;

    public List<CacheTarget> detect(Path gameDir) {
        Path root = gameDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<CacheTarget> found = new ArrayList<>();
        try {
            Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(root)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (found.size() >= MAX_TARGETS) {
                        return FileVisitResult.TERMINATE;
                    }
                    Path relative = root.relativize(dir);
                    if (isExcluded(relative)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (CACHE_DIR_NAMES.contains(name(dir))) {
                        found.add(detected(root, dir, relative, "cache folder"));
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!isUnderConfig(relative) && relative.getNameCount() >= MAX_DEPTH) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!attrs.isRegularFile() || found.size() >= MAX_TARGETS) {
                        return FileVisitResult.CONTINUE;
                    }
                    Path relative = root.relativize(file);
                    if (isExcluded(relative)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (name(file).endsWith(".cache")) {
                        found.add(detected(root, file, relative, "cache file"));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            CleanMcMod.LOGGER.debug("Cache auto-detection stopped early: {}", e.toString());
        }
        return List.copyOf(found);
    }

    private static CacheTarget detected(Path root, Path path, Path relative, String kind) {
        return CacheTarget.detected(
                idFor(relative),
                CacheCategory.MOD,
                CacheSafety.SAFE,
                path,
                kind + ": " + relative.toString().replace('\\', '/'));
    }

    private static String name(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT);
    }

    private static boolean isUnderConfig(Path relative) {
        Path first = relative.getName(0);
        return first != null && CONFIG_DIR.equalsIgnoreCase(first.toString());
    }

    private static boolean isExcluded(Path relative) {
        for (Path part : relative) {
            if (EXCLUDED_DIRS.contains(part.toString().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    static String idFor(Path relative) {
        String text = relative.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        return "detected_" + text.replaceAll("[^a-z0-9]+", "_");
    }
}
