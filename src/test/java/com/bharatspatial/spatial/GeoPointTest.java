package com.bharatspatial.spatial;

import com.bharatspatial.model.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoPointTest {

    @Test
    void testValidCoordinates() {
        GeoPoint kolkata = new GeoPoint(22.572646, 88.363895);
        assertEquals(22.572646, kolkata.getLatitude(), 1e-6);
        assertEquals(88.363895, kolkata.getLongitude(), 1e-6);
        assertTrue(kolkata.isWithinIndiaBounds());
    }

    @Test
    void testInvalidCoordinatesThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(95.0, 80.0));
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(-91.0, 80.0));
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(20.0, 185.0));
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(20.0, -185.0));
    }

    @Test
    void testHaversineDistanceBetweenKolkataAndHowrah() {
        // Kolkata GPO ~ (22.5726, 88.3639)
        // Howrah Station ~ (22.5830, 88.3426)
        GeoPoint kolkata = new GeoPoint(22.572646, 88.363895);
        GeoPoint howrah = new GeoPoint(22.583000, 88.342600);

        double distMeters = kolkata.distanceToMeters(howrah);
        double distKm = kolkata.distanceToKm(howrah);

        // Distance between Kolkata GPO and Howrah Station is ~2.4 km
        assertTrue(distKm > 2.0 && distKm < 3.0, "Distance should be ~2.4km, got: " + distKm);
        assertEquals(distMeters, distKm * 1000.0, 1e-3);
    }

    @Test
    void testBearingCalculation() {
        // Moving due north (lat increases, lon constant)
        GeoPoint p1 = new GeoPoint(10.0, 75.0);
        GeoPoint p2 = new GeoPoint(12.0, 75.0);

        double bearing = p1.bearingTo(p2);
        assertEquals(0.0, bearing, 0.5, "Bearing due north should be ~0 degrees");
    }
}
