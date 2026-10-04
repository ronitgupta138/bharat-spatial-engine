package com.bharatspatial.model;

import java.util.Objects;

public class SpatialBoundingBox {
    private static final double METERS_PER_DEGREE_LAT = 111_132.95;

    private final double minLat;
    private final double minLon;
    private final double maxLat;
    private final double maxLon;

    public SpatialBoundingBox(double minLat, double minLon, double maxLat, double maxLon) {
        this.minLat = Math.min(minLat, maxLat);
        this.maxLat = Math.max(minLat, maxLat);
        this.minLon = Math.min(minLon, maxLon);
        this.maxLon = Math.max(minLon, maxLon);
    }

    public static SpatialBoundingBox fromCenterAndRadius(GeoPoint center, double radiusKm) {
        double radiusMeters = radiusKm * 1000.0;
        double deltaLat = radiusMeters / METERS_PER_DEGREE_LAT;
        double deltaLon = radiusMeters / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(center.getLatitude())));

        return new SpatialBoundingBox(
                center.getLatitude() - deltaLat,
                center.getLongitude() - deltaLon,
                center.getLatitude() + deltaLat,
                center.getLongitude() + deltaLon
        );
    }

    public boolean contains(GeoPoint point) {
        if (point == null) return false;
        return point.getLatitude() >= minLat && point.getLatitude() <= maxLat &&
               point.getLongitude() >= minLon && point.getLongitude() <= maxLon;
    }

    public boolean intersects(SpatialBoundingBox other) {
        if (other == null) return false;
        return this.minLat <= other.maxLat && this.maxLat >= other.minLat &&
               this.minLon <= other.maxLon && this.maxLon >= other.minLon;
    }

    public double getMinLat() { return minLat; }
    public double getMinLon() { return minLon; }
    public double getMaxLat() { return maxLat; }
    public double getMaxLon() { return maxLon; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpatialBoundingBox that = (SpatialBoundingBox) o;
        return Double.compare(that.minLat, minLat) == 0 &&
               Double.compare(that.minLon, minLon) == 0 &&
               Double.compare(that.maxLat, maxLat) == 0 &&
               Double.compare(that.maxLon, maxLon) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minLat, minLon, maxLat, maxLon);
    }

    @Override
    public String toString() {
        return String.format("[%f, %f] to [%f, %f]", minLat, minLon, maxLat, maxLon);
    }
}
