package piboh.cleanmc.mod.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Lists the jars in {@code mods/} that can be looked up on Modrinth, CurseForge or a mod's GitHub repository. */
public final class ModScanner {

    public static final String OWN_MOD_ID = CleanMcMod.MOD_ID;

    private static final Pattern GITHUB = Pattern.compile(
            "github\\.com[/:]([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)", Pattern.CASE_INSENSITIVE);

    /** Path segments that are not a repository owner. */
    private static final List<String> NOT_OWNERS = List.of("sponsors", "users", "orgs", "features", "topics");

    public List<LocalMod> scan(Path gameDir) {
        Path modsDir = gameDir.resolve("mods");
        if (!Files.isDirectory(modsDir)) {
            return List.of();
        }
        List<Path> jars = new ArrayList<>();
        try (Stream<Path> entries = Files.list(modsDir)) {
            entries.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .sorted()
                    .forEach(jars::add);
        } catch (IOException e) {
            CleanMcMod.LOGGER.warn("Could not list {}: {}", modsDir, e.toString());
            return List.of();
        }

        List<LocalMod> found = new ArrayList<>(jars.size());
        for (Path jar : jars) {
            LocalMod mod = read(jar);
            if (mod != null && !isOwnJar(mod)) {
                found.add(mod);
            }
        }
        return List.copyOf(found);
    }

    private static boolean isOwnJar(LocalMod mod) {
        return OWN_MOD_ID.equals(mod.modId())
                || mod.fileName().toLowerCase(Locale.ROOT).startsWith(OWN_MOD_ID + "-");
    }

    private LocalMod read(Path jar) {
        String sha1;
        try {
            sha1 = sha1(jar);
        } catch (IOException e) {
            CleanMcMod.LOGGER.warn("Could not read mod file {}: {}", jar, e.toString());
            return null;
        }
        String modId = null;
        String version = null;
        String name = null;
        String githubRepo = null;
        Map<String, String> depends = new HashMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("fabric.mod.json");
            if (entry != null) {
                try (InputStream stream = zip.getInputStream(entry)) {
                    JsonObject metadata = JsonParser
                            .parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                            .getAsJsonObject();
                    modId = string(metadata, "id");
                    version = string(metadata, "version");
                    name = string(metadata, "name");
                    githubRepo = githubRepo(metadata);
                    depends = ranges(metadata, "depends");
                }
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            // Not a mod jar, or a broken one: still hashable, so an update can be found by hash.
            CleanMcMod.LOGGER.debug("No readable fabric.mod.json in {}: {}", jar, e.toString());
        }
        return new LocalMod(jar, sha1, modId, version, depends, githubRepo, name);
    }

    /**
     * The GitHub repository a mod names in its own metadata, as {@code owner/repo}. Sources and
     * homepage are preferred over the issue tracker, which is often a different repository.
     */
    static String githubRepo(JsonObject metadata) {
        if (!metadata.has("contact") || !metadata.get("contact").isJsonObject()) {
            return null;
        }
        JsonObject contact = metadata.getAsJsonObject("contact");
        for (String key : List.of("sources", "source", "homepage", "issues")) {
            String repo = repoFrom(string(contact, key));
            if (repo != null) {
                return repo;
            }
        }
        return null;
    }

    /** {@code owner/repo} from a GitHub URL, or null when the URL is not a repository. */
    static String repoFrom(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher matcher = GITHUB.matcher(url);
        if (!matcher.find()) {
            return null;
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        if (NOT_OWNERS.contains(owner.toLowerCase(Locale.ROOT))) {
            return null;
        }
        if (repo.toLowerCase(Locale.ROOT).endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }
        return owner + "/" + repo;
    }

    /** The {@code depends} block of a mod, as id to version predicate. Non-string values are skipped. */
    private static Map<String, String> ranges(JsonObject metadata, String key) {
        Map<String, String> ranges = new HashMap<>();
        if (!metadata.has(key) || !metadata.get(key).isJsonObject()) {
            return ranges;
        }
        for (Map.Entry<String, com.google.gson.JsonElement> entry
                : metadata.getAsJsonObject(key).entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                ranges.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return ranges;
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }

    public static String sha1(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 is not available", e);
        }
        try (InputStream stream = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder hex = new StringBuilder(40);
        for (byte value : digest.digest()) {
            hex.append(String.format(Locale.ROOT, "%02x", value));
        }
        return hex.toString();
    }
}
