package com.bharatspatial.ingestion;

import com.bharatspatial.model.AdministrativeLevel;
import com.bharatspatial.model.AdministrativeNode;
import com.bharatspatial.model.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class StreamingCensusParser {
    private static final Logger log = LoggerFactory.getLogger(StreamingCensusParser.class);

    public static class IngestionReport {
        private int totalProcessed;
        private int totalValid;
        private int totalAnomalies;
        private long durationMillis;
        private final List<String> anomalySamples = new ArrayList<>();

        public int getTotalProcessed() { return totalProcessed; }
        public int getTotalValid() { return totalValid; }
        public int getTotalAnomalies() { return totalAnomalies; }
        public long getDurationMillis() { return durationMillis; }
        public List<String> getAnomalySamples() { return anomalySamples; }
    }

    public List<AdministrativeNode> parseFromStream(InputStream inputStream, IngestionReport report) {
        long startTime = System.currentTimeMillis();
        List<AdministrativeNode> nodes = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return nodes;
            }

            String[] headers = parseCsvLine(headerLine);
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colIndex.put(headers[i].trim().toLowerCase(), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                report.totalProcessed++;

                try {
                    String[] tokens = parseCsvLine(line);
                    AdministrativeNode node = buildNodeFromTokens(tokens, colIndex);
                    if (node != null) {
                        nodes.add(node);
                        report.totalValid++;
                    } else {
                        report.totalAnomalies++;
                    }
                } catch (Exception e) {
                    report.totalAnomalies++;
                    if (report.anomalySamples.size() < 10) {
                        report.anomalySamples.add("Line " + report.totalProcessed + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Fatal error during streaming census parse", e);
        }

        report.durationMillis = System.currentTimeMillis() - startTime;
        log.info("Ingestion complete: {} processed, {} valid, {} anomalies in {}ms",
                report.totalProcessed, report.totalValid, report.totalAnomalies, report.durationMillis);
        return nodes;
    }

    private AdministrativeNode buildNodeFromTokens(String[] tokens, Map<String, Integer> col) {
        String mddsCode = getVal(tokens, col, "mddscode");
        String name = getVal(tokens, col, "name");
        String stateCode = getVal(tokens, col, "statecode");
        String stateName = getVal(tokens, col, "statename");
        String districtCode = getVal(tokens, col, "districtcode");
        String districtName = getVal(tokens, col, "districtname");
        String subDistrictCode = getVal(tokens, col, "subdistrictcode");
        String subDistrictName = getVal(tokens, col, "subdistrictname");
        String gpName = getVal(tokens, col, "grampanchayatname");
        String lgdCode = getVal(tokens, col, "lgdcode");
        String localName = getVal(tokens, col, "localname");
        String levelStr = getVal(tokens, col, "level");
        String pincode = getVal(tokens, col, "pincode");
        String category = getVal(tokens, col, "category");

        double lat = parseDouble(getVal(tokens, col, "latitude"), Double.NaN);
        double lon = parseDouble(getVal(tokens, col, "longitude"), Double.NaN);
        long population = parseLong(getVal(tokens, col, "population"), 0L);
        double area = parseDouble(getVal(tokens, col, "areasqkm"), 0.0);

        if (mddsCode.isBlank() || name.isBlank() || Double.isNaN(lat) || Double.isNaN(lon)) {
            return null;
        }

        GeoPoint point = new GeoPoint(lat, lon);
        if (!point.isWithinIndiaBounds()) {
            return null; // GPS Anomaly outside geographical India
        }

        AdministrativeLevel level = AdministrativeLevel.VILLAGE;
        if (levelStr != null && !levelStr.isBlank()) {
            try {
                level = AdministrativeLevel.valueOf(levelStr.trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        return AdministrativeNode.builder()
                .mddsCode(mddsCode)
                .lgdCode(lgdCode)
                .name(name)
                .localName(localName)
                .level(level)
                .stateCode(stateCode)
                .stateName(stateName)
                .districtCode(districtCode)
                .districtName(districtName)
                .subDistrictCode(subDistrictCode)
                .subDistrictName(subDistrictName)
                .gramPanchayatName(gpName)
                .location(point)
                .population(population)
                .areaSqKm(area)
                .pincode(pincode)
                .category(category.isBlank() ? "RURAL" : category)
                .build();
    }

    /**
     * Synthesizes realistic Indian villages & settlements anchored to authentic state/district centroids
     * to test high-volume indexing (up to 100,000+ nodes) with exact spatial correctness.
     */
    public List<AdministrativeNode> generateScaleDataset(List<AdministrativeNode> seeds, int targetCount) {
        if (seeds == null || seeds.isEmpty()) return Collections.emptyList();
        List<AdministrativeNode> dataset = new ArrayList<>(targetCount);
        dataset.addAll(seeds);

        Random rng = new Random(42); // Deterministic seed for repeatable benchmarks
        int needed = targetCount - seeds.size();
        String[] prefixes = {"Dakshin", "Uttar", "Purba", "Paschim", "Naya", "Purana", "Chhota", "Bada", "Rampur", "Govindpur", "Madhopur", "Kalyanpur", "Fatehpur", "Shivpur", "Gopalpur"};
        String[] suffixes = {"Gaon", "Pur", "Nagar", "Kalan", "Khurd", "Danga", "Patti", "Ghat", "Dih", "Basti", "Tola", "Ganj", "Bari", "Majra", "Kheda"};

        for (int i = 0; i < needed; i++) {
            AdministrativeNode seed = seeds.get(rng.nextInt(seeds.size()));

            // Jitter coordinates within ~15km radius of the seed centroid
            double latJitter = (rng.nextDouble() - 0.5) * 0.25;
            double lonJitter = (rng.nextDouble() - 0.5) * 0.25;
            double lat = seed.getLatitude() + latJitter;
            double lon = seed.getLongitude() + lonJitter;

            String synName = prefixes[rng.nextInt(prefixes.length)] + " " + suffixes[rng.nextInt(suffixes.length)];
            String synMdds = seed.getStateCode() + String.format("%06d", (100000 + i));
            String synLgd = seed.getStateCode() + "V" + (200000 + i);

            long synPop = 200 + rng.nextInt(12000);
            double synArea = 1.5 + (rng.nextDouble() * 12.0);

            dataset.add(AdministrativeNode.builder()
                    .mddsCode(synMdds)
                    .lgdCode(synLgd)
                    .name(synName)
                    .localName("")
                    .level(AdministrativeLevel.VILLAGE)
                    .stateCode(seed.getStateCode())
                    .stateName(seed.getStateName())
                    .districtCode(seed.getDistrictCode())
                    .districtName(seed.getDistrictName())
                    .subDistrictCode(seed.getSubDistrictCode())
                    .subDistrictName(seed.getSubDistrictName())
                    .gramPanchayatName(synName + " GP")
                    .location(new GeoPoint(lat, lon))
                    .population(synPop)
                    .areaSqKm(synArea)
                    .pincode(seed.getPincode())
                    .category("RURAL")
                    .build());
        }

        return dataset;
    }

    private String getVal(String[] tokens, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        if (idx != null && idx < tokens.length) {
            return tokens[idx].trim();
        }
        return "";
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }

    private long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }

    private String[] parseCsvLine(String line) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString());
        return list.toArray(new String[0]);
    }
}
