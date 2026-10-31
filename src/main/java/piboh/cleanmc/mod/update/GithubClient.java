package piboh.cleanmc.mod.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Updates from the GitHub releases of the repository a mod names in its own metadata.
 *
 * <p>GitHub publishes no per-file checksum for release assets, so these downloads cannot be
 * verified; the jar is only offered because the mod itself points at that repository. Matching is
 * done on the asset name: it must be a jar, must not name another loader, and must name the
 * running game version — or, when the release has exactly one jar, the release tag must.</p>
 */
public final class GithubClient {

    private static final String API = "https://api.github.com/repos/";
    private static final String USER_AGENT = "PiBOH/cleanmcmod (github.com/PiBOH/cleanmcmod)";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    /** A repository per mod is cheap, but a pathological mods folder must not stall the check. */
    private static final int MAX_LOOKUPS = 40;

    /** @param versionNumber the release tag without a leading {@code v} */
    public record Asset(String versionNumber, String fileName, String url) {
    }

    private final HttpClient http;
    private final String gameVersion;
    private final List<String> loaders;

    public GithubClient(String gameVersion, List<String> loaders) {
        this.gameVersion = gameVersion;
        this.loaders = List.copyOf(loaders);
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<ModUpdate> check(List<LocalMod> mods) throws IOException, InterruptedException {
        List<ModUpdate> updates = new ArrayList<>();
        int lookups = 0;
        for (LocalMod mod : mods) {
            if (!mod.hasGithubRepo()) {
                continue;
            }
            if (lookups++ >= MAX_LOOKUPS) {
                CleanMcMod.LOGGER.info("GitHub: stopped after {} repositories", MAX_LOOKUPS);
                break;
            }
            Asset asset;
            try {
                asset = pick(get(API + mod.githubRepo() + "/releases?per_page=20"), gameVersion, loaders);
            } catch (IOException e) {
                // A repository without releases, a renamed repo or the hourly rate limit: the other
                // sources still stand.
                CleanMcMod.LOGGER.debug("GitHub {}: {}", mod.githubRepo(), e.toString());
                continue;
            }
            if (asset == null) {
                continue;
            }
            ModVersion current = new ModVersion("github:current:" + mod.sha1(), mod.githubRepo(),
                    mod.version() == null || mod.version().isBlank() ? "0" : mod.version(),
                    mod.fileName(), "", null, List.of(gameVersion), loaders);
            ModVersion candidate = new ModVersion(
                    "github:" + asset.versionNumber() + ":" + asset.fileName(), mod.githubRepo(),
                    asset.versionNumber(), asset.fileName(), asset.url(), null,
                    List.of(gameVersion), loaders, List.of(), ModSource.GITHUB);
            Updates.decide(mod, current, candidate, gameVersion, loaders, mod.displayName())
                    .ifPresent(update -> updates.add(new ModUpdate(update.local(), update.current(),
                            update.candidate(), update.title(),
                            Compatibility.check(mod, candidate, mods, java.util.Map.of(),
                                    java.util.Map.of(), java.util.Map.of()))));
        }
        return List.copyOf(updates);
    }

    /** The newest release asset published for this game version, or null when there is none. */
    static Asset pick(String json, String gameVersion, List<String> loaders) {
        JsonArray releases;
        try {
            releases = JsonParser.parseString(json).getAsJsonArray();
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }
        for (JsonElement element : releases) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject release = element.getAsJsonObject();
            if (booleanValue(release, "draft")) {
                continue;
            }
            Asset asset = pickFromRelease(release, gameVersion, loaders);
            if (asset != null) {
                return asset;
            }
        }
        return null;
    }

    private static Asset pickFromRelease(JsonObject release, String gameVersion, List<String> loaders) {
        if (!release.has("assets") || !release.get("assets").isJsonArray()) {
            return null;
        }
        String tag = text(release, "tag_name");
        if (tag == null) {
            return null;
        }
        String version = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
        JsonArray assets = release.getAsJsonArray("assets");

        List<JsonObject> jars = new ArrayList<>();
        for (JsonElement element : assets) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject asset = element.getAsJsonObject();
            if (ArtifactNames.looksLikeJar(text(asset, "name"))
                    && ArtifactNames.namesLoader(text(asset, "name"), loaders)) {
                jars.add(asset);
            }
        }
        for (JsonObject jar : jars) {
            String name = text(jar, "name");
            if (ArtifactNames.namesGameVersion(name, gameVersion)) {
                return toAsset(version, jar);
            }
        }
        // A repository can publish one jar per release and put the game version in the tag only.
        JsonObject single = onlyJar(assets);
        if (single != null && ArtifactNames.namesGameVersion(tag, gameVersion)
                && ArtifactNames.namesLoader(text(single, "name"), loaders)) {
            return toAsset(version, single);
        }
        for (JsonObject jar : jars) {
            if (ArtifactNames.namesGameVersion(tag, gameVersion)) {
                return toAsset(version, jar);
            }
        }
        return null;
    }

    private static JsonObject onlyJar(JsonArray assets) {
        JsonObject found = null;
        for (JsonElement element : assets) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject asset = element.getAsJsonObject();
            if (ArtifactNames.looksLikeJar(text(asset, "name"))) {
                if (found != null) {
                    return null;
                }
                found = asset;
            }
        }
        return found;
    }

    private static Asset toAsset(String version, JsonObject jar) {
        String name = text(jar, "name");
        String url = text(jar, "browser_download_url");
        return name == null || url == null ? null : new Asset(version, name, url);
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.github+json")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("GitHub returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static boolean booleanValue(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsBoolean();
    }

    private static String text(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString().toLowerCase(Locale.ROOT).isEmpty() ? null
                : object.get(key).getAsString()
                : null;
    }
}
