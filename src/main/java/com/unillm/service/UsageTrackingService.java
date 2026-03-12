package com.unillm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unillm.model.UsageRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * File-backed usage tracking service.
 * Stores request records and computes aggregated statistics
 * for the dashboard, persisting to a JSONL file.
 */
@Service
public class UsageTrackingService {

    private static final int MAX_RECORDS = 1000;
    private final Path storageFile = Path.of("data", "usage-records.jsonl");
    private final Object fileLock = new Object();

    private final ConcurrentLinkedDeque<UsageRecord> records = new ConcurrentLinkedDeque<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    private final ObjectMapper objectMapper;

    public UsageTrackingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            if (Files.exists(storageFile)) {
                List<String> lines = Files.readAllLines(storageFile);
                for (String line : lines) {
                    try {
                        if (line.trim().isEmpty()) continue;
                        UsageRecord record = objectMapper.readValue(line, UsageRecord.class);
                        totalRequests.incrementAndGet();
                        if (!record.success()) totalErrors.incrementAndGet();

                        records.addFirst(record);
                        while (records.size() > MAX_RECORDS) {
                            records.removeLast();
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse record: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize usage storage: " + e.getMessage());
        }
    }

    /**
     * Record a completed request and persist it to file.
     */
    public void record(UsageRecord record) {
        records.addFirst(record);
        totalRequests.incrementAndGet();
        if (!record.success()) totalErrors.incrementAndGet();

        // Trim old records in memory
        while (records.size() > MAX_RECORDS) {
            records.removeLast();
        }

        // Persist to file
        try {
            String jsonLine = objectMapper.writeValueAsString(record) + System.lineSeparator();
            synchronized (fileLock) {
                if (!Files.exists(storageFile.getParent())) {
                    Files.createDirectories(storageFile.getParent());
                }
                Files.writeString(storageFile, jsonLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            System.err.println("Failed to persist usage record: " + e.getMessage());
        }
    }

    /**
     * Get the N most recent records.
     */
    public List<UsageRecord> getRecentRecords(int limit) {
        return records.stream().limit(limit).toList();
    }

    /**
     * Compute full dashboard statistics.
     */
    public Map<String, Object> getDashboardStats() {
        List<UsageRecord> all = new ArrayList<>(records);
        Map<String, Object> stats = new LinkedHashMap<>();

        // Overview
        stats.put("totalRequests", totalRequests.get());
        stats.put("totalErrors", totalErrors.get());
        stats.put("successRate", totalRequests.get() == 0 ? 100.0 :
                Math.round((1.0 - (double) totalErrors.get() / totalRequests.get()) * 10000.0) / 100.0);

        // Total tokens
        long totalTokens = all.stream().mapToLong(UsageRecord::totalTokens).sum();
        long promptTokens = all.stream().mapToLong(UsageRecord::promptTokens).sum();
        long completionTokens = all.stream().mapToLong(UsageRecord::completionTokens).sum();
        stats.put("totalTokens", totalTokens);
        stats.put("promptTokens", promptTokens);
        stats.put("completionTokens", completionTokens);

        // Average latency
        double avgLatency = all.stream().mapToLong(UsageRecord::latencyMs)
                .average().orElse(0);
        stats.put("avgLatencyMs", Math.round(avgLatency));

        // Per-provider breakdown
        Map<String, Map<String, Object>> byProvider = all.stream()
                .collect(Collectors.groupingBy(
                        r -> r.provider() != null ? r.provider() : "unknown",
                        Collectors.collectingAndThen(Collectors.toList(), this::aggregateGroup)
                ));
        stats.put("byProvider", byProvider);

        // Per-model breakdown
        Map<String, Map<String, Object>> byModel = all.stream()
                .collect(Collectors.groupingBy(
                        r -> r.model() != null ? r.model() : "unknown",
                        Collectors.collectingAndThen(Collectors.toList(), this::aggregateGroup)
                ));
        stats.put("byModel", byModel);

        // Requests over time (last 24h, bucketed by hour)
        long now = System.currentTimeMillis();
        long oneDayAgo = now - 24 * 60 * 60 * 1000;
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (int i = 23; i >= 0; i--) {
            long bucketStart = now - (long)(i + 1) * 60 * 60 * 1000;
            long bucketEnd = now - (long) i * 60 * 60 * 1000;
            long count = all.stream()
                    .filter(r -> r.timestamp() >= bucketStart && r.timestamp() < bucketEnd)
                    .count();
            Map<String, Object> bucket = new LinkedHashMap<>();
            bucket.put("hour", -i);
            bucket.put("count", count);
            timeline.add(bucket);
        }
        stats.put("timeline", timeline);

        return stats;
    }

    private Map<String, Object> aggregateGroup(List<UsageRecord> group) {
        Map<String, Object> agg = new LinkedHashMap<>();
        agg.put("requests", group.size());
        agg.put("errors", group.stream().filter(r -> !r.success()).count());
        agg.put("totalTokens", group.stream().mapToLong(UsageRecord::totalTokens).sum());
        agg.put("avgLatencyMs", Math.round(
                group.stream().mapToLong(UsageRecord::latencyMs).average().orElse(0)));
        return agg;
    }
}
