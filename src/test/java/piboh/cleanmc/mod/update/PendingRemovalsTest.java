package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A jar the game held open is queued, and the next launch moves it out of {@code mods/}. */
class PendingRemovalsTest {

    @TempDir
    Path gameDir;

    @Test
    void aQueuedJarIsMovedOutOfTheModsFolderOnTheNextLaunch() throws IOException {
        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Path config = Files.createDirectories(gameDir.resolve("config"));
        Path old = mods.resolve("mod-1.0.jar");
        Files.writeString(old, "old-content");

        PendingRemovals.schedule(old, config);
        assertTrue(Files.exists(old));
        assertEquals(List.of(old), PendingRemovals.pending(config));

        PendingRemovals.run(config);

        assertFalse(Files.exists(old));
        Path backup = config.resolve("cleanmcmod").resolve("backups").resolve("mod-1.0.jar");
        assertTrue(Files.exists(backup));
        assertEquals("old-content", Files.readString(backup));
        assertTrue(PendingRemovals.pending(config).isEmpty());
    }

    @Test
    void aJarThatIsAlreadyGoneIsDroppedFromTheQueue() throws IOException {
        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Path config = Files.createDirectories(gameDir.resolve("config"));
        PendingRemovals.schedule(mods.resolve("gone.jar"), config);

        PendingRemovals.run(config);

        assertTrue(PendingRemovals.pending(config).isEmpty());
    }
}
