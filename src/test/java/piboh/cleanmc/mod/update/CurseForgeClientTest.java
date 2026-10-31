package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CurseForgeClientTest {

    private static final List<String> FABRIC = List.of("fabric", "quilt");

    private static final String PAYLOAD = """
            {
              "title": "Test Mod",
              "files": [
                {
                  "id": 1234567,
                  "display": "Test Mod 2.0.0 for Fabric 1.21.8",
                  "name": "testmod-fabric-2.0.0+mc1.21.8.jar",
                  "type": "release",
                  "versions": ["Client", "Fabric", "1.21.8"],
                  "uploaded_at": "2026-01-01T00:00:00Z"
                },
                {
                  "id": 1234568,
                  "display": "Test Mod 2.0.1 for NeoForge 1.21.8",
                  "name": "testmod-neoforge-2.0.1+mc1.21.8.jar",
                  "type": "release",
                  "versions": ["Client", "NeoForge", "1.21.8"],
                  "uploaded_at": "2026-02-01T00:00:00Z"
                },
                {
                  "id": 1234569,
                  "display": "Test Mod 2.0.2-alpha for Fabric 1.21.8",
                  "name": "testmod-fabric-2.0.2-alpha+mc1.21.8.jar",
                  "type": "alpha",
                  "versions": ["Client", "Fabric", "1.21.8"],
                  "uploaded_at": "2026-03-01T00:00:00Z"
                },
                {
                  "id": 1234570,
                  "display": "Test Mod 3.0.0 for Fabric 1.21.9",
                  "name": "testmod-fabric-3.0.0+mc1.21.9.jar",
                  "type": "release",
                  "versions": ["Client", "Fabric", "1.21.9"],
                  "uploaded_at": "2026-04-01T00:00:00Z"
                }
              ]
            }
            """;

    @Test
    void picksTheNewestFileForThisGameVersionAndLoader() {
        CurseForgeClient.CfFile file = CurseForgeClient.pick(PAYLOAD, "1.21.8", FABRIC);

        assertEquals("testmod-fabric-2.0.0+mc1.21.8.jar", file.fileName());
        assertEquals("2.0.0", file.versionNumber());
        assertEquals("https://edge.forgecdn.net/files/1234/567/testmod-fabric-2.0.0+mc1.21.8.jar", file.url());
    }

    @Test
    void nothingIsOfferedForAGameVersionWithoutFiles() {
        assertNull(CurseForgeClient.pick(PAYLOAD, "1.21.4", FABRIC));
    }

    @Test
    void anUnreadablePayloadIsNotAnUpdate() {
        assertNull(CurseForgeClient.pick("not json", "1.21.8", FABRIC));
        assertNull(CurseForgeClient.pick("{\"title\":\"x\"}", "1.21.8", FABRIC));
    }

    @Test
    void versionComesFromTheDisplayName() {
        assertEquals("0.8.15-beta.1",
                CurseForgeClient.versionFrom("Sodium 0.8.15-beta.1 for Fabric 1.21.11", null));
        assertEquals("1.2.3", CurseForgeClient.versionFrom(null, "mod-fabric-1.2.3+mc1.21.8.jar"));
        assertNull(CurseForgeClient.versionFrom("Mod", "mod.jar"));
    }

    @Test
    void downloadUrlFollowsTheForgeCdnLayout() {
        assertEquals("https://edge.forgecdn.net/files/1234/567/x.jar",
                CurseForgeClient.downloadUrl("1234567", "x.jar"));
        assertNull(CurseForgeClient.downloadUrl("12", "x.jar"));
    }
}
