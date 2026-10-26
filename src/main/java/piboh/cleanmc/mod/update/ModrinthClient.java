package piboh.cleanmc.mod.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Talks to the public Modrinth API.
 *
 * <p>Installed jars are identified by their SHA-1 through {@code /version_files}, which is the
 * documented and exact way to match a local file to a project — no name guessing. The same hashes
 * go to {@code /version_files/update} with the running game version and loaders so Modrinth picks
 * the compatible version. Every call is blocking; callers must stay off the render thread.</p>
 */
public final class ModrinthClient implements ModInstaller.Downloader {

    private static final String API = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "PiBOH/cleanmcmod (github.com/PiBOH/cleanmcmod)";
    private static final int MAX_ITEMS_PER_REQUEST = 100;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient http;
    private final String gameVersion;
    private final List<String> loaders;

    public ModrinthClient(String gameVersion, List<String> loaders) {
        this.gameVersion = gameVersion;
        this.loaders = List.copyOf(loaders);
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<ModUpdate> check(List<LocalMod> mods) throws IOException, InterruptedException {
        if (mods.isEmpty()) {
            return List.of();
        }
        Map<String, ModVersion> current = new HashMap<>();
        Map<String, ModVersion> latest = new HashMap<>();
        for (int start = 0; start < mods.size(); start += MAX_ITEMS_PER_REQUEST) {
            List<String> hashes = mods.subList(start, Math.min(mods.size(), start + MAX_ITEMS_PER_REQUEST))
                    .stream()
                    .map(LocalMod::sha1)
                    .toList();
            current.putAll(parseVersions(post("/version_files", requestBody(hashes, null, null))));
            latest.putAll(parseVersions(post("/version_files/update",
                    requestBody(hashes, loaders, List.of(gameVersion)))));
        }
        List<String> dependencyProjects = new ArrayList<>();
        for (ModVersion version : latest.values()) {
            for (Dependency dependency : version.dependencies()) {
                dependencyProjects.add(dependency.projectId());
            }
        }
        return Updates.plan(mods, current, latest, gameVersion, loaders,
                titles(latest.values(), dependencyProjects));
    }

    /**
     * The newest version of a project published for this game version and loader.
     *
     * @return null when the project has no such version
     */
    public ModVersion resolve(String projectId) throws IOException, InterruptedException {
        String query = "/project/" + URLEncoder.encode(projectId, StandardCharsets.UTF_8) + "/version"
                + "?game_versions=" + URLEncoder.encode("[" + quote(gameVersion) + "]", StandardCharsets.UTF_8)
                + "&loaders=" + URLEncoder.encode(list(loaders), StandardCharsets.UTF_8);
        return pickCompatible(get(query), gameVersion, loaders);
    }

    /** The first version in a {@code /project/{id}/version} payload that matches game and loader. */
    static ModVersion pickCompatible(String json, String gameVersion, Collection<String> loaders) {
        for (JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject version = element.getAsJsonObject();
            ModVersion candidate = toVersion(version, primaryFile(version));
            if (candidate != null && candidate.matches(gameVersion, loaders)) {
                return candidate;
            }
        }
        return null;
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }

    private static String list(Collection<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (String value : values) {
            if (out.length() > 1) {
                out.append(',');
            }
            out.append(quote(value));
        }
        return out.append(']').toString();
    }

    @Override
    public void download(String url, Path target) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<Path> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofFile(target,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
        if (response.statusCode() != 200) {
            Files.deleteIfExists(target);
            throw new IOException("Download failed: HTTP " + response.statusCode());
        }
    }

    /** The JSON body sent to {@code /version_files} and {@code /version_files/update}. */
    static String requestBody(List<String> hashes, List<String> loaders, List<String> gameVersions) {
        JsonObject body = new JsonObject();
        JsonArray hashArray = new JsonArray();
        hashes.forEach(hashArray::add);
        body.add("hashes", hashArray);
        body.addProperty("algorithm", "sha1");
        if (loaders != null) {
            JsonArray loaderArray = new JsonArray();
            loaders.forEach(loaderArray::add);
            body.add("loaders", loaderArray);
        }
        if (gameVersions != null) {
            JsonArray gameVersionArray = new JsonArray();
            gameVersions.forEach(gameVersionArray::add);
            body.add("game_versions", gameVersionArray);
        }
        return body.toString();
    }

    private String post(String path, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + path))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        requireSuccess(response, path);
        return response.body();
    }

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + path))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        requireSuccess(response, path);
        return response.body();
    }

    private static void requireSuccess(HttpResponse<String> response, String path) throws IOException {
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Modrinth " + path + " returned HTTP " + response.statusCode());
        }
    }

    private Map<String, String> titles(Collection<ModVersion> versions, Collection<String> extra)
            throws IOException, InterruptedException {
        List<String> projectIds = new ArrayList<>(new LinkedHashSet<>(
                versions.stream().map(ModVersion::projectId).toList()));
        extra.stream().filter(id -> !projectIds.contains(id)).forEach(projectIds::add);
        Map<String, String> titles = new HashMap<>();
        for (int start = 0; start < projectIds.size(); start += MAX_ITEMS_PER_REQUEST) {
            List<String> chunk = projectIds.subList(start, Math.min(projectIds.size(), start + MAX_ITEMS_PER_REQUEST));
            JsonArray ids = new JsonArray();
            chunk.forEach(ids::add);
            String encoded = URLEncoder.encode(ids.toString(), StandardCharsets.UTF_8);
            for (JsonElement element : JsonParser.parseString(get("/projects?ids=" + encoded)).getAsJsonArray()) {
                JsonObject project = element.getAsJsonObject();
                String id = text(project, "id");
                String title = text(project, "title");
                if (id != null && title != null) {
                    titles.put(id, title);
                }
            }
        }
        return titles;
    }

    static Map<String, ModVersion> parseVersions(String json) {
        Map<String, ModVersion> parsed = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : JsonParser.parseString(json).getAsJsonObject().entrySet()) {
            ModVersion parsedVersion = toVersion(entry.getValue().getAsJsonObject(),
                    primaryFile(entry.getValue().getAsJsonObject()));
            if (parsedVersion != null) {
                parsed.put(entry.getKey(), parsedVersion);
            }
        }
        return parsed;
    }

    /** One version object plus its primary file, or null when a field we need is missing. */
    private static ModVersion toVersion(JsonObject version, JsonObject file) {
        if (file == null) {
            return null;
        }
        JsonObject hashes = file.has("hashes") && file.get("hashes").isJsonObject()
                ? file.getAsJsonObject("hashes")
                : null;
        String id = text(version, "id");
        String projectId = text(version, "project_id");
        String versionNumber = text(version, "version_number");
        String fileName = text(file, "filename");
        String url = text(file, "url");
        String sha1 = hashes == null ? null : text(hashes, "sha1");
        if (id == null || projectId == null || versionNumber == null
                || fileName == null || url == null || sha1 == null) {
            return null;
        }
        return new ModVersion(id, projectId, versionNumber, fileName, url, sha1,
                strings(version.has("game_versions") ? version.getAsJsonArray("game_versions") : null),
                strings(version.has("loaders") ? version.getAsJsonArray("loaders") : null),
                dependencies(version));
    }

    /** The {@code dependencies} array of a version, as Modrinth publishes it. */
    private static List<Dependency> dependencies(JsonObject version) {
        if (!version.has("dependencies") || !version.get("dependencies").isJsonArray()) {
            return List.of();
        }
        List<Dependency> dependencies = new ArrayList<>();
        for (JsonElement element : version.getAsJsonArray("dependencies")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject dependency = element.getAsJsonObject();
            String projectId = text(dependency, "project_id");
            if (projectId == null) {
                continue;
            }
            dependencies.add(new Dependency(projectId, text(dependency, "version_id"),
                    text(dependency, "dependency_type")));
        }
        return dependencies;
    }

    private static JsonObject primaryFile(JsonObject version) {
        JsonArray files = version.has("files") && version.get("files").isJsonArray()
                ? version.getAsJsonArray("files")
                : null;
        if (files == null || files.isEmpty()) {
            return null;
        }
        for (JsonElement element : files) {
            JsonObject file = element.getAsJsonObject();
            if (file.has("primary") && file.get("primary").getAsBoolean()) {
                return file;
            }
        }
        return files.get(0).getAsJsonObject();
    }

    private static List<String> strings(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static String text(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }
}
