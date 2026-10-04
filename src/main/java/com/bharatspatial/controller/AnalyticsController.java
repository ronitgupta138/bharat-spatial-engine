package com.bharatspatial.controller;

import com.bharatspatial.dto.EngineStatsResponse;
import com.bharatspatial.service.SpatialIndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final SpatialIndexService spatialService;

    public AnalyticsController(SpatialIndexService spatialService) {
        this.spatialService = spatialService;
    }

    /**
     * Returns real-time engine statistics: index depth, entity counts, memory footprint, virtual threads status.
     */
    @GetMapping("/stats")
    public ResponseEntity<EngineStatsResponse> getStats() {
        return ResponseEntity.ok(spatialService.getEngineStats());
    }
}
