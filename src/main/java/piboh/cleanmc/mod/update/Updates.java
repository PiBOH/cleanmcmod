package piboh.cleanmc.mod.update;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The decision that turns Modrinth answers into the list the screen shows.
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
     * One row per installed jar when several sources each know the same mod: the highest published
     * version wins, and at the same version the source earlier in {@link ModSource} wins, so an
     * update that carries a published checksum is preferred.
     */
    public static List<ModUpdate> bestPerMod(List<ModUpdate> candidates) {
        Map<Path, ModUpdate> best = new LinkedHashMap<>();
        for (ModUpdate candidate : candidates) {
            ModUpdate current = best.get(candidate.local().path());
            if (current == null) {
                best.put(candidate.local().path(), candidate);
                continue;
            }
            int compare = VersionComparator.compare(candidate.availableVersion(), current.availableVersion());
            if (compare > 0 || (compare == 0
                    && candidate.candidate().source().prefers(current.candidate().source()))) {
                best.put(candidate.local().path(), candidate);
            }
        }
        List<ModUpdate> result = new ArrayList<>(best.values());
        result.sort(Comparator.comparing(ModUpdate::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
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
