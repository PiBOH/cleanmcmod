package piboh.cleanmc.mod.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModrinthClientTest {

    @Test
    void lookupBodyHasHashesAndAlgorithmOnly() {
        JsonObject body = JsonParser.parseString(
                ModrinthClient.requestBody(List.of("abc123"), null, null)).getAsJsonObject();

        assertEquals("sha1", body.get("algorithm").getAsString());
        assertEquals("abc123", body.getAsJsonArray("hashes").get(0).getAsString());
        assertFalse(body.has("loaders"));
        assertFalse(body.has("game_versions"));
    }

    @Test
    void updateBodyCarriesGameVersionAndLoaders() {
        JsonObject body = JsonParser.parseString(ModrinthClient.requestBody(
                List.of("abc123"), List.of("fabric", "quilt"), List.of("1.21.8"))).getAsJsonObject();

        assertEquals("fabric", body.getAsJsonArray("loaders").get(0).getAsString());
        assertEquals("quilt", body.getAsJsonArray("loaders").get(1).getAsString());
        assertEquals("1.21.8", body.getAsJsonArray("game_versions").get(0).getAsString());
        assertEquals(1, body.getAsJsonArray("hashes").size());
    }

    @Test
    void parsesTheDocumentedVersionShape() {
        String json = """
                {
                  "hash-one": {
                    "id": "g58ofrov",
                    "project_id": "P7dR8mSH",
                    "version_number": "0.136.1+1.21.8",
                    "game_versions": ["1.21.8"],
                    "loaders": ["fabric"],
                    "files": [
                      {"primary": false, "filename": "sources.jar", "url": "https://cdn/x/sources.jar",
                       "hashes": {"sha1": "111"}},
                      {"primary": true, "filename": "fabric-api.jar", "url": "https://cdn/x/fabric-api.jar",
                       "hashes": {"sha1": "222"}}
                    ]
                  },
                  "hash-no-files": {
                    "id": "a", "project_id": "b", "version_number": "1", "game_versions": [], "loaders": [],
                    "files": []
                  },
                  "hash-no-sha": {
                    "id": "c", "project_id": "d", "version_number": "1", "game_versions": [], "loaders": [],
                    "files": [{"primary": true, "filename": "x.jar", "url": "https://cdn/x", "hashes": {}}]
                  }
                }
                """;

        Map<String, ModVersion> parsed = ModrinthClient.parseVersions(json);

        assertEquals(1, parsed.size());
        ModVersion version = parsed.get("hash-one");
        assertEquals("g58ofrov", version.id());
        assertEquals("P7dR8mSH", version.projectId());
        assertEquals("0.136.1+1.21.8", version.versionNumber());
        assertEquals("fabric-api.jar", version.fileName());
        assertEquals("222", version.sha1());
        assertEquals(List.of("1.21.8"), version.gameVersions());
        assertEquals(List.of("fabric"), version.loaders());
        assertTrue(version.matches("1.21.8", List.of("fabric")));
        assertFalse(version.matches("1.21.4", List.of("fabric")));
        assertFalse(version.matches("1.21.8", List.of("neoforge")));
    }

    /** Captured 2026-10-25 from /v2/project/AANobbMI/version: one 1.20.1 build, one 1.21.8 build. */
    private static final String SODIUM_OLD = """
            {
              "id": "OihdIimA", "project_id": "AANobbMI", "version_number": "mc1.20.1-0.5.13-fabric",
              "game_versions": ["1.20.1"], "loaders": ["fabric", "quilt"],
              "files": [{"primary": true, "filename": "sodium-fabric-0.5.13+mc1.20.1.jar",
                "url": "https://cdn.modrinth.com/data/AANobbMI/versions/OihdIimA/sodium-fabric-0.5.13%2Bmc1.20.1.jar",
                "hashes": {"sha1": "bcdbf37d9494e405cba210d7a80ed58e756297b5"}}]
            }
            """;

    private static final String SODIUM_NEW = """
            {
              "id": "7pwil2dy", "project_id": "AANobbMI", "version_number": "mc1.21.8-0.7.3-fabric",
              "game_versions": ["1.21.6", "1.21.7", "1.21.8"], "loaders": ["fabric"],
              "files": [{"primary": true, "filename": "sodium-fabric-0.7.3+mc1.21.8.jar",
                "url": "https://cdn.modrinth.com/data/AANobbMI/versions/7pwil2dy/sodium-fabric-0.7.3%2Bmc1.21.8.jar",
                "hashes": {"sha1": "c9ed2b00385ea191e81ea4da51c008755fe90a26"}}]
            }
            """;

    @Test
    void resolvedDependencyIsPickedForTheRunningGameVersionAndLoader() {
        ModVersion picked = ModrinthClient.pickCompatible("[" + SODIUM_OLD + "," + SODIUM_NEW + "]",
                "1.21.8", List.of("fabric"));

        assertEquals("mc1.21.8-0.7.3-fabric", picked.versionNumber());
    }

    @Test
    void aDependencyWithNoBuildForThisGameVersionIsRejected() {
        assertEquals(null, ModrinthClient.pickCompatible("[" + SODIUM_OLD + "]", "1.21.8", List.of("fabric")));
        assertEquals(null, ModrinthClient.pickCompatible("[" + SODIUM_NEW + "]", "1.21.8", List.of("quilt")));
    }
}
