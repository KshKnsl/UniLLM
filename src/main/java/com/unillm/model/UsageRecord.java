package com.unillm.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Records a single API request's metadata for dashboard analytics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsageRecord(
        String id,
        String provider,
        String model,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        long latencyMs,
        boolean streamed,
        boolean success,
        String errorMessage,
        long timestamp
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String provider;
        private String model;
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
        private long latencyMs;
        private boolean streamed;
        private boolean success = true;
        private String errorMessage;
        private long timestamp = System.currentTimeMillis();

        public Builder id(String id) { this.id = id; return this; }
        public Builder provider(String p) { this.provider = p; return this; }
        public Builder model(String m) { this.model = m; return this; }
        public Builder promptTokens(int t) { this.promptTokens = t; return this; }
        public Builder completionTokens(int t) { this.completionTokens = t; return this; }
        public Builder totalTokens(int t) { this.totalTokens = t; return this; }
        public Builder latencyMs(long l) { this.latencyMs = l; return this; }
        public Builder streamed(boolean s) { this.streamed = s; return this; }
        public Builder success(boolean s) { this.success = s; return this; }
        public Builder errorMessage(String e) { this.errorMessage = e; return this; }
        public Builder timestamp(long t) { this.timestamp = t; return this; }

        public UsageRecord build() {
            return new UsageRecord(id, provider, model, promptTokens,
                    completionTokens, totalTokens, latencyMs, streamed,
                    success, errorMessage, timestamp);
        }
    }
}
