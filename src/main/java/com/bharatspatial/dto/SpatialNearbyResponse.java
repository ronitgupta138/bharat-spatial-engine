package com.bharatspatial.dto;

import com.bharatspatial.model.AdministrativeNode;
import com.bharatspatial.model.GeoPoint;

import java.util.List;

public class SpatialNearbyResponse {
    public static class NearbyItem {
        private final AdministrativeNode entity;
        private final double distanceKm;
        private final double distanceMeters;
        private final double bearingDegrees;
        private final String breadcrumb;

        public NearbyItem(AdministrativeNode entity, double distanceMeters, double bearingDegrees) {
            this.entity = entity;
            this.distanceMeters = Math.round(distanceMeters * 100.0) / 100.0;
            this.distanceKm = Math.round((distanceMeters / 1000.0) * 1000.0) / 1000.0;
            this.bearingDegrees = Math.round(bearingDegrees * 10.0) / 10.0;
            this.breadcrumb = entity != null ? entity.getHierarchyBreadcrumb() : "";
        }

        public AdministrativeNode getEntity() { return entity; }
        public double getDistanceKm() { return distanceKm; }
        public double getDistanceMeters() { return distanceMeters; }
        public double getBearingDegrees() { return bearingDegrees; }
        public String getBreadcrumb() { return breadcrumb; }
    }

    private final GeoPoint queryPoint;
    private final double radiusKm;
    private final int count;
    private final List<NearbyItem> items;
    private final double executionTimeMs;

    public SpatialNearbyResponse(GeoPoint queryPoint, double radiusKm, List<NearbyItem> items, double executionTimeMs) {
        this.queryPoint = queryPoint;
        this.radiusKm = radiusKm;
        this.items = items;
        this.count = items != null ? items.size() : 0;
        this.executionTimeMs = Math.round(executionTimeMs * 1000.0) / 1000.0;
    }

    public GeoPoint getQueryPoint() { return queryPoint; }
    public double getRadiusKm() { return radiusKm; }
    public int getCount() { return count; }
    public List<NearbyItem> getItems() { return items; }
    public double getExecutionTimeMs() { return executionTimeMs; }
}
