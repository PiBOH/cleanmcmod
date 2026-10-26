package piboh.cleanmc.mod.clean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanMcModServiceTest {

    @TempDir
    Path gameDir;

    @TempDir
    Path configDir;

    private void write(String relative, String content) throws IOException {
        Path file = gameDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    @Test
    void cleanAsyncDeletesCachesUpdatesScanAndWritesConfig() throws IOException, ExecutionException, InterruptedException {
        write("logs/latest.log", "0123456789");
        write(".fabric/remappedJars/mod.jar", "0123456789");

        CleanMcModService service = new CleanMcModService(gameDir, configDir);
        try {
            assertTrue(service.scan().presentCount() >= 2);

            CleanReport report = service.cleanAsync().get();

            assertNotNull(report);
            assertFalse(service.isRunning());
            assertEquals(report, service.lastReport());
            assertTrue(report.filesDeleted() >= 2);
            assertFalse(Files.exists(gameDir.resolve("logs/latest.log")));
            assertTrue(service.lastScan().presentTargets().stream().allMatch(state -> state.files() == 0),
                    "the scan is refreshed after a clean");

            service.config().dryRun = true;
            service.saveConfig();
            Path configFile = configDir.resolve("cleanmcmod.json");
            assertTrue(Files.isRegularFile(configFile));
            assertTrue(Files.readString(configFile).contains("\"dryRun\": true"));
        } finally {
            service.shutdown();
        }
    }
}
