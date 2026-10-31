package piboh.cleanmc.mod.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModScannerMetadataTest {

    @Test
    void readsOwnerAndRepoFromASourceUrl() {
        assertEquals("CaffeineMC/sodium-fabric",
                ModScanner.repoFrom("https://github.com/CaffeineMC/sodium-fabric"));
        assertEquals("owner/repo", ModScanner.repoFrom("https://github.com/owner/repo.git"));
        assertEquals("owner/repo", ModScanner.repoFrom("git@github.com:owner/repo.git"));
        assertEquals("owner/repo", ModScanner.repoFrom("https://github.com/owner/repo/tree/main"));
    }

    @Test
    void ignoresUrlsThatAreNotARepository() {
        assertNull(ModScanner.repoFrom(null));
        assertNull(ModScanner.repoFrom(""));
        assertNull(ModScanner.repoFrom("https://modrinth.com/mod/sodium"));
        assertNull(ModScanner.repoFrom("https://github.com/sponsors/someone"));
    }

    @Test
    void prefersSourcesOverTheIssueTracker() {
        JsonObject metadata = JsonParser.parseString("""
                {"contact": {"sources": "https://github.com/owner/repo",
                             "issues": "https://github.com/owner/repo-issues"}}
                """).getAsJsonObject();

        assertEquals("owner/repo", ModScanner.githubRepo(metadata));
    }

    @Test
    void aModWithoutContactHasNoRepository() {
        JsonObject metadata = JsonParser.parseString("{\"id\": \"sodium\"}").getAsJsonObject();
        assertNull(ModScanner.githubRepo(metadata));
    }
}
