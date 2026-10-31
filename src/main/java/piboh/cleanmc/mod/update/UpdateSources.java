package piboh.cleanmc.mod.update;

import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The update check as a whole: Modrinth, CurseForge and GitHub asked in that order, merged into one
 * row per installed jar.
 *
 * <p>One source failing never hides the others. When every source fails the check fails, so the
 * screen can say so instead of claiming everything is up to date.</p>
 */
public final class UpdateSources implements ModUpdaterService.Checker {

    /** One named source, so a failure can be attributed to it. */
    public record Source(ModSource id, ModUpdaterService.Checker checker) {
    }

    /** A source that could not be asked. */
    public record Failure(ModSource source, String message) {
    }

    private final List<Source> sources;

    private volatile List<Failure> failures = List.of();

    public UpdateSources(String gameVersion, List<String> loaders, ModrinthClient modrinth) {
        this(List.of(
                new Source(ModSource.MODRINTH, modrinth::check),
                new Source(ModSource.CURSEFORGE, new CurseForgeClient(gameVersion, loaders)::check),
                new Source(ModSource.GITHUB, new GithubClient(gameVersion, loaders)::check)));
    }

    public UpdateSources(List<Source> sources) {
        this.sources = List.copyOf(sources);
    }

    /** The sources that could not be asked during the last check. */
    public List<Failure> failures() {
        return failures;
    }

    @Override
    public List<ModUpdate> check(List<LocalMod> mods) throws IOException, InterruptedException {
        List<ModUpdate> found = new ArrayList<>();
        List<Failure> failed = new ArrayList<>();
        for (Source source : sources) {
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
        }
        failures = List.copyOf(failed);
        if (found.isEmpty() && !sources.isEmpty() && failed.size() == sources.size()) {
            throw new IOException(failed.get(0).message());
        }
        return Updates.bestPerMod(found);
    }
}
