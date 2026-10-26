package piboh.cleanmc.mod.update;

import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Update checks and installs, off the render thread.
 *
 * <p>The screen reads {@link #snapshot()} while it renders, so network work never touches the
 * client thread: {@code snapshot} is only replaced from the worker, and each value is immutable.
 * Nothing here runs on its own — a check needs a button, an install needs a confirmation.</p>
 */
public final class ModUpdaterService {

    public enum State {
        IDLE,
        CHECKING,
        READY,
        INSTALLING,
        FAILED
    }

    public record Snapshot(
            State state,
            List<ModUpdate> updates,
            String error,
            List<ModInstaller.Outcome> outcomes,
            int scannedMods) {

        public Snapshot {
            updates = List.copyOf(updates);
            outcomes = List.copyOf(outcomes);
        }

        public boolean busy() {
            return state == State.CHECKING || state == State.INSTALLING;
        }
    }

    public interface Checker {
        List<ModUpdate> check(List<LocalMod> mods) throws IOException, InterruptedException;
    }

    /** Download metadata for one dependency, already limited to this game version and loader. */
    public interface DependencyResolver {
        ModVersion resolve(String projectId) throws IOException, InterruptedException;
    }

    private static final DependencyResolver NO_RESOLVER = projectId -> null;

    private final Path gameDir;
    private final ModScanner scanner;
    private final ModInstaller installer;
    private final Checker checker;
    private final ModInstaller.Downloader downloader;
    private final DependencyResolver resolver;
    private final ExecutorService executor;

    private volatile Snapshot snapshot = new Snapshot(State.IDLE, List.of(), null, List.of(), 0);

    public ModUpdaterService(Path gameDir, Checker checker, ModInstaller.Downloader downloader) {
        this(gameDir, checker, downloader, NO_RESOLVER);
    }

    public ModUpdaterService(Path gameDir, Checker checker, ModInstaller.Downloader downloader,
            DependencyResolver resolver) {
        this(gameDir, new ModScanner(), new ModInstaller(gameDir.resolve("mods")), checker, downloader, resolver);
    }

    ModUpdaterService(Path gameDir, ModScanner scanner, ModInstaller installer, Checker checker,
            ModInstaller.Downloader downloader, DependencyResolver resolver) {
        this.gameDir = gameDir.toAbsolutePath().normalize();
        this.scanner = scanner;
        this.installer = installer;
        this.checker = checker;
        this.downloader = downloader;
        this.resolver = resolver;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "cleanmcmod-updater");
            thread.setDaemon(true);
            return thread;
        });
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public void checkAsync() {
        if (snapshot.busy()) {
            return;
        }
        snapshot = new Snapshot(State.CHECKING, snapshot.updates(), null, List.of(), snapshot.scannedMods());
        executor.submit(() -> {
            try {
                List<LocalMod> mods = scanner.scan(gameDir);
                List<ModUpdate> updates = checker.check(mods);
                snapshot = new Snapshot(State.READY, updates, null, List.of(), mods.size());
            } catch (Exception e) {
                CleanMcMod.LOGGER.warn("Mod update check failed: {}", e.toString());
                snapshot = new Snapshot(State.FAILED, List.of(), ModInstaller.message(e), List.of(),
                        snapshot.scannedMods());
            }
        });
    }

    public void installAsync(List<ModUpdate> updates) {
        if (snapshot.busy() || updates.isEmpty()) {
            return;
        }
        List<ModUpdate> requested = List.copyOf(updates);
        snapshot = new Snapshot(State.INSTALLING, snapshot.updates(), null, List.of(), snapshot.scannedMods());
        executor.submit(() -> {
            List<ModInstaller.Outcome> outcomes = new ArrayList<>(requested.size());
            List<ModUpdate> installable = new ArrayList<>(requested.size());
            List<ModVersion> dependencies = new ArrayList<>();
            List<LocalMod> installed = scanner.scan(gameDir);
            for (ModUpdate update : requested) {
                if (!update.blocked()) {
                    installable.add(update);
                    continue;
                }
                if (update.verdict().missing().isEmpty()) {
                    // Blocked for a reason no download can fix: an incompatible mod or a range break.
                    CleanMcMod.LOGGER.info("Skipped {} {}: {}", update.displayName(),
                            update.availableVersion(), update.verdict().reason());
                    outcomes.add(new ModInstaller.Outcome(update, false, update.verdict().reason()));
                    continue;
                }
                List<ModVersion> resolved = resolveMissing(update, installed, dependencies);
                if (resolved == null) {
                    CleanMcMod.LOGGER.info("Skipped {} {}: {}", update.displayName(),
                            update.availableVersion(), update.verdict().reason());
                    outcomes.add(new ModInstaller.Outcome(update, false, update.verdict().reason()));
                    continue;
                }
                dependencies.addAll(resolved);
                installable.add(update);
            }
            if (!dependencies.isEmpty()) {
                CleanMcMod.LOGGER.info("Installing {} resolved dependencies together with the update",
                        dependencies.size());
            }
            if (!installable.isEmpty()) {
                List<ModInstaller.Outcome> batch = installer.installTogether(installable, dependencies,
                        downloader);
                outcomes.addAll(batch);
                if (batch.stream().allMatch(ModInstaller.Outcome::success)) {
                    retireReplaced(installable, dependencies);
                }
            }
            CleanMcMod.LOGGER.info("Mod update install finished: {}/{} installed",
                    outcomes.stream().filter(ModInstaller.Outcome::success).count(), outcomes.size());

            try {
                List<LocalMod> mods = scanner.scan(gameDir);
                List<ModUpdate> remaining = checker.check(mods);
                snapshot = new Snapshot(State.READY, remaining, null, outcomes, mods.size());
            } catch (Exception e) {
                snapshot = new Snapshot(State.FAILED, List.of(),
                        ModInstaller.message(e), outcomes, snapshot.scannedMods());
            }
        });
    }

    /**
     * Deletes the dependency jars the batch just replaced — and only those: a dependency whose
     * resolved build was already installed is not downloaded, so its jar must stay put.
     */
    private void retireReplaced(List<ModUpdate> installed, List<ModVersion> fetched) {
        Set<String> written = new HashSet<>();
        for (ModVersion version : fetched) {
            written.add(version.fileName());
        }
        for (ModUpdate update : installed) {
            for (Map.Entry<String, Path> entry : update.verdict().outdated().entrySet()) {
                String oldName = entry.getValue().getFileName().toString();
                boolean replaced = !written.contains(oldName) && fetched.stream()
                        .anyMatch(version -> version.projectId().equals(entry.getKey()));
                if (replaced) {
                    installer.retire(entry.getValue());
                }
            }
        }
    }

    /**
     * The jars a blocked update needs, resolved from Modrinth: the missing dependencies and, one
     * level deeper, their own missing required dependencies. Null when one of them cannot be found,
     * which keeps the update blocked with its original reason.
     */
    private List<ModVersion> resolveMissing(ModUpdate update, List<LocalMod> installed,
            List<ModVersion> planned) {
        List<ModVersion> plan = new ArrayList<>();
        try {
            for (String projectId : update.verdict().missing()) {
                ModVersion version = resolver.resolve(projectId);
                if (version == null) {
                    return null;
                }
                plan.add(version);
                for (Dependency dependency : version.dependencies()) {
                    if (!dependency.required() || Compatibility.platformProvided(dependency.projectId())
                            || dependency.projectId().equals(update.candidate().projectId())
                            || alreadyPlanned(dependency.projectId(), plan, planned)) {
                        continue;
                    }
                    ModVersion nested = resolver.resolve(dependency.projectId());
                    if (nested == null) {
                        return null;
                    }
                    plan.add(nested);
                }
            }
        } catch (IOException | InterruptedException e) {
            CleanMcMod.LOGGER.warn("Could not resolve a dependency for {}: {}", update.displayName(),
                    e.toString());
            return null;
        }
        // A jar we already have is not downloaded again.
        plan.removeIf(version -> installed.stream()
                .anyMatch(mod -> mod.sha1().equalsIgnoreCase(version.sha1())));
        return plan;
    }

    private static boolean alreadyPlanned(String projectId, List<ModVersion> plan, List<ModVersion> planned) {
        return plan.stream().anyMatch(version -> version.projectId().equals(projectId))
                || planned.stream().anyMatch(version -> version.projectId().equals(projectId));
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
