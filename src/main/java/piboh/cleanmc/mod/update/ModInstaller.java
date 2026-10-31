package piboh.cleanmc.mod.update;

import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes a downloaded update into {@code mods/} without ever losing the working jar first.
 *
 * <p>Sequence: download into a temporary file in the same folder, verify its SHA-1 against
 * Modrinth, move any same-name file aside, move the verified temp file into place, and only then
 * remove the old jar. Any failure deletes the temp file and restores whatever was moved, so the
 * folder is unchanged. The obsolete jar, if the game holds it open, is renamed to {@code *.old}
 * (which the game does not load) instead of being left as a second version.</p>
 */
public final class ModInstaller {

    public interface Downloader {
        void download(String url, Path target) throws IOException;
    }

    /**
     * @param detail on failure the reason; on success with a non-null detail, a leftover file the
     *               user has to delete by hand
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
     *
     * <p>One update whose own jar cannot be fetched fails alone: the other mods in the request are
     * still installed, and the alert names the file that could not be downloaded. A dependency is
     * different - installing a mod without the library it requires is the half-install the batch
     * exists to prevent, so a missing dependency cancels everything.</p>
     */
    public List<Outcome> installTogether(List<ModUpdate> updates, List<ModVersion> dependencies,
            Downloader downloader) {
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
                    fetch(downloader, version, temp);
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
            String reason = message(e);
            CleanMcMod.LOGGER.warn("Mod update batch cancelled, mods folder unchanged: {}", reason);
            List<Outcome> failures = new ArrayList<>(updates.size());
            for (ModUpdate update : updates) {
                failures.add(new Outcome(update, false, reason));
            }
            return failures;
        }

        List<Outcome> outcomes = new ArrayList<>(updates.size());
        for (Prepared entry : prepared) {
            try {
                Files.move(entry.temp(), modsDir.resolve(entry.version().fileName()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                discard(prepared);
                CleanMcMod.LOGGER.warn("Could not write {} into the mods folder: {}",
                        entry.version().fileName(), message(e));
                for (ModUpdate update : updates) {
                    outcomes.add(new Outcome(update, false, message(e)));
                }
                return outcomes;
            }
        }
        for (ModUpdate update : updates) {
            String reason = broken.get(update.candidate().fileKey());
            outcomes.add(reason == null
                    ? new Outcome(update, true, removeObsolete(update))
                    : new Outcome(update, false, reason));
        }
        return outcomes;
    }

    /**
     * Downloads one jar, retrying once: a dropped connection or a 5xx from the CDN is not worth
     * failing the whole batch for. The reason names the file, so the alert can say which mod could
     * not be fetched instead of a bare "download failed".
     */
    private static void fetch(Downloader downloader, ModVersion version, Path temp) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                downloader.download(version.downloadUrl(), temp);
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

    /** Deletes the jar an update replaced; a locked jar is renamed to {@code *.old} instead. */
    private String removeObsolete(ModUpdate update) {
        Path previous = update.local().path().toAbsolutePath().normalize();
        if (previous.equals(modsDir.resolve(update.candidate().fileName()))) {
            return null;
        }
        return retire(previous);
    }

    /**
     * Removes a jar a just-installed dependency replaces, so {@code mods/} never holds two builds
     * of the same mod. A jar the game holds open is renamed to {@code *.old}.
     *
     * @return the file name left behind, or null when the jar is gone
     */
    String retire(Path jar) {
        try {
            Files.deleteIfExists(jar);
            return null;
        } catch (IOException locked) {
            try {
                Files.move(jar, jar.resolveSibling(jar.getFileName() + ".old"),
                        StandardCopyOption.REPLACE_EXISTING);
                return null;
            } catch (IOException renameFailed) {
                CleanMcMod.LOGGER.warn("Could not remove the old jar {}", jar);
                return jar.getFileName().toString();
            }
        }
    }

    private static void discard(List<Prepared> prepared) {
        for (Prepared entry : prepared) {
            try {
                Files.deleteIfExists(entry.temp());
            } catch (IOException ignored) {
                CleanMcMod.LOGGER.debug("Could not delete temporary download {}", entry.temp());
            }
        }
    }

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
                try {
                    Files.deleteIfExists(previous);
                } catch (IOException locked) {
                    try {
                        Files.move(previous, previous.resolveSibling(previous.getFileName() + ".old"),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException renameFailed) {
                        CleanMcMod.LOGGER.warn("Could not remove the old jar {}", previous);
                        return new Outcome(update, true, previous.getFileName().toString());
                    }
                }
            }
            return new Outcome(update, true, null);
        } catch (IOException | RuntimeException e) {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    CleanMcMod.LOGGER.debug("Could not delete temporary download {}", temp);
                }
            }
            return new Outcome(update, false, message(e));
        }
    }

    /**
     * Checks a download against the hash its source publishes. CurseForge and GitHub publish none,
     * so those jars are accepted unverified — and that is logged, never silently pretended.
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

    private static void deleteQuietly(Path file) {
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
