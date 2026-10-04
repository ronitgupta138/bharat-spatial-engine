package com.bharatspatial.controller;

import com.bharatspatial.dto.FuzzySearchResponse;
import com.bharatspatial.service.SpatialIndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SpatialIndexService spatialService;

    public SearchController(SpatialIndexService spatialService) {
        this.spatialService = spatialService;
    }

    /**
     * Fuzzy search for Indian administrative entities across spelling and transliteration variants.
     * Example: /api/v1/search/fuzzy?q=Burdwan&limit=10
     * Example: /api/v1/search/fuzzy?q=Calcutta&limit=10
     */
    @GetMapping("/fuzzy")
    public ResponseEntity<FuzzySearchResponse> search(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(spatialService.fuzzySearch(query, limit));
    }
}
