package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModInstallerTest {

    private static final byte[] NEW_BYTES = "new-jar-content".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path gameDir;

    private ModUpdate update(String oldName, String newName, String expectedSha1) throws IOException {
        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Path old = mods.resolve(oldName);
        Files.writeString(old, "old-content");
        LocalMod local = new LocalMod(old, sha1("old-content".getBytes(StandardCharsets.UTF_8)), "test", "1.0");
        ModVersion current = new ModVersion("v1", "project", "1.0", oldName,
                "https://example.invalid/old", local.sha1(), List.of("1.21.8"), List.of("fabric"));
        ModVersion candidate = new ModVersion("v2", "project", "1.1", newName,
                "https://example.invalid/new", expectedSha1, List.of("1.21.8"), List.of("fabric"));
        return new ModUpdate(local, current, candidate, "Test Mod");
    }

    private static ModInstaller.Downloader downloader(byte[] bytes) {
        return (url, target) -> Files.write(target, bytes);
    }

    private List<String> modsFolder() throws IOException {
        try (var entries = Files.list(gameDir.resolve("mods"))) {
            return entries.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    private static String sha1(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest(bytes)) {
                hex.append(String.format(Locale.ROOT, "%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void installsTheNewJarThenRemovesTheOldOne() throws IOException {
        ModInstaller installer = new ModInstaller(gameDir.resolve("mods"));
        ModUpdate update = update("mod-1.0.jar", "mod-1.1.jar", sha1(NEW_BYTES));

        ModInstaller.Outcome outcome = installer.installOne(update, downloader(NEW_BYTES));

        assertTrue(outcome.success());
        assertNull(outcome.detail());
        assertEquals(List.of("mod-1.1.jar"), modsFolder());
        assertEquals("new-jar-content", Files.readString(gameDir.resolve("mods/mod-1.1.jar")));
    }

    @Test
    void checksumMismatchLeavesTheFolderExactlyAsItWas() throws IOException {
        ModInstaller installer = new ModInstaller(gameDir.resolve("mods"));
        ModUpdate update = update("mod-1.0.jar", "mod-1.1.jar", "0000000000000000000000000000000000000000");

        ModInstaller.Outcome outcome = installer.installOne(update, downloader(NEW_BYTES));

        assertFalse(outcome.success());
        assertTrue(outcome.detail().contains("checksum"), outcome.detail());
        assertEquals(List.of("mod-1.0.jar"), modsFolder());
        assertEquals("old-content", Files.readString(gameDir.resolve("mods/mod-1.0.jar")));
    }

    @Test
    void failedDownloadLeavesTheFolderExactlyAsItWas() throws IOException {
        ModInstaller installer = new ModInstaller(gameDir.resolve("mods"));
        ModUpdate update = update("mod-1.0.jar", "mod-1.1.jar", sha1(NEW_BYTES));

        ModInstaller.Outcome outcome = installer.installOne(update, (url, target) -> {
            throw new IOException("network down");
        });

        assertFalse(outcome.success());
        assertEquals("network down", outcome.detail());
        assertEquals(List.of("mod-1.0.jar"), modsFolder());
    }

    @Test
    void sameFileNameIsReplacedWithoutLeavingBackups() throws IOException {
        ModInstaller installer = new ModInstaller(gameDir.resolve("mods"));
        ModUpdate update = update("mod.jar", "mod.jar", sha1(NEW_BYTES));

        ModInstaller.Outcome outcome = installer.installOne(update, downloader(NEW_BYTES));

        assertTrue(outcome.success());
        assertEquals(List.of("mod.jar"), modsFolder());
        assertEquals("new-jar-content", Files.readString(gameDir.resolve("mods/mod.jar")));
    }

    @Test
    void neverLeavesTemporaryOrOldFilesBehind() throws IOException {
        ModInstaller installer = new ModInstaller(gameDir.resolve("mods"));
        ModUpdate update = update("mod-1.0.jar", "mod-1.1.jar", sha1(NEW_BYTES));
        installer.installOne(update, downloader(NEW_BYTES));

        for (String name : modsFolder()) {
            assertFalse(name.startsWith(".cleanmcmod"), "temporary file left: " + name);
            assertFalse(name.endsWith(".old"), "backup file left: " + name);
        }
    }

    @Test
    void refusesFileNamesThatEscapeTheModsFolder() throws IOException {
        ModInstaller installer = new ModInstaller(gameDir.resolve("mods"));
        ModUpdate update = update("mod-1.0.jar", "../evil.jar", sha1(NEW_BYTES));

        ModInstaller.Outcome outcome = installer.installOne(update, downloader(NEW_BYTES));

        assertFalse(outcome.success());
        assertFalse(Files.exists(gameDir.resolve("evil.jar")));
        assertEquals(List.of("mod-1.0.jar"), modsFolder());
    }
}
