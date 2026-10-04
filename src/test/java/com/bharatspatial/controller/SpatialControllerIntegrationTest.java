package com.bharatspatial.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SpatialControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testReverseGeocodingKolkata() throws Exception {
        mockMvc.perform(get("/api/v1/spatial/reverse")
                        .param("lat", "22.572646")
                        .param("lon", "88.363895")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entity.name", containsString("Kolkata GPO")))
                .andExpect(jsonPath("$.entity.stateName", is("West Bengal")))
                .andExpect(jsonPath("$.breadcrumb", containsString("West Bengal > Kolkata")))
                .andExpect(jsonPath("$.distanceMeters", lessThan(10.0)))
                .andExpect(jsonPath("$.executionTimeMs", notNullValue()));
    }

    @Test
    void testNearbyRadiusSearch() throws Exception {
        mockMvc.perform(get("/api/v1/spatial/nearby")
                        .param("lat", "22.572646")
                        .param("lon", "88.363895")
                        .param("radiusKm", "25.0")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.items[0].entity.name", is("Kolkata GPO")))
                .andExpect(jsonPath("$.radiusKm", is(25.0)));
    }

    @Test
    void testKNearestNeighbors() throws Exception {
        mockMvc.perform(get("/api/v1/spatial/knn")
                        .param("lat", "28.6315")
                        .param("lon", "77.2167")
                        .param("k", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(3)))
                .andExpect(jsonPath("$.items[0].entity.name", is("Connaught Place")));
    }

    @Test
    void testHierarchyStates() throws Exception {
        mockMvc.perform(get("/api/v1/hierarchy/states")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[?(@.stateName == 'West Bengal')].stateCode", hasItem("19")));
    }

    @Test
    void testHierarchyDistricts() throws Exception {
        mockMvc.perform(get("/api/v1/hierarchy/districts")
                        .param("stateCode", "19")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[?(@.districtName == 'Purba Bardhaman')]", not(empty())));
    }

    @Test
    void testHierarchyDrilldown() throws Exception {
        mockMvc.perform(get("/api/v1/hierarchy/drilldown")
                        .param("mddsCode", "1901007")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Shyamsundar")))
                .andExpect(jsonPath("$.breadcrumb", is("West Bengal > Purba Bardhaman > Raina I > Shyamsundar GP > Shyamsundar")));
    }

    @Test
    void testFuzzyTransliterationSearchBurdwan() throws Exception {
        mockMvc.perform(get("/api/v1/search/fuzzy")
                        .param("q", "Burdwan")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThan(0)))
                .andExpect(jsonPath("$.results[0].entity.name", containsString("Bardhaman")));
    }

    @Test
    void testFuzzyTransliterationSearchCalcutta() throws Exception {
        mockMvc.perform(get("/api/v1/search/fuzzy")
                        .param("q", "Calcutta")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThan(0)))
                .andExpect(jsonPath("$.results[0].entity.name", containsString("Kolkata")));
    }

    @Test
    void testAnalyticsStats() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIndexedEntities", greaterThan(30)))
                .andExpect(jsonPath("$.spatialKdTreeDepth", greaterThan(3)))
                .andExpect(jsonPath("$.distinctStates", greaterThan(10)))
                .andExpect(jsonPath("$.memoryFootprintMb", notNullValue()));
    }

    @Test
    void testInvalidCoordinatesValidation() throws Exception {
        mockMvc.perform(get("/api/v1/spatial/reverse")
                        .param("lat", "125.0")
                        .param("lon", "88.3639")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid Coordinates")));
    }
}
