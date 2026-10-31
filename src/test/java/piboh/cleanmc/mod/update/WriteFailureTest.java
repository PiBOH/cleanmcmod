package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The jar the game has open cannot be overwritten. That mod fails with its file name in the reason,
 * and the rest of the request still lands in {@code mods/}.
 */
class WriteFailureTest {

    @TempDir
    Path gameDir;

    private static ModUpdate update(Path local, String modId) {
        LocalMod installed = new LocalMod(local, "old-sha1", modId, "1.0");
        ModVersion current = new ModVersion("project-" + modId, modId, "1.0", local.getFileName().toString(),
                "https://example.invalid/" + modId + "-1.0.jar", "old-sha1", List.of("1.21.8"),
                List.of("fabric"), List.of(), ModSource.CURSEFORGE);
        ModVersion candidate = new ModVersion("project-" + modId, modId, "1.1", modId + "-1.1.jar",
                "https://example.invalid/" + modId + "-1.1.jar", null, List.of("1.21.8"),
                List.of("fabric"), List.of(), ModSource.CURSEFORGE);
        return new ModUpdate(installed, current, candidate, modId.toUpperCase());
    }

    @Test
    void aJarThatCannotBeWrittenFailsAlone() throws IOException {
        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Files.writeString(mods.resolve("a-1.0.jar"), "a");
        Files.writeString(mods.resolve("b-1.0.jar"), "b");
        // A non-empty directory where the new jar has to go makes the move fail, the way a jar the
        // game holds open refuses to be replaced on Windows.
        Files.createDirectories(mods.resolve("b-1.1.jar"));
        Files.writeString(mods.resolve("b-1.1.jar").resolve("keep.txt"), "keep");

        List<ModInstaller.Outcome> outcomes = new ModInstaller(mods).installTogether(
                List.of(update(mods.resolve("a-1.0.jar"), "a"), update(mods.resolve("b-1.0.jar"), "b")),
                List.of(), (url, target) -> Files.writeString(target, "downloaded"));

        assertTrue(outcomes.get(0).success(), outcomes.get(0).detail());
        assertFalse(outcomes.get(1).success());
        assertTrue(outcomes.get(1).detail().contains("b-1.1.jar"), outcomes.get(1).detail());
        assertTrue(Files.exists(mods.resolve("a-1.1.jar")));
        assertFalse(Files.exists(mods.resolve("a-1.0.jar")));
    }
}
