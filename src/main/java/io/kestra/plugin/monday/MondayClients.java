package io.kestra.plugin.monday;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;

import java.time.Duration;

public final class MondayClients {
    private MondayClients() {}

    public static MondayClient build(
        RunContext runContext,
        Property<String> apiToken,
        Property<String> apiUrl,
        Property<String> apiVersion,
        Property<Integer> maxRetries,
        Property<Duration> retryInterval,
        Property<Duration> retryMaxInterval
    ) throws Exception {
        var rApiToken = runContext.render(apiToken).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("apiToken is required"));
        var rApiUrl = runContext.render(apiUrl).as(String.class).orElse("https://api.monday.com/v2");
        var rApiVersion = runContext.render(apiVersion).as(String.class).orElse("2024-10");
        var rMaxRetries = runContext.render(maxRetries).as(Integer.class).orElse(3);
        var rRetryInterval = runContext.render(retryInterval).as(Duration.class).orElse(Duration.ofSeconds(5));
        var rRetryMaxInterval = runContext.render(retryMaxInterval).as(Duration.class).orElse(Duration.ofSeconds(30));
        return new MondayClient(runContext, rApiUrl, rApiToken, rApiVersion, rMaxRetries, rRetryInterval, rRetryMaxInterval);
    }
}
