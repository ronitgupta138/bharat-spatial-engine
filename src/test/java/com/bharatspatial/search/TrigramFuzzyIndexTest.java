package com.bharatspatial.search;

import com.bharatspatial.model.AdministrativeNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrigramFuzzyIndexTest {

    private TrigramFuzzyIndex index;

    @BeforeEach
    void setUp() {
        index = new TrigramFuzzyIndex();

        AdministrativeNode bardhaman = AdministrativeNode.builder()
                .mddsCode("1901006")
                .name("Bardhaman Sadar")
                .districtName("Purba Bardhaman")
                .build();

        AdministrativeNode kolkata = AdministrativeNode.builder()
                .mddsCode("1901001")
                .name("Kolkata GPO")
                .districtName("Kolkata")
                .build();

        AdministrativeNode bangalore = AdministrativeNode.builder()
                .mddsCode("2901001")
                .name("MG Road Bengaluru")
                .districtName("Bengaluru Urban")
                .build();

        AdministrativeNode varanasi = AdministrativeNode.builder()
                .mddsCode("0901001")
                .name("Kashi Vishwanath Corridor")
                .districtName("Varanasi")
                .build();

        index.index(List.of(bardhaman, kolkata, bangalore, varanasi));
    }

    @Test
    void testTransliterationAliasMatchBurdwan() {
        // Querying "burdwan" should match "Bardhaman Sadar" via alias dictionary
        List<TrigramFuzzyIndex.MatchResult> results = index.search("burdwan", 5);
        assertFalse(results.isEmpty());
        assertEquals("Bardhaman Sadar", results.get(0).getNode().getName());
        assertTrue(results.get(0).getScore() > 0.5);
    }

    @Test
    void testTransliterationAliasMatchCalcutta() {
        // Querying "calcutta" should match "Kolkata GPO"
        List<TrigramFuzzyIndex.MatchResult> results = index.search("calcutta", 5);
        assertFalse(results.isEmpty());
        assertEquals("Kolkata GPO", results.get(0).getNode().getName());
    }

    @Test
    void testTransliterationAliasMatchBangalore() {
        // Querying "bangalore" should match "MG Road Bengaluru"
        List<TrigramFuzzyIndex.MatchResult> results = index.search("bangalore", 5);
        assertFalse(results.isEmpty());
        assertEquals("MG Road Bengaluru", results.get(0).getNode().getName());
    }

    @Test
    void testFuzzyTypoTolerance() {
        // Typo: "Bardhamn" instead of "Bardhaman"
        List<TrigramFuzzyIndex.MatchResult> results = index.search("Bardhamn", 5);
        assertFalse(results.isEmpty());
        assertEquals("Bardhaman Sadar", results.get(0).getNode().getName());
    }

    @Test
    void testLevenshteinDistanceMetric() {
        assertEquals(0, TrigramFuzzyIndex.levenshteinDistance("kolkata", "kolkata"));
        assertEquals(1, TrigramFuzzyIndex.levenshteinDistance("kolkata", "kolkatta"));
        assertEquals(3, TrigramFuzzyIndex.levenshteinDistance("kitten", "sitting"));
    }
}
