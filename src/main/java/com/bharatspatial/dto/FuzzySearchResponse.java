package com.bharatspatial.dto;

import com.bharatspatial.model.AdministrativeNode;

import java.util.List;

public class FuzzySearchResponse {
    public static class SearchResultItem {
        private final AdministrativeNode entity;
        private final double score;
        private final String matchedTerm;
        private final String breadcrumb;

        public SearchResultItem(AdministrativeNode entity, double score, String matchedTerm) {
            this.entity = entity;
            this.score = Math.round(score * 1000.0) / 1000.0;
            this.matchedTerm = matchedTerm;
            this.breadcrumb = entity != null ? entity.getHierarchyBreadcrumb() : "";
        }

        public AdministrativeNode getEntity() { return entity; }
        public double getScore() { return score; }
        public String getMatchedTerm() { return matchedTerm; }
        public String getBreadcrumb() { return breadcrumb; }
    }

    private final String query;
    private final int count;
    private final List<SearchResultItem> results;
    private final double executionTimeMs;

    public FuzzySearchResponse(String query, List<SearchResultItem> results, double executionTimeMs) {
        this.query = query;
        this.results = results;
        this.count = results != null ? results.size() : 0;
        this.executionTimeMs = Math.round(executionTimeMs * 1000.0) / 1000.0;
    }

    public String getQuery() { return query; }
    public int getCount() { return count; }
    public List<SearchResultItem> getResults() { return results; }
    public double getExecutionTimeMs() { return executionTimeMs; }
}
