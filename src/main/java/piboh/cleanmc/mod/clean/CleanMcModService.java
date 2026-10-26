package piboh.cleanmc.mod.clean;

import piboh.cleanmc.mod.CleanMcMod;
import piboh.cleanmc.mod.config.CleanMcModConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client-side facade over scanning and cleaning.
 *
 * <p>Cleaning runs on a daemon thread so a multi-gigabyte asset purge does not freeze the game.
 * The GUI polls {@link #isRunning()} and {@link #lastReport()}; the command subscribes to the
 * returned future.</p>
 */
public final class CleanMcModService {

    private final Path gameDir;
    private final Path configFile;
    private final CleanMcModConfig config;
    private final CacheScanner scanner = new CacheScanner();
    private final CacheAutoDetector detector = new CacheAutoDetector();
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile CacheScan lastScan;
    private volatile CleanReport lastReport;

    public CleanMcModService(Path gameDir, Path configDir) {
        this.gameDir = gameDir.toAbsolutePath().normalize();
        this.configFile = configDir.resolve(CleanMcMod.MOD_ID + ".json");
        this.config = CleanMcModConfig.load(this.configFile);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "cleanmcmod-cleaner");
            thread.setDaemon(true);
            return thread;
        });
    }

    public Path gameDir() {
        return gameDir;
    }

    public CleanMcModConfig config() {
        return config;
    }

    public void saveConfig() {
        CleanMcModConfig.save(config, configFile);
    }

    public List<CacheTarget> targets() {
        return CacheTargets.forConfig(gameDir, config, detector);
    }

    /** Measures every configured target. Safe to call on the client thread. */
    public CacheScan scan() {
        CacheScan scan = scanner.scanAll(targets());
        lastScan = scan;
        return scan;
    }

    public CacheScan lastScan() {
        return lastScan;
    }

    public CleanReport lastReport() {
        return lastReport;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** Cleans asynchronously. Returns an already completed future when a clean is in progress. */
    public CompletableFuture<CleanReport> cleanAsync() {
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(lastReport);
        }
        boolean dryRun = config.dryRun;
        List<CacheTarget> targets = targets();
        return CompletableFuture
                .supplyAsync(() -> new CacheCleaner(gameDir).clean(targets, dryRun), executor)
                .whenComplete((report, error) -> {
                    running.set(false);
                    if (error != null) {
                        CleanMcMod.LOGGER.error("Cache clean failed", error);
                        return;
                    }
                    if (report != null) {
                        lastReport = report;
                        log(report);
                        lastScan = scanner.scanAll(targets);
                    }
                });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private static void log(CleanReport report) {
        CleanMcMod.LOGGER.info("Cache clean finished in {} ms — {}", report.durationMillis(), report.summary());
        for (TargetResult result : report.results()) {
            CleanMcMod.LOGGER.info("  {}", result.describe());
        }
    }
}
