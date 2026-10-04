package com.bharatspatial.dto;

import com.bharatspatial.model.AdministrativeLevel;
import com.bharatspatial.model.AdministrativeNode;

import java.util.List;

public class HierarchyResponse {
    private final String code;
    private final String name;
    private final AdministrativeLevel level;
    private final String breadcrumb;
    private final List<AdministrativeNode> directChildren;
    private final int totalChildCount;
    private final long totalPopulationRollup;

    public HierarchyResponse(String code, String name, AdministrativeLevel level,
                             String breadcrumb, List<AdministrativeNode> directChildren,
                             int totalChildCount, long totalPopulationRollup) {
        this.code = code;
        this.name = name;
        this.level = level;
        this.breadcrumb = breadcrumb;
        this.directChildren = directChildren;
        this.totalChildCount = totalChildCount;
        this.totalPopulationRollup = totalPopulationRollup;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public AdministrativeLevel getLevel() { return level; }
    public String getBreadcrumb() { return breadcrumb; }
    public List<AdministrativeNode> getDirectChildren() { return directChildren; }
    public int getTotalChildCount() { return totalChildCount; }
    public long getTotalPopulationRollup() { return totalPopulationRollup; }
}
