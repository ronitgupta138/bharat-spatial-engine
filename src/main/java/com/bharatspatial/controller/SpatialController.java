package com.bharatspatial.controller;

import com.bharatspatial.dto.ReverseGeoResponse;
import com.bharatspatial.dto.SpatialNearbyResponse;
import com.bharatspatial.model.AdministrativeNode;
import com.bharatspatial.service.SpatialIndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/spatial")
public class SpatialController {

    private final SpatialIndexService spatialService;

    public SpatialController(SpatialIndexService spatialService) {
        this.spatialService = spatialService;
    }

    /**
     * Reverse geocodes a GPS coordinate to the nearest administrative settlement.
     * Example: /api/v1/spatial/reverse?lat=22.5726&lon=88.3639
     */
    @GetMapping("/reverse")
    public ResponseEntity<ReverseGeoResponse> reverseGeocode(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon) {
        return ResponseEntity.ok(spatialService.reverseGeocode(lat, lon));
    }

    /**
     * Finds all settlements within a given radius in kilometers.
     * Example: /api/v1/spatial/nearby?lat=22.5726&lon=88.3639&radiusKm=25&limit=20
     */
    @GetMapping("/nearby")
    public ResponseEntity<SpatialNearbyResponse> findNearby(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(spatialService.findNearby(lat, lon, radiusKm, limit));
    }

    /**
     * K-Nearest Neighbors query.
     * Example: /api/v1/spatial/knn?lat=28.6315&lon=77.2167&k=5
     */
    @GetMapping("/knn")
    public ResponseEntity<SpatialNearbyResponse> findKnn(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(value = "k", defaultValue = "10") int k) {
        return ResponseEntity.ok(spatialService.findKnn(lat, lon, k));
    }

    /**
     * Bounding box spatial query.
     * Example: /api/v1/spatial/bbox?minLat=22.0&minLon=87.0&maxLat=24.0&maxLon=89.0
     */
    @GetMapping("/bbox")
    public ResponseEntity<List<AdministrativeNode>> findWithinBoundingBox(
            @RequestParam("minLat") double minLat,
            @RequestParam("minLon") double minLon,
            @RequestParam("maxLat") double maxLat,
            @RequestParam("maxLon") double maxLon) {
        return ResponseEntity.ok(spatialService.findWithinBoundingBox(minLat, minLon, maxLat, maxLon));
    }
}
