package com.bharatspatial.model;

import java.util.Objects;

public class GeoPoint {
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final double INDIA_MIN_LAT = 6.0;
    private static final double INDIA_MAX_LAT = 38.0;
    private static final double INDIA_MIN_LON = 68.0;
    private static final double INDIA_MAX_LON = 98.0;

    private final double latitude;
    private final double longitude;

    public GeoPoint(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and +90 degrees: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and +180 degrees: " + longitude);
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isWithinIndiaBounds() {
        return latitude >= INDIA_MIN_LAT && latitude <= INDIA_MAX_LAT &&
               longitude >= INDIA_MIN_LON && longitude <= INDIA_MAX_LON;
    }

    /**
     * Calculates the Great-Circle distance using Haversine formula in meters.
     */
    public double distanceToMeters(GeoPoint other) {
        if (other == null) {
            throw new IllegalArgumentException("Target GeoPoint cannot be null");
        }
        double lat1Rad = Math.toRadians(this.latitude);
        double lon1Rad = Math.toRadians(this.longitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double lon2Rad = Math.toRadians(other.longitude);

        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        double a = Math.sin(deltaLat / 2.0) * Math.sin(deltaLat / 2.0) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2.0) * Math.sin(deltaLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }

    public double distanceToKm(GeoPoint other) {
        return distanceToMeters(other) / 1000.0;
    }

    /**
     * Calculates initial bearing (azimuth) in degrees (0 to 360).
     */
    public double bearingTo(GeoPoint other) {
        double lat1Rad = Math.toRadians(this.latitude);
        double lon1Rad = Math.toRadians(this.longitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double lon2Rad = Math.toRadians(other.longitude);

        double deltaLon = lon2Rad - lon1Rad;
        double y = Math.sin(deltaLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360.0) % 360.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoPoint geoPoint = (GeoPoint) o;
        return Double.compare(geoPoint.latitude, latitude) == 0 &&
               Double.compare(geoPoint.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return String.format("%.6f, %.6f", latitude, longitude);
    }
}
