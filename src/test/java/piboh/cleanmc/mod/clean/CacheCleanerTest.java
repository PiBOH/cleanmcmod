package piboh.cleanmc.mod.clean;

import piboh.cleanmc.mod.config.CleanMcModConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheCleanerTest {

    @TempDir
    Path gameDir;

    private Path write(String relative, String content) throws IOException {
        Path file = gameDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    private List<CacheTarget> targets(CleanMcModConfig config) {
        return CacheTargets.forConfig(gameDir, config, new CacheAutoDetector());
    }

    private TargetResult result(CleanReport report, String id) {
        return report.results().stream()
                .filter(candidate -> candidate.targetId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no result for " + id));
    }

    @Test
    void cleansGameLoaderAndServerPackCachesButKeepsUserData() throws IOException {
        write("logs/latest.log", "0123456789");
        write("logs/2026-01-01-1.log.gz", "0123456789");
        write(".fabric/remappedJars/sodium.jar", "0123456789");
        write(".mixin.out/class/audit.txt", "0123456789");
        write("server-resource-packs/server-pack.zip", "0123456789");
        write("saves/world/level.dat", "user data");
        write("resourcepacks/my-pack.zip", "user data");
        write("assets/objects/ab/cdef", "asset");

        CleanReport report = new CacheCleaner(gameDir).clean(targets(new CleanMcModConfig()), false);

        assertFalse(Files.exists(gameDir.resolve("logs/latest.log")));
        assertFalse(Files.exists(gameDir.resolve("logs/2026-01-01-1.log.gz")));
        assertFalse(Files.exists(gameDir.resolve(".fabric/remappedJars/sodium.jar")));
        assertFalse(Files.exists(gameDir.resolve("server-resource-packs/server-pack.zip")));
        assertTrue(Files.exists(gameDir.resolve("saves/world/level.dat")), "saves must survive");
        assertTrue(Files.exists(gameDir.resolve("resourcepacks/my-pack.zip")), "resource packs must survive");
        assertTrue(Files.exists(gameDir.resolve("assets/objects/ab/cdef")), "asset cache is opt-in");

        assertEquals(TargetStatus.CLEANED, result(report, CacheTargets.VANILLA_LOGS).status());
        assertEquals(TargetStatus.CLEANED, result(report, CacheTargets.FABRIC_REMAPPED_JARS).status());
        assertEquals(TargetStatus.CLEANED, result(report, CacheTargets.SERVER_RESOURCE_PACKS).status());
        assertEquals(TargetStatus.MISSING, result(report, CacheTargets.CRASH_REPORTS).status());
        assertTrue(report.filesDeleted() >= 5);
        assertTrue(report.bytesFreed() > 0);
        assertTrue(Files.isDirectory(gameDir.resolve("logs")), "the target folder itself is kept");
    }

    @Test
    void deepCleanRemovesTheAssetCacheWhenEnabled() throws IOException {
        write("assets/objects/ab/cdef", "asset");
        write("assets/indexes/24.json", "index");

        CleanMcModConfig config = new CleanMcModConfig();
        config.includeAssetCache = true;

        CleanReport report = new CacheCleaner(gameDir).clean(targets(config), false);

        assertFalse(Files.exists(gameDir.resolve("assets/objects/ab/cdef")));
        assertFalse(Files.exists(gameDir.resolve("assets/indexes/24.json")));
        assertEquals(TargetStatus.CLEANED, result(report, CacheTargets.ASSET_OBJECTS).status());
    }

    @Test
    void dryRunCountsEverythingAndDeletesNothing() throws IOException {
        write("logs/latest.log", "0123456789");
        write(".fabric/remappedJars/mod.jar", "0123456789");

        CleanReport report = new CacheCleaner(gameDir).clean(targets(new CleanMcModConfig()), true);

        assertTrue(Files.exists(gameDir.resolve("logs/latest.log")));
        assertTrue(Files.exists(gameDir.resolve(".fabric/remappedJars/mod.jar")));
        assertTrue(report.dryRun());
        TargetResult logs = result(report, CacheTargets.VANILLA_LOGS);
        assertEquals(TargetStatus.DRY_RUN, logs.status());
        assertTrue(logs.planned().files() >= 1, "the dry run still reports what it would remove");
        assertTrue(logs.planned().bytes() > 0);
        assertEquals(0, logs.deleted().files(), "a dry run deletes nothing");
        assertEquals(0, report.filesDeleted());
        assertEquals(0L, report.bytesFreed());
        assertTrue(report.filesPlanned() >= 2, "both targets are listed as planned removals");
        assertTrue(report.bytesPlanned() > 0);
        assertTrue(report.summary().contains("would"), "the summary says would, not freed: " + report.summary());
    }

    @Test
    void refusesTargetsOutsideTheGameDirectory() throws IOException {
        Path outside = gameDir.getParent().resolve("not-a-cache.txt");
        Files.writeString(outside, "keep me");

        CacheTarget target = CacheTarget.builtin("outside", CacheCategory.CUSTOM, CacheSafety.SAFE, outside);
        CleanReport report = new CacheCleaner(gameDir).clean(List.of(target), false);

        assertEquals(TargetStatus.FAILED, report.results().get(0).status());
        assertTrue(Files.exists(outside), "a path outside the game directory must never be deleted");
    }

    @Test
    void refusesTheGameDirectoryItself() {
        CacheTarget target = CacheTarget.builtin("root", CacheCategory.CUSTOM, CacheSafety.SAFE, gameDir);
        CleanReport report = new CacheCleaner(gameDir).clean(List.of(target), false);

        assertEquals(TargetStatus.FAILED, report.results().get(0).status());
        assertTrue(Files.isDirectory(gameDir));
    }

    @Test
    void autoDetectsModCachesAndSkipsUserDataFolders() throws IOException {
        write("config/some-mod/cache/shader.bin", "0123456789");
        write("saves/world/cache/level-cache.bin", "user data");
        write("shaderpacks/cache/program.bin", "user data");
        write("resourcepacks/cache/atlas.bin", "user data");
        write("mods/some-mod.jar", "jar");

        List<CacheTarget> detected = new CacheAutoDetector().detect(gameDir);

        assertTrue(detected.stream().anyMatch(t -> t.path().equals(gameDir.resolve("config/some-mod/cache"))),
                "config caches are detected: " + detected);
        assertTrue(detected.stream().noneMatch(t -> t.path().startsWith(gameDir.resolve("saves"))));
        assertTrue(detected.stream().noneMatch(t -> t.path().startsWith(gameDir.resolve("shaderpacks"))));
        assertTrue(detected.stream().noneMatch(t -> t.path().startsWith(gameDir.resolve("resourcepacks"))));

        CleanReport report = new CacheCleaner(gameDir).clean(targets(new CleanMcModConfig()), false);

        assertFalse(Files.exists(gameDir.resolve("config/some-mod/cache/shader.bin")));
        assertTrue(Files.exists(gameDir.resolve("saves/world/cache/level-cache.bin")));
        assertTrue(Files.exists(gameDir.resolve("shaderpacks/cache/program.bin")));
        assertEquals(1, detected.size(), "only the mod cache folder is a target here: " + detected);
        assertEquals(TargetStatus.CLEANED, result(report, detected.get(0).id()).status());
    }

    @Test
    void customExtraPathsAreCleanedAndEscapesAreIgnored() throws IOException {
        write("config/custom-tmp/blob.bin", "0123456789");
        Path outside = gameDir.getParent().resolve("escape.bin");
        Files.writeString(outside, "keep me");

        CleanMcModConfig config = new CleanMcModConfig();
        config.autoDetect = false;
        config.extraPaths = List.of("config/custom-tmp", "../escape.bin");

        List<CacheTarget> targets = targets(config);

        assertTrue(targets.stream().anyMatch(t -> t.path().equals(gameDir.resolve("config/custom-tmp"))));
        assertTrue(targets.stream().noneMatch(t -> t.path().startsWith(gameDir.getParent().resolve("escape.bin"))));
        assertTrue(CacheTargets.resolveExtra(gameDir, "../escape.bin") == null);

        new CacheCleaner(gameDir).clean(targets, false);
        assertFalse(Files.exists(gameDir.resolve("config/custom-tmp/blob.bin")));
        assertTrue(Files.exists(outside));
    }

    @Test
    void findsCacheFoldersAtAnyDepthUnderConfig() throws IOException {
        write("config/mod/cache/a.bin", "0123456789");
        write("config/mod/a/b/cache/deep.bin", "0123456789");
        write("config/mod/.cache/deepest.bin", "0123456789");

        List<CacheTarget> detected = new CacheAutoDetector().detect(gameDir);

        assertTrue(detected.stream().anyMatch(t -> t.path().equals(gameDir.resolve("config/mod/cache"))), detected.toString());
        assertTrue(detected.stream().anyMatch(t -> t.path().equals(gameDir.resolve("config/mod/a/b/cache"))), detected.toString());
        assertTrue(detected.stream().anyMatch(t -> t.path().equals(gameDir.resolve("config/mod/.cache"))), detected.toString());
        assertEquals(3, detected.size(), "one target per cache folder, no duplicates: " + detected);

        new CacheCleaner(gameDir).clean(targets(new CleanMcModConfig()), false);
        assertFalse(Files.exists(gameDir.resolve("config/mod/a/b/cache/deep.bin")));
        assertFalse(Files.exists(gameDir.resolve("config/mod/.cache/deepest.bin")));
    }

    @Test
    void detectsCacheFoldersAtTheDepthLimitOutsideConfig() throws IOException {
        write("data/a/b/cache/blob.bin", "0123456789");
        write("data/a/b/not-a-cache/blob.bin", "keep me");

        List<CacheTarget> detected = new CacheAutoDetector().detect(gameDir);
        assertTrue(detected.stream().anyMatch(t -> t.path().equals(gameDir.resolve("data/a/b/cache"))),
                "a directory at the depth limit is a directory, not a file: " + detected);

        new CacheCleaner(gameDir).clean(targets(new CleanMcModConfig()), false);
        assertFalse(Files.exists(gameDir.resolve("data/a/b/cache/blob.bin")));
        assertTrue(Files.exists(gameDir.resolve("data/a/b/not-a-cache/blob.bin")));
    }

    @Test
    void emptyOrUnreadableGameDirectoryIsNotAnError() {
        assertTrue(new CacheAutoDetector().detect(gameDir).isEmpty());
        assertTrue(new CacheAutoDetector().detect(gameDir.resolve("missing")).isEmpty());
    }

    @Test
    void neverDetectsTheGameDirectoryItself() throws IOException {
        Path cacheRoot = Files.createDirectories(gameDir.resolve("cache"));
        Files.writeString(cacheRoot.resolve("inner.bin"), "0123456789");

        assertTrue(new CacheAutoDetector().detect(cacheRoot).isEmpty(),
                "the game directory must never become a target of its own cleaner");
    }

    @Test
    void scanReportsSizesWithoutDeleting() throws IOException {
        write("logs/latest.log", "0123456789");

        CacheScan scan = new CacheScanner().scanAll(targets(new CleanMcModConfig()));
        TargetState logs = scan.targets().stream()
                .filter(state -> state.target().id().equals(CacheTargets.VANILLA_LOGS))
                .findFirst()
                .orElseThrow();

        assertTrue(logs.present());
        assertEquals(1, logs.files());
        assertEquals(10L, logs.bytes());
        assertNotNull(scan.presentTargets());
        assertTrue(Files.exists(gameDir.resolve("logs/latest.log")));
        assertEquals("1.0 KiB", CleanReport.formatBytes(1024));
    }
}
