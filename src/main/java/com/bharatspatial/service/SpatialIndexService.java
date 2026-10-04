package com.bharatspatial.service;

import com.bharatspatial.dto.*;
import com.bharatspatial.exception.EntityNotFoundException;
import com.bharatspatial.exception.InvalidCoordinateException;
import com.bharatspatial.ingestion.StreamingCensusParser;
import com.bharatspatial.model.AdministrativeLevel;
import com.bharatspatial.model.AdministrativeNode;
import com.bharatspatial.model.GeoPoint;
import com.bharatspatial.model.SpatialBoundingBox;
import com.bharatspatial.search.TrigramFuzzyIndex;
import com.bharatspatial.spatial.SpatialKDTree;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SpatialIndexService {
    private static final Logger log = LoggerFactory.getLogger(SpatialIndexService.class);

    private final ResourceLoader resourceLoader;
    private final StreamingCensusParser parser;

    @Value("${bharat-spatial.data.preload-default:true}")
    private boolean preloadDefault;

    @Value("${bharat-spatial.data.synthetic-scale-count:0}")
    private int syntheticScaleCount;

    @Value("${bharat-spatial.data.dataset-path:classpath:data/mdds_india_settlements.csv}")
    private String datasetPath;

    @Value("${bharat-spatial.cache.default-radius-km:10.0}")
    private double defaultRadiusKm;

    @Value("${bharat-spatial.cache.max-radius-km:150.0}")
    private double maxRadiusKm;

    private final SpatialKDTree kdTree = new SpatialKDTree();
    private final TrigramFuzzyIndex fuzzyIndex = new TrigramFuzzyIndex();
    private final Map<String, AdministrativeNode> mddsMap = new ConcurrentHashMap<>();
    private final Map<String, List<AdministrativeNode>> stateMap = new ConcurrentHashMap<>();
    private final Map<String, List<AdministrativeNode>> districtMap = new ConcurrentHashMap<>();
    private final Map<String, List<AdministrativeNode>> subDistrictMap = new ConcurrentHashMap<>();

    private volatile boolean ready = false;

    public SpatialIndexService(ResourceLoader resourceLoader, StreamingCensusParser parser) {
        this.resourceLoader = resourceLoader;
        this.parser = parser;
    }

    @PostConstruct
    public synchronized void initialize() {
        if (!preloadDefault) {
            log.info("Spatial preload disabled by configuration.");
            ready = true;
            return;
        }

        try {
            long startNanos = System.nanoTime();
            Resource resource = resourceLoader.getResource(datasetPath);
            StreamingCensusParser.IngestionReport report = new StreamingCensusParser.IngestionReport();

            List<AdministrativeNode> loadedNodes;
            try (InputStream is = resource.getInputStream()) {
                loadedNodes = parser.parseFromStream(is, report);
            }

            if (syntheticScaleCount > loadedNodes.size()) {
                log.info("Scaling in-memory spatial index to {} nodes for stress testing...", syntheticScaleCount);
                loadedNodes = parser.generateScaleDataset(loadedNodes, syntheticScaleCount);
            }

            // Build primary in-memory indexes
            kdTree.build(loadedNodes);
            fuzzyIndex.index(loadedNodes);

            mddsMap.clear();
            stateMap.clear();
            districtMap.clear();
            subDistrictMap.clear();

            for (AdministrativeNode node : loadedNodes) {
                mddsMap.put(node.getMddsCode(), node);
                stateMap.computeIfAbsent(node.getStateCode(), k -> new ArrayList<>()).add(node);
                districtMap.computeIfAbsent(node.getDistrictCode(), k -> new ArrayList<>()).add(node);
                subDistrictMap.computeIfAbsent(node.getSubDistrictCode(), k -> new ArrayList<>()).add(node);
            }

            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
            ready = true;

            Runtime runtime = Runtime.getRuntime();
            long usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

            log.info("===============================================================");
            log.info("BHARAT SPATIAL ENGINE INDEX READY:");
            log.info("  - Total Entities: {}", loadedNodes.size());
            log.info("  - KD-Tree Depth : {}", kdTree.getMaxDepth());
            log.info("  - Trigrams      : {}", fuzzyIndex.getIndexedTrigramsCount());
            log.info("  - Memory Footprint : ~{} MB", usedMemMb);
            log.info("  - Index Build Time : {} ms", elapsedMillis);
            log.info("===============================================================");
        } catch (Exception e) {
            log.error("Failed to initialize Bharat Spatial Index", e);
            ready = false;
        }
    }

    public ReverseGeoResponse reverseGeocode(double lat, double lon) {
        validateCoordinates(lat, lon);
        long start = System.nanoTime();
        GeoPoint query = new GeoPoint(lat, lon);

        Optional<SpatialKDTree.ScoredNode> nearest = kdTree.findNearestNeighbor(query);
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

        if (nearest.isEmpty()) {
            throw new EntityNotFoundException("No settlements found within coverage area");
        }

        SpatialKDTree.ScoredNode scored = nearest.get();
        double bearing = query.bearingTo(scored.getNode().getLocation());
        return new ReverseGeoResponse(query, scored.getNode(), scored.getDistanceMeters(), bearing, elapsedMs);
    }

    public SpatialNearbyResponse findNearby(double lat, double lon, Double radiusKm, Integer limit) {
        validateCoordinates(lat, lon);
        double rad = (radiusKm != null && radiusKm > 0.0) ? Math.min(radiusKm, maxRadiusKm) : defaultRadiusKm;
        int maxResults = (limit != null && limit > 0) ? Math.min(limit, 500) : 50;

        long start = System.nanoTime();
        GeoPoint query = new GeoPoint(lat, lon);

        List<SpatialKDTree.ScoredNode> withinRadius = kdTree.findWithinRadius(query, rad);
        if (withinRadius.size() > maxResults) {
            withinRadius = withinRadius.subList(0, maxResults);
        }

        List<SpatialNearbyResponse.NearbyItem> items = withinRadius.stream()
                .map(scored -> {
                    double bearing = query.bearingTo(scored.getNode().getLocation());
                    return new SpatialNearbyResponse.NearbyItem(scored.getNode(), scored.getDistanceMeters(), bearing);
                })
                .collect(Collectors.toList());

        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        return new SpatialNearbyResponse(query, rad, items, elapsedMs);
    }

    public SpatialNearbyResponse findKnn(double lat, double lon, int k) {
        validateCoordinates(lat, lon);
        int clampedK = Math.min(Math.max(k, 1), 200);

        long start = System.nanoTime();
        GeoPoint query = new GeoPoint(lat, lon);

        List<SpatialKDTree.ScoredNode> knn = kdTree.findKNearestNeighbors(query, clampedK);

        List<SpatialNearbyResponse.NearbyItem> items = knn.stream()
                .map(scored -> {
                    double bearing = query.bearingTo(scored.getNode().getLocation());
                    return new SpatialNearbyResponse.NearbyItem(scored.getNode(), scored.getDistanceMeters(), bearing);
                })
                .collect(Collectors.toList());

        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        return new SpatialNearbyResponse(query, 0.0, items, elapsedMs);
    }

    public List<AdministrativeNode> findWithinBoundingBox(double minLat, double minLon, double maxLat, double maxLon) {
        validateCoordinates(minLat, minLon);
        validateCoordinates(maxLat, maxLon);
        SpatialBoundingBox bbox = new SpatialBoundingBox(minLat, minLon, maxLat, maxLon);
        return kdTree.findWithinBoundingBox(bbox);
    }

    public FuzzySearchResponse fuzzySearch(String query, Integer limit) {
        int maxResults = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;
        long start = System.nanoTime();

        List<TrigramFuzzyIndex.MatchResult> matches = fuzzyIndex.search(query, maxResults);
        List<FuzzySearchResponse.SearchResultItem> items = matches.stream()
                .map(m -> new FuzzySearchResponse.SearchResultItem(m.getNode(), m.getScore(), m.getMatchedTerm()))
                .collect(Collectors.toList());

        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        return new FuzzySearchResponse(query, items, elapsedMs);
    }

    public HierarchyResponse getHierarchyByMdds(String mddsCode) {
        AdministrativeNode node = mddsMap.get(mddsCode);
        if (node == null) {
            throw new EntityNotFoundException("Entity with MDDS code " + mddsCode + " not found");
        }

        List<AdministrativeNode> children = Collections.emptyList();
        long rollupPop = node.getPopulation();

        if (node.getLevel() == AdministrativeLevel.STATE) {
            children = stateMap.getOrDefault(node.getStateCode(), Collections.emptyList());
            rollupPop = children.stream().mapToLong(AdministrativeNode::getPopulation).sum();
        } else if (node.getLevel() == AdministrativeLevel.DISTRICT) {
            children = districtMap.getOrDefault(node.getDistrictCode(), Collections.emptyList());
            rollupPop = children.stream().mapToLong(AdministrativeNode::getPopulation).sum();
        } else if (node.getLevel() == AdministrativeLevel.SUB_DISTRICT) {
            children = subDistrictMap.getOrDefault(node.getSubDistrictCode(), Collections.emptyList());
            rollupPop = children.stream().mapToLong(AdministrativeNode::getPopulation).sum();
        }

        return new HierarchyResponse(
                node.getMddsCode(),
                node.getName(),
                node.getLevel(),
                node.getHierarchyBreadcrumb(),
                children,
                children.size(),
                rollupPop
        );
    }

    public List<Map<String, Object>> listStates() {
        Map<String, List<AdministrativeNode>> byState = stateMap;
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, List<AdministrativeNode>> entry : byState.entrySet()) {
            List<AdministrativeNode> nodes = entry.getValue();
            if (nodes.isEmpty()) continue;
            AdministrativeNode sample = nodes.get(0);

            Map<String, Object> stateInfo = new LinkedHashMap<>();
            stateInfo.put("stateCode", entry.getKey());
            stateInfo.put("stateName", sample.getStateName());
            stateInfo.put("settlementCount", nodes.size());
            stateInfo.put("totalPopulation", nodes.stream().mapToLong(AdministrativeNode::getPopulation).sum());
            result.add(stateInfo);
        }

        result.sort((a, b) -> ((String) a.get("stateName")).compareTo((String) b.get("stateName")));
        return result;
    }

    public List<Map<String, Object>> listDistricts(String stateCode) {
        List<AdministrativeNode> stateNodes = stateMap.get(stateCode);
        if (stateNodes == null || stateNodes.isEmpty()) {
            throw new EntityNotFoundException("State with code " + stateCode + " not found");
        }

        Map<String, List<AdministrativeNode>> byDistrict = stateNodes.stream()
                .collect(Collectors.groupingBy(AdministrativeNode::getDistrictCode));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<AdministrativeNode>> entry : byDistrict.entrySet()) {
            List<AdministrativeNode> dNodes = entry.getValue();
            AdministrativeNode sample = dNodes.get(0);

            Map<String, Object> distInfo = new LinkedHashMap<>();
            distInfo.put("districtCode", entry.getKey());
            distInfo.put("districtName", sample.getDistrictName());
            distInfo.put("stateCode", sample.getStateCode());
            distInfo.put("stateName", sample.getStateName());
            distInfo.put("settlementCount", dNodes.size());
            distInfo.put("totalPopulation", dNodes.stream().mapToLong(AdministrativeNode::getPopulation).sum());
            result.add(distInfo);
        }

        result.sort((a, b) -> ((String) a.get("districtName")).compareTo((String) b.get("districtName")));
        return result;
    }

    public EngineStatsResponse getEngineStats() {
        int total = kdTree.size();
        int depth = kdTree.getMaxDepth();
        int states = stateMap.size();
        int districts = districtMap.size();
        int subDistricts = subDistrictMap.size();
        int trigrams = fuzzyIndex.getIndexedTrigramsCount();

        long totalPop = mddsMap.values().stream().mapToLong(AdministrativeNode::getPopulation).sum();
        Runtime rt = Runtime.getRuntime();
        long memMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        Map<String, Long> stateCounts = new TreeMap<>();
        for (Map.Entry<String, List<AdministrativeNode>> entry : stateMap.entrySet()) {
            String name = entry.getValue().isEmpty() ? entry.getKey() : entry.getValue().get(0).getStateName();
            stateCounts.put(name, (long) entry.getValue().size());
        }

        boolean vtActive = Thread.currentThread().isVirtual();

        return new EngineStatsResponse(
                total, depth, states, districts, subDistricts,
                trigrams, totalPop, memMb + " MB", vtActive, stateCounts
        );
    }

    public boolean isReady() {
        return ready;
    }

    private void validateCoordinates(double lat, double lon) {
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
            throw new InvalidCoordinateException("Coordinates out of range: [" + lat + ", " + lon + "]");
        }
    }
}
