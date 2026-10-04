package com.bharatspatial.dto;

import java.util.Map;

public class EngineStatsResponse {
    private final int totalIndexedEntities;
    private final int spatialKdTreeDepth;
    private final int distinctStates;
    private final int distinctDistricts;
    private final int distinctSubDistricts;
    private final int trigramsIndexed;
    private final long totalPopulationCovered;
    private final String memoryFootprintMb;
    private final boolean virtualThreadsActive;
    private final Map<String, Long> stateCounts;

    public EngineStatsResponse(int totalIndexedEntities, int spatialKdTreeDepth,
                               int distinctStates, int distinctDistricts, int distinctSubDistricts,
                               int trigramsIndexed, long totalPopulationCovered,
                               String memoryFootprintMb, boolean virtualThreadsActive,
                               Map<String, Long> stateCounts) {
        this.totalIndexedEntities = totalIndexedEntities;
        this.spatialKdTreeDepth = spatialKdTreeDepth;
        this.distinctStates = distinctStates;
        this.distinctDistricts = distinctDistricts;
        this.distinctSubDistricts = distinctSubDistricts;
        this.trigramsIndexed = trigramsIndexed;
        this.totalPopulationCovered = totalPopulationCovered;
        this.memoryFootprintMb = memoryFootprintMb;
        this.virtualThreadsActive = virtualThreadsActive;
        this.stateCounts = stateCounts;
    }

    public int getTotalIndexedEntities() { return totalIndexedEntities; }
    public int getSpatialKdTreeDepth() { return spatialKdTreeDepth; }
    public int getDistinctStates() { return distinctStates; }
    public int getDistinctDistricts() { return distinctDistricts; }
    public int getDistinctSubDistricts() { return distinctSubDistricts; }
    public int getTrigramsIndexed() { return trigramsIndexed; }
    public long getTotalPopulationCovered() { return totalPopulationCovered; }
    public String getMemoryFootprintMb() { return memoryFootprintMb; }
    public boolean isVirtualThreadsActive() { return virtualThreadsActive; }
    public Map<String, Long> getStateCounts() { return stateCounts; }
}
