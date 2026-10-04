package com.bharatspatial.service;

import com.bharatspatial.dto.ReverseGeoResponse;
import com.bharatspatial.dto.SpatialNearbyResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SpatialEngineBenchmarkTest {
    private static final Logger log = LoggerFactory.getLogger(SpatialEngineBenchmarkTest.class);

    @Autowired
    private SpatialIndexService spatialService;

    @Test
    void benchmarkHighConcurrencyVirtualThreads() throws Exception {
        assertTrue(spatialService.isReady());

        // Warm-up phase to trigger JIT C2 compilation
        Random warmupRng = new Random(42);
        for (int i = 0; i < 500; i++) {
            double wLat = 12.0 + (warmupRng.nextDouble() * 16.0);
            double wLon = 72.0 + (warmupRng.nextDouble() * 16.0);
            spatialService.reverseGeocode(wLat, wLon);
            spatialService.findNearby(wLat, wLon, 15.0, 5);
        }

        int totalRequests = 1000;
        long[] latenciesNanos = new long[totalRequests];

        // Coordinate bounding box for test queries across India
        double minLat = 12.0, maxLat = 28.0;
        double minLon = 72.0, maxLon = 88.0;
        Random rng = new Random(138);

        long benchStart = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>(totalRequests);

            for (int i = 0; i < totalRequests; i++) {
                final int idx = i;
                final double qLat = minLat + (rng.nextDouble() * (maxLat - minLat));
                final double qLon = minLon + (rng.nextDouble() * (maxLon - minLon));
                final boolean isNearby = (i % 2 == 0);

                futures.add(executor.submit(() -> {
                    long t0 = System.nanoTime();
                    if (isNearby) {
                        SpatialNearbyResponse res = spatialService.findNearby(qLat, qLon, 25.0, 10);
                        assertNotNull(res);
                    } else {
                        ReverseGeoResponse res = spatialService.reverseGeocode(qLat, qLon);
                        assertNotNull(res);
                    }
                    latenciesNanos[idx] = System.nanoTime() - t0;
                }));
            }

            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        }

        long totalBenchDurationMs = System.currentTimeMillis() - benchStart;

        // Calculate percentiles
        double[] latenciesMs = new double[totalRequests];
        for (int i = 0; i < totalRequests; i++) {
            latenciesMs[i] = latenciesNanos[i] / 1_000_000.0;
        }
        Arrays.sort(latenciesMs);

        double p50 = latenciesMs[(int) (totalRequests * 0.50)];
        double p95 = latenciesMs[(int) (totalRequests * 0.95)];
        double p99 = latenciesMs[(int) (totalRequests * 0.99)];
        double throughputQps = (totalRequests / (double) totalBenchDurationMs) * 1000.0;

        log.info("==================================================================");
        log.info("CONCURRENCY BENCHMARK RESULTS (Java 21 Loom Virtual Threads):");
        log.info("  - Total Executed Queries : {}", totalRequests);
        log.info("  - Total Test Duration    : {} ms", totalBenchDurationMs);
        log.info("  - Throughput             : {} QPS", String.format("%.2f", throughputQps));
        log.info("  - Latency p50 (Median)   : {} ms", String.format("%.3f", p50));
        log.info("  - Latency p95            : {} ms", String.format("%.3f", p95));
        log.info("  - Latency p99            : {} ms", String.format("%.3f", p99));
        log.info("==================================================================");

        // Strict SLA assertions across 50,000 indexed entities
        assertTrue(p99 < 10.0, "p99 latency should be strictly under 10ms, was: " + p99 + "ms");
        assertTrue(p50 < 1.0, "p50 median latency should be strictly under 1ms, was: " + p50 + "ms");
    }
}
