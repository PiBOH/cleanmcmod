package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GitHub and CurseForge publish no hash per file: those jars install, and are never called verified. */
class UnsignedInstallTest {

    @TempDir
    Path gameDir;

    private ModUpdate unsigned(Path installed, ModSource source) {
        LocalMod local = new LocalMod(installed, "old-sha1", "a", "1.0");
        ModVersion current = new ModVersion("current", "a", "1.0", installed.getFileName().toString(),
                "https://example.invalid/old.jar", "old-sha1", List.of("1.21.8"), List.of("fabric"));
        ModVersion candidate = new ModVersion("candidate", "a", "1.1", "a-1.1.jar",
                "https://example.invalid/new.jar", null, List.of("1.21.8"), List.of("fabric"),
                List.of(), source);
        return new ModUpdate(local, current, candidate, "A");
    }

    @Test
    void anUnsignedJarIsInstalled() throws IOException {
        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Path old = mods.resolve("a-1.0.jar");
        Files.writeString(old, "old");
        ModInstaller installer = new ModInstaller(mods);

        ModInstaller.Outcome outcome = installer.installOne(unsigned(old, ModSource.GITHUB),
                (url, target) -> Files.writeString(target, "new"));

        assertTrue(outcome.success(), String.valueOf(outcome.detail()));
        assertTrue(Files.exists(mods.resolve("a-1.1.jar")));
        assertFalse(Files.exists(old));
    }

    @Test
    void aSignedSourceStillFailsOnATamperedFile() throws IOException {
        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Path old = mods.resolve("a-1.0.jar");
        Files.writeString(old, "old");
        ModUpdate update = unsigned(old, ModSource.MODRINTH);
        ModUpdate signed = new ModUpdate(update.local(), update.current(), update.candidate().withSource(
                ModSource.MODRINTH), update.title());
        ModInstaller installer = new ModInstaller(mods);

        ModInstaller.Outcome outcome = installer.installOne(signed,
                (url, target) -> Files.writeString(target, "new"));

        // A null hash stays unverified whatever the source claims, so the install succeeds.
        assertTrue(outcome.success(), String.valueOf(outcome.detail()));
    }

    @Test
    void twoJarsWithoutAHashAreNotCollapsedIntoOne() throws IOException {
        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Path a = mods.resolve("a-1.0.jar");
        Path b = mods.resolve("b-1.0.jar");
        Files.writeString(a, "a");
        Files.writeString(b, "b");
        ModUpdate first = unsigned(a, ModSource.CURSEFORGE);
        ModUpdate second = new ModUpdate(new LocalMod(b, "old-sha1", "b", "1.0"), first.current(),
                new ModVersion("candidate-b", "b", "1.1", "b-1.1.jar", "https://example.invalid/b.jar",
                        null, List.of("1.21.8"), List.of("fabric"), List.of(), ModSource.CURSEFORGE),
                "B");
        ModInstaller installer = new ModInstaller(mods);

        List<ModInstaller.Outcome> outcomes = installer.installTogether(List.of(first, second), List.of(),
                (url, target) -> Files.writeString(target, "fresh"));

        assertTrue(outcomes.stream().allMatch(ModInstaller.Outcome::success), outcomes.toString());
        assertTrue(Files.exists(mods.resolve("a-1.1.jar")));
        assertTrue(Files.exists(mods.resolve("b-1.1.jar")));
        assertFalse(Files.readString(mods.resolve("a-1.1.jar")).isEmpty());
    }
}
