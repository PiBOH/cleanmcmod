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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates from CurseForge.
 *
 * <p>CurseForge's own API requires an API key, and a client mod must not ship one, so this reads
 * the public read-only mirror {@code api.cfwidget.com}, which serves the same file metadata. Only
 * the latest files are described there, so a mod that has not been published for the running game
 * version simply offers nothing — it is never offered a jar meant for another version.</p>
 *
 * <p>CurseForge publishes no hash per file. The download URL is the Forge CDN path the file id
 * encodes; it is built here instead of taken from the mirror because the mirror only links the
 * newest build of a project.</p>
 */
public final class CurseForgeClient {

    private static final String API = "https://api.cfwidget.com/minecraft/mc-mods/";
    private static final String CDN = "https://edge.forgecdn.net/files/";

    private static final Pattern VERSION = Pattern.compile("(\\d+(?:\\.\\d+)+(?:[-+][A-Za-z0-9.]+)?)");

    /** A file entry of a project, reduced to what an update needs. */
    public record CfFile(String id, String fileName, String versionNumber, String url, String uploadedAt) {
    }

    private final HttpClient http;
    private final String gameVersion;
    private final List<String> loaders;

    public CurseForgeClient(String gameVersion, List<String> loaders) {
        this.gameVersion = gameVersion;
        this.loaders = List.copyOf(loaders);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<ModUpdate> check(List<LocalMod> mods) throws IOException, InterruptedException {
        List<ModUpdate> updates = new ArrayList<>();
        int lookups = 0;
        for (LocalMod mod : mods) {
            String slug = mod.curseSlug();
            if (slug == null) {
                continue;
            }
            if (lookups++ >= 40) {
                CleanMcMod.LOGGER.info("CurseForge: stopped after 40 projects");
                break;
            }
            String json;
            try {
                json = get(API + slug);
            } catch (IOException e) {
                // Not on CurseForge, or the mirror is down: the other sources still stand.
                CleanMcMod.LOGGER.debug("CurseForge {}: {}", slug, e.toString());
                continue;
            }
            CfFile file = pick(json, gameVersion, loaders);
            if (file == null || file.url() == null) {
                continue;
            }
            String title = title(json, mod.displayName());
            ModVersion current = new ModVersion("curseforge:current:" + mod.sha1(), slug,
                    mod.version() == null || mod.version().isBlank() ? "0" : mod.version(),
                    mod.fileName(), "", null, List.of(gameVersion), loaders);
            ModVersion candidate = new ModVersion("curseforge:" + file.id(), slug, file.versionNumber(),
                    file.fileName(), file.url(), null, List.of(gameVersion), loaders, List.of(),
                    ModSource.CURSEFORGE);
            Updates.decide(mod, current, candidate, gameVersion, loaders, title)
                    .ifPresent(update -> updates.add(new ModUpdate(update.local(), update.current(),
                            update.candidate(), update.title(),
                            Compatibility.check(mod, candidate, mods, java.util.Map.of(),
                                    java.util.Map.of(), java.util.Map.of()))));
        }
        return List.copyOf(updates);
    }

    /** The newest published file that names this game version and loader, or null. */
    static CfFile pick(String json, String gameVersion, Collection<String> loaders) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }
        if (!root.has("files") || !root.get("files").isJsonArray()) {
            return null;
        }
        JsonArray files = root.getAsJsonArray("files");
        CfFile newest = null;
        for (JsonElement element : files) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject file = element.getAsJsonObject();
            String type = text(file, "type");
            if ("alpha".equalsIgnoreCase(type)) {
                continue;
            }
            String fileName = text(file, "name");
            if (!ArtifactNames.looksLikeJar(fileName) || !ArtifactNames.namesLoader(fileName, loaders)) {
                continue;
            }
            String id = text(file, "id");
            String uploaded = text(file, "uploaded_at");
            if (id == null || uploaded == null) {
                continue;
            }
            if (!namesGameVersion(file, fileName, gameVersion)) {
                continue;
            }
            if (newest != null && uploaded.compareTo(newest.uploadedAt()) <= 0) {
                continue;
            }
            String version = versionFrom(text(file, "display"), fileName);
            if (version == null) {
                continue;
            }
            newest = new CfFile(id, fileName, version, downloadUrl(id, fileName), uploaded);
        }
        return newest;
    }

    private static boolean namesGameVersion(JsonObject file, String fileName, String gameVersion) {
        if (file.has("versions") && file.get("versions").isJsonArray()) {
            for (JsonElement element : file.getAsJsonArray("versions")) {
                if (element.isJsonPrimitive() && gameVersion.equalsIgnoreCase(element.getAsString())) {
                    return true;
                }
            }
        }
        return ArtifactNames.namesGameVersion(fileName, gameVersion)
                || ArtifactNames.namesGameVersion(text(file, "display"), gameVersion);
    }

    /** "Sodium 0.8.15-beta.1 for Fabric 1.21.11" → "0.8.15-beta.1"; falls back to the jar name. */
    static String versionFrom(String display, String fileName) {
        if (display != null) {
            String head = display;
            int forIndex = head.toLowerCase(Locale.ROOT).indexOf(" for ");
            if (forIndex > 0) {
                head = head.substring(0, forIndex);
            }
            String[] parts = head.trim().split("\\s+");
            if (parts.length >= 2) {
                String candidate = parts[parts.length - 1];
                if (VERSION.matcher(candidate).find()) {
                    return candidate;
                }
            }
        }
        if (fileName == null) {
            return null;
        }
        Matcher matcher = VERSION.matcher(fileName.replaceAll("(?i)\\.jar$", ""));
        if (!matcher.find()) {
            return null;
        }
        // "mod-fabric-1.2.3+mc1.21.8" is version 1.2.3: the build metadata names the game.
        String found = matcher.group(1);
        int buildMetadata = found.indexOf('+');
        return buildMetadata > 0 ? found.substring(0, buildMetadata) : found;
    }

    /** The Forge CDN path of a file: {@code files/1234/567/name.jar} for file id {@code 1234567}. */
    static String downloadUrl(String fileId, String fileName) {
        if (fileId == null || fileId.length() < 4 || fileName == null) {
            return null;
        }
        String head = fileId.substring(0, fileId.length() - 3);
        String tail = fileId.substring(fileId.length() - 3);
        return CDN + head + "/" + tail + "/" + fileName;
    }

    private static String title(String json, String fallback) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String title = text(root, "title");
            return title == null || title.isBlank() ? fallback : title;
        } catch (JsonParseException | IllegalStateException e) {
            return fallback;
        }
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "PiBOH/cleanmcmod (github.com/PiBOH/cleanmcmod)")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("CurseForge mirror returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static String text(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return null;
        }
        String value = object.get(key).getAsString();
        return value.isBlank() ? null : value;
    }
}
