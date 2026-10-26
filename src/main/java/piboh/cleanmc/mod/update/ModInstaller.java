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
     * is downloaded and hash-checked first, so a failure leaves {@code mods/} untouched.
     */
    public List<Outcome> installTogether(List<ModUpdate> updates, List<ModVersion> dependencies,
            Downloader downloader) {
        // A mod and its dependency can both be in the update list, and the dependency can be
        // resolved for the mod too: the same build is one jar, downloaded once.
        Map<String, ModVersion> unique = new LinkedHashMap<>();
        for (ModVersion version : dependencies) {
            unique.putIfAbsent(version.sha1(), version);
        }
        for (ModUpdate update : updates) {
            unique.putIfAbsent(update.candidate().sha1(), update.candidate());
        }
        List<ModVersion> jars = new ArrayList<>(unique.values());

        List<Prepared> prepared = new ArrayList<>(jars.size());
        try {
            Files.createDirectories(modsDir);
            for (ModVersion version : jars) {
                if (!modsDir.resolve(version.fileName()).normalize().startsWith(modsDir)) {
                    throw new IOException("file name escapes the mods folder: " + version.fileName());
                }
                Path temp = Files.createTempFile(modsDir, ".cleanmcmod-", ".tmp");
                prepared.add(new Prepared(version, temp));
                downloader.download(version.downloadUrl(), temp);
                if (!ModScanner.sha1(temp).equalsIgnoreCase(version.sha1())) {
                    throw new IOException("downloaded file does not match the published checksum");
                }
            }
        } catch (IOException | RuntimeException e) {
            discard(prepared);
            String reason = message(e);
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
                for (ModUpdate update : updates) {
                    outcomes.add(new Outcome(update, false, message(e)));
                }
                return outcomes;
            }
        }
        for (ModUpdate update : updates) {
            outcomes.add(new Outcome(update, true, removeObsolete(update)));
        }
        return outcomes;
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
            if (!actual.equalsIgnoreCase(update.candidate().sha1())) {
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

    static String message(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
