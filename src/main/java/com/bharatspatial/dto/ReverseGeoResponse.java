package com.bharatspatial.dto;

import com.bharatspatial.model.AdministrativeNode;
import com.bharatspatial.model.GeoPoint;

public class ReverseGeoResponse {
    private final GeoPoint queryPoint;
    private final AdministrativeNode entity;
    private final double distanceMeters;
    private final double distanceKm;
    private final double bearingDegrees;
    private final String breadcrumb;
    private final double executionTimeMs;

    public ReverseGeoResponse(GeoPoint queryPoint, AdministrativeNode entity,
                              double distanceMeters, double bearingDegrees, double executionTimeMs) {
        this.queryPoint = queryPoint;
        this.entity = entity;
        this.distanceMeters = Math.round(distanceMeters * 100.0) / 100.0;
        this.distanceKm = Math.round((distanceMeters / 1000.0) * 1000.0) / 1000.0;
        this.bearingDegrees = Math.round(bearingDegrees * 10.0) / 10.0;
        this.breadcrumb = entity != null ? entity.getHierarchyBreadcrumb() : "";
        this.executionTimeMs = Math.round(executionTimeMs * 1000.0) / 1000.0;
    }

    public GeoPoint getQueryPoint() { return queryPoint; }
    public AdministrativeNode getEntity() { return entity; }
    public double getDistanceMeters() { return distanceMeters; }
    public double getDistanceKm() { return distanceKm; }
    public double getBearingDegrees() { return bearingDegrees; }
    public String getBreadcrumb() { return breadcrumb; }
    public double getExecutionTimeMs() { return executionTimeMs; }
}
