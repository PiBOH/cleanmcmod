package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GithubClientTest {

    private static final List<String> FABRIC = List.of("fabric", "quilt");

    @Test
    void picksTheNewestAssetForThisGameVersion() {
        String payload = """
                [
                  {
                    "tag_name": "v1.2.0",
                    "draft": false,
                    "assets": [
                      {"name": "mod-1.2.0-forged.jar", "browser_download_url": "https://example.invalid/forge.jar"},
                      {"name": "mod-1.2.0+mc1.21.8.jar", "browser_download_url": "https://example.invalid/ok.jar"}
                    ]
                  },
                  {
                    "tag_name": "v1.3.0",
                    "draft": false,
                    "assets": [
                      {"name": "mod-1.3.0+mc1.21.9.jar", "browser_download_url": "https://example.invalid/newer.jar"}
                    ]
                  }
                ]
                """;

        GithubClient.Asset asset = GithubClient.pick(payload, "1.21.8", FABRIC);

        assertEquals("1.2.0", asset.versionNumber());
        assertEquals("mod-1.2.0+mc1.21.8.jar", asset.fileName());
        assertEquals("https://example.invalid/ok.jar", asset.url());
    }

    @Test
    void aReleaseThatNamesTheVersionOnlyInItsTagStillWorks() {
        String payload = """
                [
                  {
                    "tag_name": "v2.0.0+1.21.8",
                    "draft": false,
                    "assets": [{"name": "mod.jar", "browser_download_url": "https://example.invalid/mod.jar"}]
                  }
                ]
                """;

        GithubClient.Asset asset = GithubClient.pick(payload, "1.21.8", FABRIC);

        assertEquals("2.0.0+1.21.8", asset.versionNumber());
        assertEquals("mod.jar", asset.fileName());
    }

    @Test
    void draftsAndForeignReleasesAreIgnored() {
        String payload = """
                [
                  {"tag_name": "v9.0.0", "draft": true, "assets": [
                      {"name": "mod-9.0.0+mc1.21.8.jar", "browser_download_url": "https://example.invalid/draft.jar"}]},
                  {"tag_name": "v1.0.0", "draft": false, "assets": [
                      {"name": "mod-1.0.0+mc1.21.9.jar", "browser_download_url": "https://example.invalid/x.jar"}]}
                ]
                """;

        assertNull(GithubClient.pick(payload, "1.21.8", FABRIC));
    }

    @Test
    void anUnreadablePayloadIsNotAnUpdate() {
        assertNull(GithubClient.pick("{\"message\":\"rate limited\"}", "1.21.8", FABRIC));
        assertNull(GithubClient.pick("[]", "1.21.8", FABRIC));
    }
}
