package com.bharatspatial.spatial;

import com.bharatspatial.model.GeoPoint;
import com.bharatspatial.model.SpatialBoundingBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeohashUtilTest {

    @Test
    void testEncodeAndDecodeConsistency() {
        double lat = 22.572646;
        double lon = 88.363895;

        String geohash = GeohashUtil.encode(lat, lon, 7);
        assertNotNull(geohash);
        assertEquals(7, geohash.length());

        GeoPoint decoded = GeohashUtil.decode(geohash);
        // Precision 7 geohash has ~150m accuracy
        double errorMeters = new GeoPoint(lat, lon).distanceToMeters(decoded);
        assertTrue(errorMeters < 150.0, "Decoded point should be within 150m cell, error was: " + errorMeters);
    }

    @Test
    void testGetNeighbors() {
        String geohash = "tunb9";
        List<String> neighbors = GeohashUtil.getNeighbors(geohash);

        assertEquals(9, neighbors.size(), "Should return 8 neighbors + center = 9 hashes");
        assertTrue(neighbors.contains(geohash), "Should contain the center geohash");
    }

    @Test
    void testBoundingBoxDecoding() {
        String geohash = "tu";
        SpatialBoundingBox bbox = GeohashUtil.decodeBoundingBox(geohash);

        assertNotNull(bbox);
        assertTrue(bbox.getMaxLat() > bbox.getMinLat());
        assertTrue(bbox.getMaxLon() > bbox.getMinLon());
    }
}
