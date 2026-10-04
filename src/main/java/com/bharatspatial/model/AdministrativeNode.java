package com.bharatspatial.model;

import java.util.Objects;

public class AdministrativeNode {
    private final String mddsCode;
    private final String lgdCode;
    private final String name;
    private final String localName;
    private final AdministrativeLevel level;
    private final String stateCode;
    private final String stateName;
    private final String districtCode;
    private final String districtName;
    private final String subDistrictCode;
    private final String subDistrictName;
    private final String gramPanchayatName;
    private final GeoPoint location;
    private final long population;
    private final double areaSqKm;
    private final String pincode;
    private final String category;

    private AdministrativeNode(Builder builder) {
        this.mddsCode = builder.mddsCode;
        this.lgdCode = builder.lgdCode;
        this.name = builder.name;
        this.localName = builder.localName;
        this.level = builder.level;
        this.stateCode = builder.stateCode;
        this.stateName = builder.stateName;
        this.districtCode = builder.districtCode;
        this.districtName = builder.districtName;
        this.subDistrictCode = builder.subDistrictCode;
        this.subDistrictName = builder.subDistrictName;
        this.gramPanchayatName = builder.gramPanchayatName;
        this.location = builder.location;
        this.population = builder.population;
        this.areaSqKm = builder.areaSqKm;
        this.pincode = builder.pincode;
        this.category = builder.category;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMddsCode() { return mddsCode; }
    public String getLgdCode() { return lgdCode; }
    public String getName() { return name; }
    public String getLocalName() { return localName; }
    public AdministrativeLevel getLevel() { return level; }
    public String getStateCode() { return stateCode; }
    public String getStateName() { return stateName; }
    public String getDistrictCode() { return districtCode; }
    public String getDistrictName() { return districtName; }
    public String getSubDistrictCode() { return subDistrictCode; }
    public String getSubDistrictName() { return subDistrictName; }
    public String getGramPanchayatName() { return gramPanchayatName; }
    public GeoPoint getLocation() { return location; }
    public double getLatitude() { return location != null ? location.getLatitude() : 0.0; }
    public double getLongitude() { return location != null ? location.getLongitude() : 0.0; }
    public long getPopulation() { return population; }
    public double getAreaSqKm() { return areaSqKm; }
    public String getPincode() { return pincode; }
    public String getCategory() { return category; }

    public String getHierarchyBreadcrumb() {
        StringBuilder sb = new StringBuilder();
        if (stateName != null && !stateName.isBlank()) sb.append(stateName);
        if (districtName != null && !districtName.isBlank()) {
            if (sb.length() > 0) sb.append(" > ");
            sb.append(districtName);
        }
        if (subDistrictName != null && !subDistrictName.isBlank()) {
            if (sb.length() > 0) sb.append(" > ");
            sb.append(subDistrictName);
        }
        if (gramPanchayatName != null && !gramPanchayatName.isBlank()) {
            if (sb.length() > 0) sb.append(" > ");
            sb.append(gramPanchayatName);
        }
        if (name != null && !name.isBlank() && !name.equalsIgnoreCase(gramPanchayatName)) {
            if (sb.length() > 0) sb.append(" > ");
            sb.append(name);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdministrativeNode that = (AdministrativeNode) o;
        return Objects.equals(mddsCode, that.mddsCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mddsCode);
    }

    public static class Builder {
        private String mddsCode;
        private String lgdCode;
        private String name;
        private String localName;
        private AdministrativeLevel level = AdministrativeLevel.VILLAGE;
        private String stateCode;
        private String stateName;
        private String districtCode;
        private String districtName;
        private String subDistrictCode;
        private String subDistrictName;
        private String gramPanchayatName;
        private GeoPoint location;
        private long population;
        private double areaSqKm;
        private String pincode;
        private String category = "RURAL";

        public Builder mddsCode(String mddsCode) { this.mddsCode = mddsCode; return this; }
        public Builder lgdCode(String lgdCode) { this.lgdCode = lgdCode; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder localName(String localName) { this.localName = localName; return this; }
        public Builder level(AdministrativeLevel level) { this.level = level; return this; }
        public Builder stateCode(String stateCode) { this.stateCode = stateCode; return this; }
        public Builder stateName(String stateName) { this.stateName = stateName; return this; }
        public Builder districtCode(String districtCode) { this.districtCode = districtCode; return this; }
        public Builder districtName(String districtName) { this.districtName = districtName; return this; }
        public Builder subDistrictCode(String subDistrictCode) { this.subDistrictCode = subDistrictCode; return this; }
        public Builder subDistrictName(String subDistrictName) { this.subDistrictName = subDistrictName; return this; }
        public Builder gramPanchayatName(String gramPanchayatName) { this.gramPanchayatName = gramPanchayatName; return this; }
        public Builder location(GeoPoint location) { this.location = location; return this; }
        public Builder location(double lat, double lon) { this.location = new GeoPoint(lat, lon); return this; }
        public Builder population(long population) { this.population = population; return this; }
        public Builder areaSqKm(double areaSqKm) { this.areaSqKm = areaSqKm; return this; }
        public Builder pincode(String pincode) { this.pincode = pincode; return this; }
        public Builder category(String category) { this.category = category; return this; }

        public AdministrativeNode build() {
            return new AdministrativeNode(this);
        }
    }
}
