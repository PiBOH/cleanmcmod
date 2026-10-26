package piboh.cleanmc.mod.clean;

import piboh.cleanmc.mod.CleanMcMod;
import piboh.cleanmc.mod.config.CleanMcModConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Turns the configuration into the concrete list of cache targets for this game directory. */
public final class CacheTargets {

    public static final String VANILLA_LOGS = "vanilla_logs";
    public static final String CRASH_REPORTS = "crash_reports";
    public static final String DEBUG_DUMPS = "debug_dumps";
    public static final String SERVER_RESOURCE_PACKS = "server_resource_packs";
    public static final String FABRIC_REMAPPED_JARS = "fabric_remapped_jars";
    public static final String MIXIN_DEBUG_OUTPUT = "mixin_debug_output";
    public static final String ASSET_INDEXES = "asset_indexes";
    public static final String ASSET_OBJECTS = "asset_objects";

    private CacheTargets() {
    }

    public static List<CacheTarget> forConfig(Path gameDir, CleanMcModConfig config, CacheAutoDetector detector) {
        Path root = gameDir.toAbsolutePath().normalize();
        List<CacheTarget> targets = new ArrayList<>();

        if (config.includeLogs) {
            targets.add(builtin(root, VANILLA_LOGS, CacheCategory.VANILLA, CacheSafety.SAFE, "logs"));
        }
        if (config.includeCrashReports) {
            targets.add(builtin(root, CRASH_REPORTS, CacheCategory.VANILLA, CacheSafety.SAFE, "crash-reports"));
        }
        if (config.includeDebugDumps) {
            targets.add(builtin(root, DEBUG_DUMPS, CacheCategory.VANILLA, CacheSafety.SAFE, "debug"));
        }
        if (config.includeServerResourcePacks) {
            targets.add(builtin(root, SERVER_RESOURCE_PACKS, CacheCategory.RESOURCE_PACK,
                    CacheSafety.SAFE, "server-resource-packs"));
        }
        if (config.includeLoaderCaches) {
            targets.add(builtin(root, FABRIC_REMAPPED_JARS, CacheCategory.LOADER, CacheSafety.SAFE,
                    ".fabric/remappedJars"));
            targets.add(builtin(root, MIXIN_DEBUG_OUTPUT, CacheCategory.LOADER, CacheSafety.SAFE, ".mixin.out"));
        }
        if (config.includeAssetCache) {
            targets.add(builtin(root, ASSET_INDEXES, CacheCategory.VANILLA, CacheSafety.REQUIRES_REDOWNLOAD,
                    "assets/indexes"));
            targets.add(builtin(root, ASSET_OBJECTS, CacheCategory.VANILLA, CacheSafety.REQUIRES_REDOWNLOAD,
                    "assets/objects"));
        }

        if (config.autoDetect) {
            for (CacheTarget candidate : detector.detect(root)) {
                if (!isCovered(targets, candidate.path())) {
                    targets.add(candidate);
                }
            }
        }

        for (String raw : config.extraPaths()) {
            Path resolved = resolveExtra(root, raw);
            if (resolved != null && !isCovered(targets, resolved)) {
                targets.add(CacheTarget.detected(
                        "custom_" + raw.replaceAll("[^A-Za-z0-9]+", "_"),
                        CacheCategory.CUSTOM,
                        CacheSafety.SAFE,
                        resolved,
                        raw));
            }
        }

        return List.copyOf(targets);
    }

    private static CacheTarget builtin(Path root, String id, CacheCategory category, CacheSafety safety, String relative) {
        return CacheTarget.builtin(id, category, safety, root.resolve(relative));
    }

    /** Accepts a path relative to the game directory; absolute paths and escapes outside it are rejected. */
    static Path resolveExtra(Path root, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Path candidate;
        try {
            candidate = root.resolve(raw).normalize().toAbsolutePath();
        } catch (RuntimeException e) {
            CleanMcMod.LOGGER.warn("Ignoring invalid extra cache path '{}': {}", raw, e.toString());
            return null;
        }
        if (candidate.equals(root) || !candidate.startsWith(root)) {
            CleanMcMod.LOGGER.warn("Ignoring extra cache path '{}': outside the game directory", raw);
            return null;
        }
        return candidate;
    }

    private static boolean isCovered(List<CacheTarget> existing, Path candidate) {
        for (CacheTarget target : existing) {
            if (candidate.startsWith(target.path()) || target.path().startsWith(candidate)) {
                return true;
            }
        }
        return false;
    }
}
