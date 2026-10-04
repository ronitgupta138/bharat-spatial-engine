package com.bharatspatial.search;

import com.bharatspatial.model.AdministrativeNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrigramFuzzyIndex {
    public static class MatchResult implements Comparable<MatchResult> {
        private final AdministrativeNode node;
        private final double score; // 0.0 to 1.0
        private final String matchedTerm;

        public MatchResult(AdministrativeNode node, double score, String matchedTerm) {
            this.node = node;
            this.score = score;
            this.matchedTerm = matchedTerm;
        }

        public AdministrativeNode getNode() { return node; }
        public double getScore() { return score; }
        public String getMatchedTerm() { return matchedTerm; }

        @Override
        public int compareTo(MatchResult o) {
            return Double.compare(o.score, this.score); // Descending score
        }
    }

    private final Map<String, Set<AdministrativeNode>> trigramIndex = new ConcurrentHashMap<>();
    private final Map<String, String> aliasDictionary = new ConcurrentHashMap<>();
    private final List<AdministrativeNode> allNodes = new ArrayList<>();

    public TrigramFuzzyIndex() {
        aliasDictionary.put("calcutta", "kolkata");
        aliasDictionary.put("burdwan", "bardhaman");
        aliasDictionary.put("bangalore", "bengaluru");
        aliasDictionary.put("banaras", "varanasi");
        aliasDictionary.put("benares", "varanasi");
        aliasDictionary.put("kashi", "varanasi");
        aliasDictionary.put("bombay", "mumbai");
        aliasDictionary.put("madras", "chennai");
        aliasDictionary.put("trivandrum", "thiruvananthapuram");
        aliasDictionary.put("cochin", "kochi");
        aliasDictionary.put("calicut", "kozhikode");
        aliasDictionary.put("mysore", "mysuru");
        aliasDictionary.put("hubli", "hubballi");
        aliasDictionary.put("belgaum", "belagavi");
        aliasDictionary.put("pondicherry", "puducherry");
        aliasDictionary.put("allahabad", "prayagraj");
        aliasDictionary.put("poona", "pune");
        aliasDictionary.put("baroda", "vadodara");
        aliasDictionary.put("gurgaon", "gurugram");
        aliasDictionary.put("orissa", "odisha");
    }

    public synchronized void index(List<AdministrativeNode> nodes) {
        trigramIndex.clear();
        allNodes.clear();
        if (nodes == null) return;

        allNodes.addAll(nodes);
        for (AdministrativeNode node : nodes) {
            indexString(node.getName(), node);
            if (node.getLocalName() != null && !node.getLocalName().isBlank()) {
                indexString(node.getLocalName(), node);
            }
            if (node.getDistrictName() != null && !node.getDistrictName().isBlank()) {
                indexString(node.getDistrictName(), node);
            }
        }
    }

    private void indexString(String text, AdministrativeNode node) {
        if (text == null || text.isBlank()) return;
        Set<String> trigrams = generateTrigrams(text);
        for (String tri : trigrams) {
            trigramIndex.computeIfAbsent(tri, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(node);
        }
    }

    public List<MatchResult> search(String query, int limit) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        String normalizedQuery = normalize(query);

        String aliasResolved = aliasDictionary.getOrDefault(normalizedQuery, normalizedQuery);
        Set<String> queryTrigrams = generateTrigrams(aliasResolved);

        Map<AdministrativeNode, Integer> matchCounts = new HashMap<>();
        for (String tri : queryTrigrams) {
            Set<AdministrativeNode> matches = trigramIndex.get(tri);
            if (matches != null) {
                for (AdministrativeNode n : matches) {
                    matchCounts.merge(n, 1, Integer::sum);
                }
            }
        }

        List<MatchResult> results = new ArrayList<>();
        int qLen = queryTrigrams.size();

        for (Map.Entry<AdministrativeNode, Integer> entry : matchCounts.entrySet()) {
            AdministrativeNode node = entry.getKey();
            int shared = entry.getValue();

            String nameNorm = normalize(node.getName());
            String distNorm = normalize(node.getDistrictName());

            double nameScore = scoreTerm(aliasResolved, normalizedQuery, nameNorm, queryTrigrams, shared);
            double distScore = scoreTerm(aliasResolved, normalizedQuery, distNorm, queryTrigrams, shared) * 0.7; // District discount

            double bestScore = Math.max(nameScore, distScore);

            // Direct substring containment boost on name
            if (nameNorm.contains(aliasResolved) || aliasResolved.contains(nameNorm)) {
                bestScore = Math.max(bestScore, 0.95);
            }
            if (nameNorm.equalsIgnoreCase(aliasResolved)) {
                bestScore = 1.0;
            }

            if (bestScore >= 0.30) {
                String matchedTerm = (nameScore >= distScore) ? node.getName() : node.getDistrictName();
                results.add(new MatchResult(node, bestScore, matchedTerm));
            }
        }

        Collections.sort(results);
        if (results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }

    private double scoreTerm(String aliasResolved, String rawQuery, String targetNorm,
                             Set<String> queryTrigrams, int shared) {
        if (targetNorm == null || targetNorm.isBlank()) return 0.0;
        Set<String> targetTrigrams = generateTrigrams(targetNorm);
        int total = queryTrigrams.size() + targetTrigrams.size() - shared;
        double jaccard = total > 0 ? (double) shared / total : 0.0;

        if (targetNorm.startsWith(aliasResolved) || targetNorm.startsWith(rawQuery)) {
            jaccard = Math.min(1.0, jaccard + 0.35);
        }

        int lev = levenshteinDistance(aliasResolved, targetNorm);
        double levRatio = 1.0 - ((double) lev / Math.max(aliasResolved.length(), targetNorm.length()));
        return Math.max(0.0, (jaccard * 0.6) + (levRatio * 0.4));
    }

    public static Set<String> generateTrigrams(String text) {
        Set<String> set = new HashSet<>();
        String s = "  " + normalize(text) + "  ";
        for (int i = 0; i <= s.length() - 3; i++) {
            set.add(s.substring(i, i + 3));
        }
        return set;
    }

    public static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    public static int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    public int getIndexedTrigramsCount() {
        return trigramIndex.size();
    }
}
