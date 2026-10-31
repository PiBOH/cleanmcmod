package piboh.cleanmc.mod.update;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The decision that turns source answers into the list the screen shows.
 *
 * <p>Deliberately pure: no files, no HTTP, so the "is this an update" rules are testable. Modrinth
 * already filters by game version and loader; the same filter is applied again here so a wrong or
 * stale response can never offer an incompatible jar.</p>
 */
public final class Updates {

    private Updates() {
    }

    public static List<ModUpdate> plan(
            List<LocalMod> mods,
            Map<String, ModVersion> currentByHash,
            Map<String, ModVersion> latestByHash,
            String gameVersion,
            Collection<String> loaders,
            Map<String, String> titles) {

        Map<String, String> projectIdByModId = new HashMap<>();
        for (LocalMod mod : mods) {
            ModVersion known = currentByHash.get(mod.sha1());
            if (known != null && mod.modId() != null) {
                projectIdByModId.put(mod.modId(), known.projectId());
            }
        }

        List<ModUpdate> updates = new ArrayList<>();
        for (LocalMod mod : mods) {
            ModVersion current = currentByHash.get(mod.sha1());
            ModVersion latest = latestByHash.get(mod.sha1());
            String title = latest == null ? null : titles.get(latest.projectId());
            decide(mod, current, latest, gameVersion, loaders, title)
                    .map(update -> new ModUpdate(update.local(), update.current(), update.candidate(),
                            update.title(), Compatibility.check(mod, update.candidate(), mods,
                                    currentByHash, projectIdByModId, titles)))
                    .ifPresent(updates::add);
        }
        return List.copyOf(updates);
    }

    /**
     * One row per installed mod, never two.
     *
     * <p>Two jars of the same mod in {@code mods/} (an old copy next to the new one), or the same
     * file published twice by one source, must not produce two rows for the same name: the newest
     * build wins and the older copy is not offered at all. At equal versions the source earlier in
     * {@link ModSource} wins, so an update that carries a published checksum is preferred.</p>
     */
    public static List<ModUpdate> bestPerMod(List<ModUpdate> candidates) {
        Map<Path, ModUpdate> bestPerJar = new LinkedHashMap<>();
        for (ModUpdate candidate : candidates) {
            ModUpdate current = bestPerJar.get(candidate.local().path());
            if (current == null || newer(candidate, current)) {
                bestPerJar.put(candidate.local().path(), candidate);
            }
        }

        Map<String, ModUpdate> bestPerMod = new LinkedHashMap<>();
        for (ModUpdate candidate : bestPerJar.values()) {
            String key = duplicateKey(candidate);
            ModUpdate current = bestPerMod.get(key);
            if (current == null || newer(candidate, current)) {
                bestPerMod.put(key, candidate);
            }
        }

        List<ModUpdate> result = new ArrayList<>(bestPerMod.values());
        result.sort(Comparator.comparing(ModUpdate::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    /** The same mod installed twice is identified by its mod id, or by its name when it has none. */
    private static String duplicateKey(ModUpdate update) {
        String id = update.local().modId();
        if (id == null || id.isBlank()) {
            id = update.displayName();
        }
        return id.toLowerCase(Locale.ROOT);
    }

    private static boolean newer(ModUpdate candidate, ModUpdate current) {
        int available = VersionComparator.compare(candidate.availableVersion(), current.availableVersion());
        if (available != 0) {
            return available > 0;
        }
        int installed = VersionComparator.compare(candidate.installedVersion(), current.installedVersion());
        if (installed != 0) {
            return installed > 0;
        }
        return candidate.candidate().source().prefers(current.candidate().source());
    }

    static Optional<ModUpdate> decide(
            LocalMod mod,
            ModVersion current,
            ModVersion candidate,
            String gameVersion,
            Collection<String> loaders,
            String title) {

        if (current == null || candidate == null) {
            return Optional.empty();
        }
        if (!candidate.matches(gameVersion, loaders)) {
            return Optional.empty();
        }
        if (candidate.id().equals(current.id())) {
            return Optional.empty();
        }
        // An older number is never offered. The same number may come from a different build (the
        // 1.21.8 jar of a version whose 1.20.1 jar is installed), and that is a real update.
        if (VersionComparator.compare(candidate.versionNumber(), current.versionNumber()) < 0) {
            return Optional.empty();
        }
        return Optional.of(new ModUpdate(mod, current, candidate, title));
    }
}
