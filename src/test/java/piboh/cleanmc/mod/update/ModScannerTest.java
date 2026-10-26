package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModScannerTest {

    @TempDir
    Path gameDir;

    private Path modJar(String name, String metadataJson) throws IOException {
        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Path jar = mods.resolve(name);
        try (OutputStream out = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("fabric.mod.json"));
            zip.write(metadataJson.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return jar;
    }

    @Test
    void readsIdVersionAndHash() throws IOException {
        Path jar = modJar("sodium.jar", "{\"schemaVersion\":1,\"id\":\"sodium\",\"version\":\"0.5.9\"}");

        List<LocalMod> mods = new ModScanner().scan(gameDir);

        assertEquals(1, mods.size());
        assertEquals("sodium", mods.get(0).modId());
        assertEquals("0.5.9", mods.get(0).version());
        assertEquals(jar, mods.get(0).path());
        assertEquals(ModScanner.sha1(jar), mods.get(0).sha1());
        assertEquals(40, mods.get(0).sha1().length());
    }

    @Test
    void skipsThisModByMetadataAndByFileName() throws IOException {
        modJar("whatever.jar", "{\"schemaVersion\":1,\"id\":\"cleanmcmod\",\"version\":\"0.1.0\"}");
        Files.writeString(gameDir.resolve("mods/cleanmcmod-0.1.0.jar"), "not a zip");

        assertTrue(new ModScanner().scan(gameDir).isEmpty());
    }

    @Test
    void ignoresFilesThatAreNotJars() throws IOException {
        modJar("sodium.jar", "{\"schemaVersion\":1,\"id\":\"sodium\",\"version\":\"0.5.9\"}");
        Files.writeString(gameDir.resolve("mods/notes.txt"), "hello");
        Files.writeString(gameDir.resolve("mods/mod.jar.old"), "old backup");

        assertEquals(1, new ModScanner().scan(gameDir).size());
    }

    @Test
    void unreadableJarIsStillHashedSoModrinthCanMatchIt() throws IOException {
        Files.createDirectories(gameDir.resolve("mods"));
        Files.writeString(gameDir.resolve("mods/broken.jar"), "not a zip");

        List<LocalMod> mods = new ModScanner().scan(gameDir);

        assertEquals(1, mods.size());
        assertNull(mods.get(0).modId());
        assertNull(mods.get(0).version());
    }

    @Test
    void missingModsFolderIsEmptyNotAnError() {
        assertTrue(new ModScanner().scan(gameDir).isEmpty());
        assertTrue(new ModScanner().scan(gameDir.resolve("nope")).isEmpty());
    }
}
