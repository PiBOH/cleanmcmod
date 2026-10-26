package piboh.cleanmc.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * User settings, stored as {@code config/cleanmcmod.json}.
 *
 * <p>The defaults only touch caches the game regenerates by itself. Anything that forces a
 * re-download is off until the user asks for it.</p>
 */
public final class CleanMcModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean includeLogs = true;
    public boolean includeCrashReports = true;
    public boolean includeDebugDumps = true;
    public boolean includeServerResourcePacks = true;
    public boolean includeLoaderCaches = true;
    public boolean includeAssetCache = false;
    public boolean autoDetect = true;
    public boolean dryRun = false;
    public List<String> extraPaths = new ArrayList<>();

    public List<String> extraPaths() {
        return extraPaths == null ? List.of() : List.copyOf(extraPaths);
    }

    public static CleanMcModConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            return new CleanMcModConfig();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            CleanMcModConfig config = GSON.fromJson(reader, CleanMcModConfig.class);
            return config == null ? new CleanMcModConfig() : config;
        } catch (IOException | JsonParseException e) {
            CleanMcMod.LOGGER.warn("Could not read config {}: {}", file, e.toString());
            return new CleanMcModConfig();
        }
    }

    public static void save(CleanMcModConfig config, Path file) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            CleanMcMod.LOGGER.warn("Could not write config {}: {}", file, e.toString());
        }
    }
}
