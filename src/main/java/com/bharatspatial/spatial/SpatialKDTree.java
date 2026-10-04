package com.bharatspatial.spatial;

import com.bharatspatial.model.AdministrativeNode;
import com.bharatspatial.model.GeoPoint;
import com.bharatspatial.model.SpatialBoundingBox;

import java.util.*;

public class SpatialKDTree {
    private static final double METERS_PER_DEGREE_LAT = 111_132.95;

    public static class ScoredNode implements Comparable<ScoredNode> {
        private final AdministrativeNode node;
        private final double distanceMeters;

        public ScoredNode(AdministrativeNode node, double distanceMeters) {
            this.node = node;
            this.distanceMeters = distanceMeters;
        }

        public AdministrativeNode getNode() { return node; }
        public double getDistanceMeters() { return distanceMeters; }
        public double getDistanceKm() { return distanceMeters / 1000.0; }

        @Override
        public int compareTo(ScoredNode o) {
            return Double.compare(this.distanceMeters, o.distanceMeters);
        }
    }

    private static class KDNode {
        AdministrativeNode data;
        KDNode left;
        KDNode right;
        int depth;

        KDNode(AdministrativeNode data, int depth) {
            this.data = data;
            this.depth = depth;
        }
    }

    private KDNode root;
    private int size = 0;
    private int maxDepth = 0;

    public synchronized void build(List<AdministrativeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            this.root = null;
            this.size = 0;
            this.maxDepth = 0;
            return;
        }
        List<AdministrativeNode> cleanList = new ArrayList<>(nodes);
        this.size = cleanList.size();
        this.maxDepth = 0;
        this.root = buildRecursive(cleanList, 0);
    }

    private KDNode buildRecursive(List<AdministrativeNode> nodes, int depth) {
        if (nodes.isEmpty()) return null;
        if (depth > maxDepth) maxDepth = depth;

        int axis = depth % 2; // 0 for latitude, 1 for longitude
        nodes.sort((a, b) -> {
            if (axis == 0) {
                return Double.compare(a.getLatitude(), b.getLatitude());
            } else {
                return Double.compare(a.getLongitude(), b.getLongitude());
            }
        });

        int medianIndex = nodes.size() / 2;
        AdministrativeNode medianNode = nodes.get(medianIndex);
        KDNode current = new KDNode(medianNode, depth);

        current.left = buildRecursive(new ArrayList<>(nodes.subList(0, medianIndex)), depth + 1);
        current.right = buildRecursive(new ArrayList<>(nodes.subList(medianIndex + 1, nodes.size())), depth + 1);
        return current;
    }

    public Optional<ScoredNode> findNearestNeighbor(GeoPoint query) {
        if (root == null || query == null) return Optional.empty();
        List<ScoredNode> knn = findKNearestNeighbors(query, 1);
        return knn.isEmpty() ? Optional.empty() : Optional.of(knn.get(0));
    }

    public List<ScoredNode> findKNearestNeighbors(GeoPoint query, int k) {
        if (root == null || query == null || k <= 0) return Collections.emptyList();

        // Max-heap ordered by distance (largest distance at top)
        PriorityQueue<ScoredNode> maxHeap = new PriorityQueue<>(k, (a, b) ->
                Double.compare(b.distanceMeters, a.distanceMeters));

        knnRecursive(root, query, k, maxHeap);

        List<ScoredNode> result = new ArrayList<>(maxHeap);
        Collections.sort(result); // Ascending order by distance
        return result;
    }

    private void knnRecursive(KDNode current, GeoPoint query, int k, PriorityQueue<ScoredNode> maxHeap) {
        if (current == null) return;

        double distMeters = query.distanceToMeters(current.data.getLocation());

        if (maxHeap.size() < k) {
            maxHeap.offer(new ScoredNode(current.data, distMeters));
        } else if (distMeters < maxHeap.peek().distanceMeters) {
            maxHeap.poll();
            maxHeap.offer(new ScoredNode(current.data, distMeters));
        }

        int axis = current.depth % 2;
        double queryCoord = (axis == 0) ? query.getLatitude() : query.getLongitude();
        double nodeCoord = (axis == 0) ? current.data.getLatitude() : current.data.getLongitude();

        KDNode firstBranch = queryCoord < nodeCoord ? current.left : current.right;
        KDNode secondBranch = queryCoord < nodeCoord ? current.right : current.left;

        knnRecursive(firstBranch, query, k, maxHeap);

        // Distance from query point to the splitting axis plane
        double planeDistMeters;
        if (axis == 0) {
            planeDistMeters = Math.abs(query.getLatitude() - current.data.getLatitude()) * METERS_PER_DEGREE_LAT;
        } else {
            double radLat = Math.toRadians(query.getLatitude());
            planeDistMeters = Math.abs(query.getLongitude() - current.data.getLongitude()) *
                              METERS_PER_DEGREE_LAT * Math.cos(radLat);
        }

        // Only search the other side if the splitting plane intersects current sphere
        if (maxHeap.size() < k || planeDistMeters < maxHeap.peek().distanceMeters) {
            knnRecursive(secondBranch, query, k, maxHeap);
        }
    }

    public List<ScoredNode> findWithinRadius(GeoPoint query, double radiusKm) {
        if (root == null || query == null || radiusKm <= 0.0) return Collections.emptyList();
        double radiusMeters = radiusKm * 1000.0;
        List<ScoredNode> results = new ArrayList<>();
        radiusRecursive(root, query, radiusMeters, results);
        Collections.sort(results);
        return results;
    }

    private void radiusRecursive(KDNode current, GeoPoint query, double radiusMeters, List<ScoredNode> results) {
        if (current == null) return;

        double distMeters = query.distanceToMeters(current.data.getLocation());
        if (distMeters <= radiusMeters) {
            results.add(new ScoredNode(current.data, distMeters));
        }

        int axis = current.depth % 2;
        double queryCoord = (axis == 0) ? query.getLatitude() : query.getLongitude();
        double nodeCoord = (axis == 0) ? current.data.getLatitude() : current.data.getLongitude();

        KDNode firstBranch = queryCoord < nodeCoord ? current.left : current.right;
        KDNode secondBranch = queryCoord < nodeCoord ? current.right : current.left;

        radiusRecursive(firstBranch, query, radiusMeters, results);

        double planeDistMeters;
        if (axis == 0) {
            planeDistMeters = Math.abs(query.getLatitude() - current.data.getLatitude()) * METERS_PER_DEGREE_LAT;
        } else {
            double radLat = Math.toRadians(query.getLatitude());
            planeDistMeters = Math.abs(query.getLongitude() - current.data.getLongitude()) *
                              METERS_PER_DEGREE_LAT * Math.cos(radLat);
        }

        if (planeDistMeters <= radiusMeters) {
            radiusRecursive(secondBranch, query, radiusMeters, results);
        }
    }

    public List<AdministrativeNode> findWithinBoundingBox(SpatialBoundingBox bbox) {
        if (root == null || bbox == null) return Collections.emptyList();
        List<AdministrativeNode> results = new ArrayList<>();
        bboxRecursive(root, bbox, results);
        return results;
    }

    private void bboxRecursive(KDNode current, SpatialBoundingBox bbox, List<AdministrativeNode> results) {
        if (current == null) return;

        if (bbox.contains(current.data.getLocation())) {
            results.add(current.data);
        }

        int axis = current.depth % 2;
        double nodeCoord = (axis == 0) ? current.data.getLatitude() : current.data.getLongitude();
        double minCoord = (axis == 0) ? bbox.getMinLat() : bbox.getMinLon();
        double maxCoord = (axis == 0) ? bbox.getMaxLat() : bbox.getMaxLon();

        if (minCoord <= nodeCoord) {
            bboxRecursive(current.left, bbox, results);
        }
        if (maxCoord >= nodeCoord) {
            bboxRecursive(current.right, bbox, results);
        }
    }

    public int size() { return size; }
    public int getMaxDepth() { return maxDepth; }
}
