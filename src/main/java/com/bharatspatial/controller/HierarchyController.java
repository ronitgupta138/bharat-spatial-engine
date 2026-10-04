package com.bharatspatial.controller;

import com.bharatspatial.dto.HierarchyResponse;
import com.bharatspatial.service.SpatialIndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hierarchy")
public class HierarchyController {

    private final SpatialIndexService spatialService;

    public HierarchyController(SpatialIndexService spatialService) {
        this.spatialService = spatialService;
    }

    /**
     * Lists all states and union territories with aggregate stats.
     */
    @GetMapping("/states")
    public ResponseEntity<List<Map<String, Object>>> listStates() {
        return ResponseEntity.ok(spatialService.listStates());
    }

    /**
     * Lists all districts within a state.
     * Example: /api/v1/hierarchy/districts?stateCode=19
     */
    @GetMapping("/districts")
    public ResponseEntity<List<Map<String, Object>>> listDistricts(@RequestParam("stateCode") String stateCode) {
        return ResponseEntity.ok(spatialService.listDistricts(stateCode));
    }

    /**
     * Drilldown full hierarchy and direct children by MDDS code.
     * Example: /api/v1/hierarchy/drilldown?mddsCode=1901007
     */
    @GetMapping("/drilldown")
    public ResponseEntity<HierarchyResponse> getHierarchy(@RequestParam("mddsCode") String mddsCode) {
        return ResponseEntity.ok(spatialService.getHierarchyByMdds(mddsCode));
    }
}
