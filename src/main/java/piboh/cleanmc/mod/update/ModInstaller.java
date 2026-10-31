package piboh.cleanmc.mod.update;

import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Writes a downloaded update into {@code mods/} without ever losing the working jar.
 *
 * <p>Minecraft keeps the jars in {@code mods/} open for the whole session, so Windows refuses to
 * delete, rename or overwrite one while the game runs. The installer therefore never fights that
 * lock, the same way Mod Menu Updater does not: the new build is written under its own file name,
 * and the jar it replaces is deleted now when the game has let go of it and otherwise handed to
 * {@link PendingRemovals}, which moves it out of {@code mods/} on the next launch. Nothing is
 * deleted before the replacement is in place.</p>
 *
 * <p>The whole request still behaves as one batch: every jar is downloaded and hash-checked before
 * anything is written, one update whose own jar cannot be fetched or written fails alone, and a
 * missing dependency cancels everything, because a mod installed without its library is the half
 * install the batch exists to prevent.</p>
 */
public final class ModInstaller {

    public interface Downloader {
        void download(String url, Path target) throws IOException;
    }

    /**
     * @param detail on failure the reason naming the file
     */
    public record Outcome(ModUpdate update, boolean success, String detail) {
    }

    /** One jar the batch will write: the downloaded file and where it goes. */
    private record Prepared(ModVersion version, Path temp) {
    }

    private final Path modsDir;

    public ModInstaller(Path modsDir) {
        this.modsDir = modsDir.toAbsolutePath().normalize();
    }

    /**
     * Installs the requested updates and the dependencies resolved for them as one unit: every jar
     * is downloaded and hash-checked first, so nothing in {@code mods/} changes until the whole set
     * is ready.
     */
    public List<Outcome> installTogether(List<ModUpdate> updates, List<ModVersion> dependencies,
            Downloader downloader) {
        return installTogether(updates, dependencies, downloader, line -> {
        });
    }

    /**
     * @param status told which jar is being fetched and how fast it arrived, so the screen can show
     *               the work happening instead of a bar that sits still
     */
    public List<Outcome> installTogether(List<ModUpdate> updates, List<ModVersion> dependencies,
            Downloader downloader, Consumer<String> status) {
        // A mod and its dependency can both be in the update list, and the dependency can be
        // resolved for the mod too: the same build is one jar, downloaded once.
        Map<String, ModVersion> unique = new LinkedHashMap<>();
        for (ModVersion version : dependencies) {
            unique.putIfAbsent(version.fileKey(), version);
        }
        Map<String, ModUpdate> requested = new LinkedHashMap<>();
        for (ModUpdate update : updates) {
            unique.putIfAbsent(update.candidate().fileKey(), update.candidate());
            requested.put(update.candidate().fileKey(), update);
        }

        List<Prepared> prepared = new ArrayList<>(unique.size());
        Map<String, String> broken = new LinkedHashMap<>();
        try {
            Files.createDirectories(modsDir);
            for (ModVersion version : unique.values()) {
                if (!modsDir.resolve(version.fileName()).normalize().startsWith(modsDir)) {
                    throw new IOException("file name escapes the mods folder: " + version.fileName());
                }
                Path temp = Files.createTempFile(modsDir, ".cleanmcmod-", ".tmp");
                try {
                    fetch(downloader, version, temp, status);
                    verify(version, temp);
                } catch (IOException e) {
                    deleteQuietly(temp);
                    if (!requested.containsKey(version.fileKey())) {
                        // A library the batch needs is missing: the updates that need it cannot be
                        // installed, and the ones that do not are not worth splitting the batch for.
                        throw e;
                    }
                    broken.put(version.fileKey(), message(e));
                    CleanMcMod.LOGGER.warn("Leaving {} out of this batch: {}", version.fileName(),
                            message(e));
                    continue;
                }
                prepared.add(new Prepared(version, temp));
            }
        } catch (IOException | RuntimeException e) {
            discard(prepared);
            return cancelled(updates, message(e));
        }

        List<Path> written = new ArrayList<>(prepared.size());
        for (int index = 0; index < prepared.size(); index++) {
            Prepared entry = prepared.get(index);
            Path target = modsDir.resolve(entry.version().fileName()).normalize();
            try {
                Files.move(entry.temp(), target, StandardCopyOption.REPLACE_EXISTING);
                written.add(target);
            } catch (IOException e) {
                deleteQuietly(entry.temp());
                if (requested.containsKey(entry.version().fileKey())) {
                    // The jar the game has open cannot be overwritten: this mod fails, the others
                    // in the batch still go in.
                    broken.put(entry.version().fileKey(), entry.version().fileName() + ": " + message(e));
                    CleanMcMod.LOGGER.warn("Leaving {} out of this batch: {}",
                            entry.version().fileName(), message(e));
                    continue;
                }
                for (Prepared rest : prepared.subList(index + 1, prepared.size())) {
                    deleteQuietly(rest.temp());
                }
                for (Path path : written) {
                    deleteQuietly(path);
                }
                return cancelled(updates, message(e));
            }
        }

        List<Outcome> outcomes = new ArrayList<>(updates.size());
        for (ModUpdate update : updates) {
            String reason = broken.get(update.candidate().fileKey());
            if (reason != null) {
                outcomes.add(new Outcome(update, false, reason));
                continue;
            }
            Path previous = update.local().path().toAbsolutePath().normalize();
            if (!previous.equals(modsDir.resolve(update.candidate().fileName()).normalize())) {
                queueRemoval(previous);
            }
            outcomes.add(new Outcome(update, true, null));
        }
        return outcomes;
    }

    /**
     * Deletes the jar an update replaced. A jar the game holds open cannot be deleted, so it is
     * queued instead and removed on the next launch.
     */
    public void queueRemoval(Path jar) {
        try {
            if (Files.deleteIfExists(jar)) {
                CleanMcMod.LOGGER.info("Removed the replaced jar {}", jar.getFileName());
            }
        } catch (IOException locked) {
            PendingRemovals.schedule(jar, configDir());
        }
    }

    private Path configDir() {
        Path parent = modsDir.getParent();
        return parent == null ? modsDir.resolve("config") : parent.resolve("config");
    }

    /**
     * Downloads one jar, retrying once: a dropped connection or a 5xx from the CDN is not worth
     * failing the whole batch for. The reason names the file, so the alert can say which mod could
     * not be fetched instead of a bare "download failed".
     */
    private static void fetch(Downloader downloader, ModVersion version, Path temp,
            Consumer<String> status) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            status.accept(version.fileName());
            long started = System.nanoTime();
            try {
                downloader.download(version.downloadUrl(), temp);
                status.accept(version.fileName() + " · "
                        + describe(temp, (System.nanoTime() - started) / 1_000_000L));
                return;
            } catch (IOException e) {
                last = e;
                CleanMcMod.LOGGER.warn("Download of {} failed (attempt {}/2): {}", version.fileName(), attempt,
                        message(e));
                if (attempt == 1) {
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw new IOException(version.fileName() + ": " + message(last));
    }

    private static List<Outcome> cancelled(List<ModUpdate> updates, String reason) {
        CleanMcMod.LOGGER.warn("Mod update batch cancelled, mods folder unchanged: {}", reason);
        List<Outcome> failures = new ArrayList<>(updates.size());
        for (ModUpdate update : updates) {
            failures.add(new Outcome(update, false, reason));
        }
        return failures;
    }

    private static void discard(List<Prepared> prepared) {
        for (Prepared entry : prepared) {
            deleteQuietly(entry.temp());
        }
    }

    /**
     * Installs a single update: download, verify, move the same-name file aside, move the new jar
     * in, and only then retire the old one.
     */
    public Outcome installOne(ModUpdate update, Downloader downloader) {
        Path target = modsDir.resolve(update.candidate().fileName()).normalize();
        if (!target.startsWith(modsDir) || target.equals(modsDir)) {
            return new Outcome(update, false, "file name escapes the mods folder");
        }

        Path temp = null;
        Path backup = null;
        try {
            Files.createDirectories(modsDir);
            temp = Files.createTempFile(modsDir, ".cleanmcmod-", ".tmp");
            downloader.download(update.candidate().downloadUrl(), temp);

            String actual = ModScanner.sha1(temp);
            if (update.candidate().checksummed()
                    && !actual.equalsIgnoreCase(update.candidate().sha1())) {
                throw new IOException("downloaded file does not match the published checksum");
            }

            if (Files.exists(target)) {
                backup = target.resolveSibling(target.getFileName() + ".old");
                Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                if (backup != null) {
                    Files.move(backup, target, StandardCopyOption.REPLACE_EXISTING);
                }
                throw e;
            }
            temp = null;
            if (backup != null) {
                Files.deleteIfExists(backup);
            }

            Path previous = update.local().path().toAbsolutePath().normalize();
            if (!previous.equals(target)) {
                queueRemoval(previous);
            }
            return new Outcome(update, true, null);
        } catch (IOException | RuntimeException e) {
            deleteQuietly(temp);
            return new Outcome(update, false, message(e));
        }
    }

    /**
     * Checks a download against the hash its source publishes. CurseForge and GitHub publish none,
     * so those jars are accepted unverified - and that is logged, never silently pretended.
     */
    private static void verify(ModVersion version, Path temp) throws IOException {
        if (!version.checksummed()) {
            CleanMcMod.LOGGER.info("{} publishes no checksum for {}; installed unverified",
                    version.source(), version.fileName());
            return;
        }
        if (!ModScanner.sha1(temp).equalsIgnoreCase(version.sha1())) {
            throw new IOException(version.fileName() + ": downloaded file does not match the published checksum");
        }
    }

    /** Size and speed of a finished download, as the one line the update screen prints. */
    private static String describe(Path temp, long elapsedMillis) {
        try {
            long bytes = Files.size(temp);
            long perSecond = bytes * 1000L / Math.max(1L, elapsedMillis);
            return megabytes(bytes) + " MB · " + megabytes(perSecond) + " MB/s";
        } catch (IOException e) {
            return "";
        }
    }

    private static String megabytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f", bytes / (1024.0 * 1024.0));
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            CleanMcMod.LOGGER.debug("Could not delete temporary download {}", file);
        }
    }

    static String message(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
