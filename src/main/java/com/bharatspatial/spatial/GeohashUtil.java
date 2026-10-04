package com.bharatspatial.spatial;

import com.bharatspatial.model.GeoPoint;
import com.bharatspatial.model.SpatialBoundingBox;

import java.util.ArrayList;
import java.util.List;

public final class GeohashUtil {
    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
    private static final int[] BITS = {16, 8, 4, 2, 1};

    private GeohashUtil() {}

    /**
     * Encodes latitude and longitude into a geohash of given precision length (default 6 or 7).
     */
    public static String encode(double latitude, double longitude, int precision) {
        if (precision < 1 || precision > 12) {
            precision = 7;
        }

        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        StringBuilder geohash = new StringBuilder();

        boolean isEven = true;
        int bit = 0;
        int ch = 0;

        while (geohash.length() < precision) {
            if (isEven) {
                double mid = (lonRange[0] + lonRange[1]) / 2.0;
                if (longitude >= mid) {
                    ch |= BITS[bit];
                    lonRange[0] = mid;
                } else {
                    lonRange[1] = mid;
                }
            } else {
                double mid = (latRange[0] + latRange[1]) / 2.0;
                if (latitude >= mid) {
                    ch |= BITS[bit];
                    latRange[0] = mid;
                } else {
                    latRange[1] = mid;
                }
            }

            isEven = !isEven;
            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }

        return geohash.toString();
    }

    public static String encode(GeoPoint point, int precision) {
        return encode(point.getLatitude(), point.getLongitude(), precision);
    }

    /**
     * Decodes a geohash string into its center GeoPoint.
     */
    public static GeoPoint decode(String geohash) {
        SpatialBoundingBox bbox = decodeBoundingBox(geohash);
        double centerLat = (bbox.getMinLat() + bbox.getMaxLat()) / 2.0;
        double centerLon = (bbox.getMinLon() + bbox.getMaxLon()) / 2.0;
        return new GeoPoint(centerLat, centerLon);
    }

    /**
     * Returns the bounding box corresponding to a geohash.
     */
    public static SpatialBoundingBox decodeBoundingBox(String geohash) {
        if (geohash == null || geohash.isEmpty()) {
            throw new IllegalArgumentException("Geohash cannot be null or empty");
        }
        geohash = geohash.toLowerCase();

        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};

        boolean isEven = true;
        for (int i = 0; i < geohash.length(); i++) {
            char c = geohash.charAt(i);
            int cd = BASE32.indexOf(c);
            if (cd == -1) {
                throw new IllegalArgumentException("Invalid character in geohash: " + c);
            }

            for (int j = 0; j < 5; j++) {
                int mask = BITS[j];
                if (isEven) {
                    refineInterval(lonRange, cd, mask);
                } else {
                    refineInterval(latRange, cd, mask);
                }
                isEven = !isEven;
            }
        }

        return new SpatialBoundingBox(latRange[0], lonRange[0], latRange[1], lonRange[1]);
    }

    private static void refineInterval(double[] interval, int cd, int mask) {
        double mid = (interval[0] + interval[1]) / 2.0;
        if ((cd & mask) != 0) {
            interval[0] = mid;
        } else {
            interval[1] = mid;
        }
    }

    /**
     * Returns the 8 adjacent neighbor geohashes plus the center geohash (total 9 cells).
     */
    public static List<String> getNeighbors(String geohash) {
        SpatialBoundingBox bbox = decodeBoundingBox(geohash);
        double latHeight = bbox.getMaxLat() - bbox.getMinLat();
        double lonWidth = bbox.getMaxLon() - bbox.getMinLon();
        double centerLat = (bbox.getMinLat() + bbox.getMaxLat()) / 2.0;
        double centerLon = (bbox.getMinLon() + bbox.getMaxLon()) / 2.0;
        int precision = geohash.length();

        List<String> neighbors = new ArrayList<>(9);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                double nLat = centerLat + (dy * latHeight);
                double nLon = centerLon + (dx * lonWidth);
                if (nLat >= -90.0 && nLat <= 90.0 && nLon >= -180.0 && nLon <= 180.0) {
                    neighbors.add(encode(nLat, nLon, precision));
                }
            }
        }
        return neighbors;
    }
}
