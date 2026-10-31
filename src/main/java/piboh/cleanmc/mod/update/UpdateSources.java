package piboh.cleanmc.mod.update;

import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The update check as a whole: Modrinth, GitHub and CurseForge asked in that order, merged into one
 * row per installed mod.
 *
 * <p>One source failing never hides the others. When every source fails the check fails, so the
 * screen can say so instead of claiming everything is up to date. Progress is reported per source,
 * which is the only step this class can genuinely measure.</p>
 */
public final class UpdateSources implements ModUpdaterService.Checker, ModUpdaterService.ProgressAware {

    /** One named source, so a failure can be attributed to it. */
    public record Source(ModSource id, ModUpdaterService.Checker checker) {
    }

    /** A source that could not be asked. */
    public record Failure(ModSource source, String message) {
    }

    private final List<Source> sources;

    private volatile List<Failure> failures = List.of();
    private volatile BiConsumer<Integer, Integer> progress = (done, total) -> {
    };

    public UpdateSources(String gameVersion, List<String> loaders, ModrinthClient modrinth) {
        this(List.of(
                new Source(ModSource.MODRINTH, modrinth::check),
                new Source(ModSource.GITHUB, new GithubClient(gameVersion, loaders)::check),
                new Source(ModSource.CURSEFORGE, new CurseForgeClient(gameVersion, loaders)::check)));
    }

    public UpdateSources(List<Source> sources) {
        this.sources = List.copyOf(sources);
    }

    /** The sources that could not be asked during the last check. */
    public List<Failure> failures() {
        return failures;
    }

    @Override
    public void progressListener(BiConsumer<Integer, Integer> listener) {
        this.progress = listener == null ? (done, total) -> {
        } : listener;
    }

    @Override
    public List<ModUpdate> check(List<LocalMod> mods) throws IOException, InterruptedException {
        List<ModUpdate> found = new ArrayList<>();
        List<Failure> failed = new ArrayList<>();
        int total = sources.size();
        progress.accept(0, total);
        for (int index = 0; index < sources.size(); index++) {
            Source source = sources.get(index);
            try {
                found.addAll(source.checker().check(mods));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failed.add(new Failure(source.id(), "interrupted"));
                break;
            } catch (Exception e) {
                CleanMcMod.LOGGER.warn("{} update check failed: {}", source.id(), e.toString());
                failed.add(new Failure(source.id(), ModInstaller.message(e)));
            }
            progress.accept(index + 1, total);
        }
        failures = List.copyOf(failed);
        if (found.isEmpty() && !sources.isEmpty() && failed.size() == sources.size()) {
            throw new IOException(failed.get(0).message());
        }
        return Updates.bestPerMod(found);
    }
}
