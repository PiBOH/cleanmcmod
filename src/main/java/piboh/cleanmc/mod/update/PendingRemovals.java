package piboh.cleanmc.mod.update;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The jars an update replaced, waiting for a launch in which the game does not hold them open.
 *
 * <p>Every jar in {@code mods/} is open for the whole session, so Windows refuses to delete or
 * rename one while Minecraft runs. The install writes the new build under its own file name and
 * leaves the old jar to this list, exactly like Mod Menu Updater's cleanup file: the next launch
 * moves it out of {@code mods/} into {@code config/cleanmcmod/backups/}. Nothing is deleted, so the
 * previous build stays recoverable.</p>
 */
public final class PendingRemovals {

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    private PendingRemovals() {
    }

    private static Path file(Path configDir) {
        return configDir.resolve("cleanmcmod").resolve("pending.json");
    }

    /** Queues a jar for removal on the next launch. Locked jars are the reason this exists. */
    public static synchronized void schedule(Path jar, Path configDir) {
        if (jar == null) {
            return;
        }
        String path = jar.toAbsolutePath().normalize().toString();
        List<String> queued = read(configDir);
        if (queued.contains(path)) {
            return;
        }
        queued.add(path);
        write(configDir, queued);
        CleanMcMod.LOGGER.info("Queued {} for removal on the next launch", jar.getFileName());
    }

    /** The jars still waiting, oldest first. */
    public static synchronized List<Path> pending(Path configDir) {
        List<Path> jars = new ArrayList<>();
        for (String entry : read(configDir)) {
            jars.add(Paths.get(entry));
        }
        return List.copyOf(jars);
    }

    /**
     * Moves every queued jar out of {@code mods/}. A jar the game opened again before this ran stays
     * queued, so nothing is ever lost to a lock.
     */
    public static synchronized void run(Path configDir) {
        List<String> queued = read(configDir);
        if (queued.isEmpty()) {
            return;
        }
        Path backups = configDir.resolve("cleanmcmod").resolve("backups");
        List<String> still = new ArrayList<>();
        for (String entry : queued) {
            Path jar = Paths.get(entry);
            if (!Files.exists(jar)) {
                continue;
            }
            try {
                Files.createDirectories(backups);
                Files.move(jar, backups.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                CleanMcMod.LOGGER.info("Moved the replaced jar {} into cleanmcmod/backups", jar.getFileName());
            } catch (IOException locked) {
                still.add(entry);
            }
        }
        write(configDir, still);
        if (!still.isEmpty()) {
            CleanMcMod.LOGGER.warn("{} replaced jars are still in use and stay queued", still.size());
        }
    }

    private static List<String> read(Path configDir) {
        Path file = file(configDir);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<String> paths = GSON.fromJson(reader, LIST_TYPE);
            return paths == null ? new ArrayList<>() : new ArrayList<>(paths);
        } catch (IOException | RuntimeException e) {
            CleanMcMod.LOGGER.warn("Could not read {}: {}", file, e.toString());
            return new ArrayList<>();
        }
    }

    private static void write(Path configDir, List<String> paths) {
        Path file = file(configDir);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(paths, writer);
            }
        } catch (IOException e) {
            CleanMcMod.LOGGER.warn("Could not write {}: {}", file, e.toString());
        }
    }
}
