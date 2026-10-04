package com.bharatspatial.spatial;

import com.bharatspatial.model.AdministrativeLevel;
import com.bharatspatial.model.AdministrativeNode;
import com.bharatspatial.model.GeoPoint;
import com.bharatspatial.model.SpatialBoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SpatialKDTreeTest {

    private SpatialKDTree kdTree;
    private AdministrativeNode kolkata;
    private AdministrativeNode howrah;
    private AdministrativeNode saltLake;
    private AdministrativeNode bardhaman;
    private AdministrativeNode delhi;

    @BeforeEach
    void setUp() {
        kdTree = new SpatialKDTree();

        kolkata = AdministrativeNode.builder()
                .mddsCode("1901001")
                .name("Kolkata GPO")
                .level(AdministrativeLevel.URBAN_BODY)
                .location(22.572646, 88.363895)
                .build();

        howrah = AdministrativeNode.builder()
                .mddsCode("1901005")
                .name("Howrah Station Area")
                .level(AdministrativeLevel.URBAN_BODY)
                .location(22.583000, 88.342600)
                .build();

        saltLake = AdministrativeNode.builder()
                .mddsCode("1901003")
                .name("Salt Lake Sector V")
                .level(AdministrativeLevel.URBAN_BODY)
                .location(22.586700, 88.417800)
                .build();

        bardhaman = AdministrativeNode.builder()
                .mddsCode("1901006")
                .name("Bardhaman Sadar")
                .level(AdministrativeLevel.URBAN_BODY)
                .location(23.232400, 87.861500)
                .build();

        delhi = AdministrativeNode.builder()
                .mddsCode("0701001")
                .name("Connaught Place")
                .level(AdministrativeLevel.URBAN_BODY)
                .location(28.631500, 77.216700)
                .build();

        kdTree.build(List.of(kolkata, howrah, saltLake, bardhaman, delhi));
    }

    @Test
    void testTreeSizeAndMaxDepth() {
        assertEquals(5, kdTree.size());
        assertTrue(kdTree.getMaxDepth() >= 2);
    }

    @Test
    void testNearestNeighborFromParkStreet() {
        // Query near Park Street (22.5511, 88.3524)
        GeoPoint parkStreet = new GeoPoint(22.551100, 88.352400);

        Optional<SpatialKDTree.ScoredNode> nearest = kdTree.findNearestNeighbor(parkStreet);
        assertTrue(nearest.isPresent());
        // Closest among nodes is Kolkata GPO
        assertEquals("Kolkata GPO", nearest.get().getNode().getName());
        assertTrue(nearest.get().getDistanceKm() < 5.0);
    }

    @Test
    void testRadiusSearchAroundKolkata() {
        // Radius of 15km around Kolkata center should include Kolkata, Howrah, Salt Lake, but NOT Bardhaman or Delhi
        GeoPoint kolkataCenter = new GeoPoint(22.572646, 88.363895);
        List<SpatialKDTree.ScoredNode> results = kdTree.findWithinRadius(kolkataCenter, 15.0);

        assertEquals(3, results.size());
        List<String> names = results.stream().map(r -> r.getNode().getName()).toList();
        assertTrue(names.contains("Kolkata GPO"));
        assertTrue(names.contains("Howrah Station Area"));
        assertTrue(names.contains("Salt Lake Sector V"));
        assertFalse(names.contains("Bardhaman Sadar"));
        assertFalse(names.contains("Connaught Place"));
    }

    @Test
    void testKNearestNeighbors() {
        GeoPoint testPoint = new GeoPoint(22.570000, 88.360000);
        List<SpatialKDTree.ScoredNode> knn = kdTree.findKNearestNeighbors(testPoint, 2);

        assertEquals(2, knn.size());
        assertTrue(knn.get(0).getDistanceMeters() <= knn.get(1).getDistanceMeters());
    }

    @Test
    void testBoundingBoxFilter() {
        // Bounding box covering Kolkata metropolitan region (approx 22.4 to 22.7 N, 88.2 to 88.5 E)
        SpatialBoundingBox bbox = new SpatialBoundingBox(22.4, 88.2, 22.7, 88.5);
        List<AdministrativeNode> found = kdTree.findWithinBoundingBox(bbox);

        assertEquals(3, found.size());
        List<String> names = found.stream().map(AdministrativeNode::getName).toList();
        assertTrue(names.contains("Kolkata GPO"));
        assertTrue(names.contains("Howrah Station Area"));
        assertTrue(names.contains("Salt Lake Sector V"));
    }
}
