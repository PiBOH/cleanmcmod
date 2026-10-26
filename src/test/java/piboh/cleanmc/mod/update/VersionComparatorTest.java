package piboh.cleanmc.mod.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionComparatorTest {

    @Test
    void comparesNumericSegmentsAsNumbers() {
        assertTrue(VersionComparator.compare("1.2.10", "1.2.9") > 0);
        assertTrue(VersionComparator.compare("2.0", "1.9.9") > 0);
        assertTrue(VersionComparator.compare("0.10", "0.9") > 0);
    }

    @Test
    void missingSegmentsCountAsZero() {
        assertEquals(0, VersionComparator.compare("1.0", "1.0.0"));
        assertEquals(0, VersionComparator.compare("1", "1.0.0.0"));
        assertTrue(VersionComparator.compare("1.0.1", "1.0") > 0);
    }

    @Test
    void releaseBeatsPreRelease() {
        assertTrue(VersionComparator.compare("1.0.0", "1.0.0-beta") > 0);
        assertTrue(VersionComparator.compare("1.0.0-rc1", "1.0.0-beta") > 0);
    }

    @Test
    void buildMetadataIsIgnored() {
        assertEquals(0, VersionComparator.compare("0.136.1+1.21.8", "0.136.1"));
        assertEquals(0, VersionComparator.compare("0.5.9+1.21.8", "0.5.9+1.20.1"));
    }

    @Test
    void stripsOneLeadingV() {
        assertTrue(VersionComparator.compare("v2.0", "1.9.9") > 0);
        assertEquals(0, VersionComparator.compare("v1.2", "1.2"));
    }
}
