package com.unillm.controller;

import com.unillm.model.UsageRecord;
import com.unillm.service.UsageTrackingService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the usage dashboard.
 */
@RestController
@RequestMapping("/api/usage")
@CrossOrigin(origins = "*")
public class UsageController {

    private final UsageTrackingService trackingService;

    public UsageController(UsageTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    /**
     * Get aggregated dashboard statistics.
     */
    @GetMapping("/stats")
    public Mono<Map<String, Object>> getStats() {
        return Mono.just(trackingService.getDashboardStats());
    }

    /**
     * Get recent request records.
     */
    @GetMapping("/recent")
    public Mono<List<UsageRecord>> getRecentRecords(
            @RequestParam(defaultValue = "50") int limit) {
        return Mono.just(trackingService.getRecentRecords(limit));
    }
}
