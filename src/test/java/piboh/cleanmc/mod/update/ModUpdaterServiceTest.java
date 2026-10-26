package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ModUpdaterServiceTest {

    private static final byte[] NEW_BYTES = "updated-jar".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SODIUM_BYTES = "sodium-jar".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LIBRARY_BYTES = "library-jar".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path gameDir;

    private Path installedMod(String name) throws IOException {
        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Path jar = mods.resolve(name);
        Files.writeString(jar, "old");
        return jar;
    }

    private static String sha1(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest(bytes)) {
                hex.append(String.format(Locale.ROOT, "%02x", value));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ModUpdate update(Path old) {
        LocalMod local = new LocalMod(old, "old-sha1", "test", "1.0");
        ModVersion current = new ModVersion("v1", "project", "1.0", old.getFileName().toString(),
                "https://example.invalid/old", "old-sha1", List.of("1.21.8"), List.of("fabric"));
        ModVersion candidate = new ModVersion("v2", "project", "1.1", "test-1.1.jar",
                "https://example.invalid/new", sha1(NEW_BYTES), List.of("1.21.8"), List.of("fabric"));
        return new ModUpdate(local, current, candidate, "Test Mod");
    }

    private static ModUpdaterService.Snapshot await(ModUpdaterService service, ModUpdaterService.State state)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            ModUpdaterService.Snapshot snapshot = service.snapshot();
            if (snapshot.state() == state) {
                return snapshot;
            }
            Thread.sleep(20);
        }
        fail("timed out waiting for " + state + ", last state " + service.snapshot().state());
        return null;
    }

    @Test
    void doesNothingUntilAsked() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> {
                    calls.incrementAndGet();
                    return List.of();
                },
                (url, target) -> { });
        try {
            Thread.sleep(80);
            assertEquals(ModUpdaterService.State.IDLE, service.snapshot().state());
            assertEquals(0, calls.get());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void checkReportsUpdatesAndModCount() throws Exception {
        Path old = installedMod("test-1.0.jar");
        ModUpdate update = update(old);
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(update),
                (url, target) -> { });
        try {
            service.checkAsync();
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertEquals(1, snapshot.updates().size());
            assertEquals(1, snapshot.scannedMods());
            assertNull(snapshot.error());
            assertFalse(service.snapshot().busy());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void networkFailureBecomesOneReadableMessage() throws Exception {
        installedMod("test-1.0.jar");
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> {
                    throw new IOException("Modrinth /version_files returned HTTP 429");
                },
                (url, target) -> { });
        try {
            service.checkAsync();
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.FAILED);

            assertTrue(snapshot.error().contains("429"), snapshot.error());
            assertTrue(snapshot.updates().isEmpty());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void installWritesTheJarAndRefreshesTheList() throws Exception {
        Path old = installedMod("test-1.0.jar");
        ModUpdate update = update(old);
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(),
                (url, target) -> Files.write(target, NEW_BYTES));
        try {
            service.installAsync(List.of(update));
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertEquals(1, snapshot.outcomes().size());
            assertTrue(snapshot.outcomes().get(0).success());
            assertTrue(Files.exists(gameDir.resolve("mods/test-1.1.jar")));
            assertFalse(Files.exists(old));
            assertTrue(snapshot.updates().isEmpty());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void blockedUpdateIsReportedAndNeverDownloaded() throws Exception {
        Path old = installedMod("test-1.0.jar");
        ModUpdate base = update(old);
        ModUpdate blocked = new ModUpdate(base.local(), base.current(), base.candidate(), base.title(),
                Compatibility.Verdict.block("it needs Sodium, which is not in mods/ — install that first"));
        AtomicInteger downloads = new AtomicInteger();
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(),
                (url, target) -> downloads.incrementAndGet());
        try {
            service.installAsync(List.of(blocked));
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertEquals(0, downloads.get());
            assertEquals(1, snapshot.outcomes().size());
            assertFalse(snapshot.outcomes().get(0).success());
            assertTrue(snapshot.outcomes().get(0).detail().contains("Sodium"),
                    snapshot.outcomes().get(0).detail());
            assertTrue(Files.exists(old));
            assertFalse(Files.exists(gameDir.resolve("mods/test-1.1.jar")));
        } finally {
            service.shutdown();
        }
    }

    private static ModVersion dependency(String projectId, String fileName, byte[] bytes,
            List<Dependency> dependencies) {
        return new ModVersion(projectId + "-v1", projectId, "1.0", fileName,
                "https://example.invalid/dependency/" + fileName, sha1(bytes),
                List.of("1.21.8"), List.of("fabric"), dependencies);
    }

    @Test
    void resolvesAndInstallsAMissingDependencyTogether() throws Exception {
        ModUpdate iris = update(installedMod("iris-1.0.jar"));
        ModUpdate target = new ModUpdate(iris.local(), iris.current(), iris.candidate(), "Iris Shaders",
                Compatibility.Verdict.block("it needs Sodium", List.of("SODIUM")));
        Map<String, byte[]> jars = new HashMap<>();
        jars.put(target.candidate().downloadUrl(), NEW_BYTES);
        jars.put("https://example.invalid/dependency/sodium.jar", SODIUM_BYTES);
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(),
                (url, file) -> Files.write(file, jars.get(url)),
                projectId -> "SODIUM".equals(projectId)
                        ? dependency("SODIUM", "sodium.jar", SODIUM_BYTES, List.of())
                        : null);
        try {
            service.installAsync(List.of(target));
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertTrue(snapshot.outcomes().get(0).success(), String.valueOf(snapshot.outcomes().get(0).detail()));
            assertTrue(Files.exists(gameDir.resolve("mods/sodium.jar")), "dependency jar installed");
            assertTrue(Files.exists(gameDir.resolve("mods/test-1.1.jar")), "update jar installed");
        } finally {
            service.shutdown();
        }
    }

    @Test
    void followsOneExtraLevelOfDependencies() throws Exception {
        ModUpdate iris = update(installedMod("iris-1.0.jar"));
        ModUpdate target = new ModUpdate(iris.local(), iris.current(), iris.candidate(), "Iris Shaders",
                Compatibility.Verdict.block("it needs Sodium", List.of("SODIUM")));
        Map<String, byte[]> jars = new HashMap<>();
        jars.put(target.candidate().downloadUrl(), NEW_BYTES);
        jars.put("https://example.invalid/dependency/sodium.jar", SODIUM_BYTES);
        jars.put("https://example.invalid/dependency/library.jar", LIBRARY_BYTES);
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(),
                (url, file) -> Files.write(file, jars.get(url)),
                projectId -> switch (projectId) {
                    case "SODIUM" -> dependency("SODIUM", "sodium.jar", SODIUM_BYTES,
                            List.of(new Dependency("LIBRARY", null, "required")));
                    case "LIBRARY" -> dependency("LIBRARY", "library.jar", LIBRARY_BYTES, List.of());
                    default -> null;
                });
        try {
            service.installAsync(List.of(target));
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertTrue(snapshot.outcomes().get(0).success());
            assertTrue(Files.exists(gameDir.resolve("mods/sodium.jar")));
            assertTrue(Files.exists(gameDir.resolve("mods/library.jar")));
            assertTrue(Files.exists(gameDir.resolve("mods/test-1.1.jar")));
        } finally {
            service.shutdown();
        }
    }

    @Test
    void anUnsatisfiableDependencyBlocksWithoutDownloadingAnything() throws Exception {
        Path old = installedMod("iris-1.0.jar");
        ModUpdate iris = update(old);
        ModUpdate target = new ModUpdate(iris.local(), iris.current(), iris.candidate(), "Iris Shaders",
                Compatibility.Verdict.block("it needs Sodium, which is not in mods/ — install that first",
                        List.of("SODIUM")));
        AtomicInteger downloads = new AtomicInteger();
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(),
                (url, file) -> downloads.incrementAndGet(),
                projectId -> null);
        try {
            service.installAsync(List.of(target));
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertEquals(0, downloads.get());
            assertFalse(snapshot.outcomes().get(0).success());
            assertTrue(snapshot.outcomes().get(0).detail().contains("Sodium"));
            assertTrue(Files.exists(old));
            assertFalse(Files.exists(gameDir.resolve("mods/test-1.1.jar")));
            assertFalse(Files.exists(gameDir.resolve("mods/sodium.jar")));
        } finally {
            service.shutdown();
        }
    }

    @Test
    void aChecksumMismatchInTheBatchInstallsNothing() throws Exception {
        Path old = installedMod("iris-1.0.jar");
        ModUpdate iris = update(old);
        ModUpdate target = new ModUpdate(iris.local(), iris.current(), iris.candidate(), "Iris Shaders",
                Compatibility.Verdict.block("it needs Sodium", List.of("SODIUM")));
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(),
                (url, file) -> Files.write(file, url.contains("sodium") ? "tampered".getBytes(StandardCharsets.UTF_8)
                        : NEW_BYTES),
                projectId -> dependency("SODIUM", "sodium.jar", SODIUM_BYTES, List.of()));
        try {
            service.installAsync(List.of(target));
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertFalse(snapshot.outcomes().get(0).success());
            assertTrue(Files.exists(old), "the installed jar is untouched");
            assertFalse(Files.exists(gameDir.resolve("mods/test-1.1.jar")));
            assertFalse(Files.exists(gameDir.resolve("mods/sodium.jar")));
        } finally {
            service.shutdown();
        }
    }

    @Test
    void installFailureIsReportedInsteadOfThrown() throws Exception {
        Path old = installedMod("test-1.0.jar");
        ModUpdate update = update(old);
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(),
                (url, target) -> {
                    throw new IOException("Download failed: HTTP 503");
                });
        try {
            service.installAsync(List.of(update));
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertEquals(1, snapshot.outcomes().size());
            assertFalse(snapshot.outcomes().get(0).success());
            assertTrue(snapshot.outcomes().get(0).detail().contains("503"));
            assertTrue(Files.exists(old));
            assertFalse(Files.exists(gameDir.resolve("mods/test-1.1.jar")));
        } finally {
            service.shutdown();
        }
    }

    /** An outdated dependency is replaced, and the jar it replaces must not stay next to it. */
    @Test
    void anOutdatedDependencyIsReplacedAndItsOldJarRetired() throws Exception {
        Path old = installedMod("test-1.0.jar");
        Path oldSodium = installedMod("sodium-0.7.0.jar");
        ModUpdate base = update(old);
        ModUpdate target = new ModUpdate(base.local(), base.current(), base.candidate(), "Iris Shaders",
                Compatibility.Verdict.block("it asks for a different Sodium build than the installed mc1.21.8-0.7.0-fabric",
                        List.of("SODIUM"), Map.of("SODIUM", oldSodium)));
        Map<String, byte[]> jars = new HashMap<>();
        jars.put(target.candidate().downloadUrl(), NEW_BYTES);
        jars.put("https://example.invalid/dependency/sodium-0.7.3.jar", SODIUM_BYTES);
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(),
                (url, file) -> Files.write(file, jars.get(url)),
                projectId -> "SODIUM".equals(projectId)
                        ? dependency("SODIUM", "sodium-0.7.3.jar", SODIUM_BYTES, List.of())
                        : null);
        try {
            service.installAsync(List.of(target));
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertTrue(snapshot.outcomes().get(0).success(), String.valueOf(snapshot.outcomes().get(0).detail()));
            assertTrue(Files.exists(gameDir.resolve("mods/sodium-0.7.3.jar")));
            assertFalse(Files.exists(oldSodium), "the dependency build it replaced is gone");
            assertTrue(Files.exists(gameDir.resolve("mods/test-1.1.jar")));
        } finally {
            service.shutdown();
        }
    }

    /** The resolved build is the one already installed: nothing is downloaded, so nothing is deleted. */
    @Test
    void aDependencyAlreadyAtTheResolvedBuildIsKept() throws Exception {
        Path old = installedMod("test-1.0.jar");
        Path sodium = installedMod("sodium-0.7.3.jar");
        String sodiumSha = ModScanner.sha1(sodium);
        ModUpdate base = update(old);
        ModUpdate target = new ModUpdate(base.local(), base.current(), base.candidate(), "Iris Shaders",
                Compatibility.Verdict.block("it asks for a different Sodium build",
                        List.of("SODIUM"), Map.of("SODIUM", sodium)));
        AtomicInteger sodiumDownloads = new AtomicInteger();
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(),
                (url, file) -> {
                    if (url.contains("sodium")) {
                        sodiumDownloads.incrementAndGet();
                    }
                    Files.write(file, NEW_BYTES);
                },
                projectId -> "SODIUM".equals(projectId)
                        ? new ModVersion("SODIUM-v3", "SODIUM", "0.7.3", "sodium-0.7.3.jar",
                                "https://example.invalid/dependency/sodium.jar", sodiumSha,
                                List.of("1.21.8"), List.of("fabric"))
                        : null);
        try {
            service.installAsync(List.of(target));
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertTrue(snapshot.outcomes().get(0).success(), String.valueOf(snapshot.outcomes().get(0).detail()));
            assertEquals(0, sodiumDownloads.get());
            assertTrue(Files.exists(sodium), "an up-to-date dependency is not deleted");
            assertTrue(Files.exists(gameDir.resolve("mods/test-1.1.jar")));
        } finally {
            service.shutdown();
        }
    }

    /** The screen marks a row from the same verdict the service acts on. */
    @Test
    void rowMarkingFollowsWhatTheUpdaterCanFix() {
        ModUpdate base = update(Path.of("/tmp/mods/x-1.0.jar"));
        assertFalse(base.resolvable());
        assertNull(base.reason());

        ModUpdate fixable = new ModUpdate(base.local(), base.current(), base.candidate(), "X",
                Compatibility.Verdict.block("it needs Sodium, which is not in mods/", List.of("SODIUM")));
        assertTrue(fixable.resolvable());
        assertEquals("it needs Sodium, which is not in mods/", fixable.reason());

        ModUpdate stuck = new ModUpdate(base.local(), base.current(), base.candidate(), "X",
                Compatibility.Verdict.block("sodium-extra needs sodium >=0.6.0 <0.7.0"));
        assertFalse(stuck.resolvable());
        assertEquals("sodium-extra needs sodium >=0.6.0 <0.7.0", stuck.reason());

        // A warning is not a block, but it still has something to say on hover.
        ModUpdate warned = new ModUpdate(base.local(), base.current(), base.candidate(), "X",
                Compatibility.Verdict.warn("other asks for x 1.0, which cannot be checked"));
        assertFalse(warned.blocked());
        assertEquals("other asks for x 1.0, which cannot be checked", warned.reason());
    }

    /** A re-upload can reuse a file name: the jar just written must not be deleted as "the old one". */
    @Test
    void aReplacementWithTheSameFileNameIsNotDeleted() throws Exception {
        Path old = installedMod("test-1.0.jar");
        Path sodium = installedMod("sodium.jar");
        byte[] fresh = "new-sodium-jar".getBytes(StandardCharsets.UTF_8);
        ModUpdate base = update(old);
        ModUpdate target = new ModUpdate(base.local(), base.current(), base.candidate(), "Iris Shaders",
                Compatibility.Verdict.block("it asks for a different Sodium build",
                        List.of("SODIUM"), Map.of("SODIUM", sodium)));
        ModUpdaterService service = new ModUpdaterService(gameDir,
                mods -> List.of(),
                (url, file) -> Files.write(file, url.contains("sodium") ? fresh : NEW_BYTES),
                projectId -> "SODIUM".equals(projectId)
                        ? new ModVersion("SODIUM-v4", "SODIUM", "0.7.3", "sodium.jar",
                                "https://example.invalid/dependency/sodium.jar", sha1(fresh),
                                List.of("1.21.8"), List.of("fabric"))
                        : null);
        try {
            service.installAsync(List.of(target));
            ModUpdaterService.Snapshot snapshot = await(service, ModUpdaterService.State.READY);

            assertTrue(snapshot.outcomes().get(0).success(), String.valueOf(snapshot.outcomes().get(0).detail()));
            assertTrue(Files.exists(sodium), "the freshly installed dependency is not deleted");
            assertEquals(new String(fresh, StandardCharsets.UTF_8), Files.readString(sodium));
        } finally {
            service.shutdown();
        }
    }
}
