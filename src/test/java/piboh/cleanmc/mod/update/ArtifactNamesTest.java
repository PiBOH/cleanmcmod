package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactNamesTest {

    private static final List<String> FABRIC = List.of("fabric", "quilt");

    @Test
    void acceptsTheRunningGameVersionInAnyShape() {
        assertTrue(ArtifactNames.namesGameVersion("sodium-fabric-0.6.13+mc1.21.8.jar", "1.21.8"));
        assertTrue(ArtifactNames.namesGameVersion("mod-1.21.8.jar", "1.21.8"));
        assertTrue(ArtifactNames.namesGameVersion("mod 1.2.0 for Fabric 1.21.8", "1.21.8"));
    }

    @Test
    void rejectsAnotherGameVersion() {
        assertFalse(ArtifactNames.namesGameVersion("sodium-fabric-0.6.13+mc1.21.9.jar", "1.21.8"));
        assertFalse(ArtifactNames.namesGameVersion("sodium-1.21.1.jar", "1.21.8"));
    }

    @Test
    void rejectsAFileMeantForAnotherLoader() {
        assertFalse(ArtifactNames.namesLoader("sodium-neoforge-0.6.13.jar", FABRIC));
        assertFalse(ArtifactNames.namesLoader("sodium-forge-0.6.13.jar", FABRIC));
        assertTrue(ArtifactNames.namesLoader("sodium-fabric-0.6.13.jar", FABRIC));
        assertTrue(ArtifactNames.namesLoader("sodium.jar", FABRIC));
    }

    @Test
    void ignoresFilesThatAreNotAModJar() {
        assertTrue(ArtifactNames.looksLikeJar("sodium-fabric-0.6.13.jar"));
        assertFalse(ArtifactNames.looksLikeJar("sodium-0.6.13-sources.jar"));
        assertFalse(ArtifactNames.looksLikeJar("sodium-0.6.13-javadoc.jar"));
        assertFalse(ArtifactNames.looksLikeJar("sodium-0.6.13-dev.jar"));
        assertFalse(ArtifactNames.looksLikeJar("checksums.txt"));
        assertFalse(ArtifactNames.looksLikeJar(null));
    }
}
