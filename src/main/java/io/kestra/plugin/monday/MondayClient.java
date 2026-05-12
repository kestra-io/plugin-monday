package io.kestra.plugin.monday;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.retrys.Exponential;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.core.utils.RetryUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MondayClient implements AutoCloseable {
    private static final ObjectMapper MAPPER = JacksonMapper.ofJson();

    private final RunContext runContext;
    private final Logger logger;
    private final String apiUrl;
    private final String apiToken;
    private final String apiVersion;
    private final int maxRetries;
    private final Duration retryInterval;
    private final Duration retryMaxInterval;
    private HttpClient httpClient;

    public MondayClient(RunContext runContext, String apiUrl, String apiToken, String apiVersion,
                        int maxRetries, Duration retryInterval, Duration retryMaxInterval) {
        this.runContext = runContext;
        this.logger = runContext.logger();
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
        this.apiVersion = apiVersion;
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
        this.retryMaxInterval = retryMaxInterval;
    }

    public JsonNode execute(String query) throws Exception {
        return execute(query, null);
    }

    public JsonNode execute(String query, Map<String, Object> variables) throws Exception {
        var payload = MAPPER.createObjectNode();
        payload.put("query", query);
        if (variables != null && !variables.isEmpty()) {
            var filtered = new HashMap<String, Object>();
            for (var entry : variables.entrySet()) {
                if (entry.getValue() != null) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            if (!filtered.isEmpty()) {
                payload.set("variables", MAPPER.valueToTree(filtered));
            }
        }
        var body = MAPPER.writeValueAsString(payload);

        var headers = new HashMap<String, List<String>>();
        headers.put("Authorization", List.of(apiToken));
        headers.put("Content-Type", List.of("application/json"));
        if (apiVersion != null && !apiVersion.isBlank()) {
            headers.put("API-Version", List.of(apiVersion));
        }

        try {
            return RetryUtils.<JsonNode, Exception>of(
                Exponential.builder()
                    .delayFactor(2.0)
                    .interval(retryInterval)
                    .maxInterval(retryMaxInterval)
                    .maxAttempts(maxRetries)
                    .build(),
                logger
            ).runRetryIf(
                t -> t instanceof RetryableMondayException,
                () -> doRequest(body, headers)
            );
        } catch (RetryableMondayException e) {
            throw new IllegalStateException(formatMondayError("RATE_LIMITED", "Monday request failed after retries", List.of()));
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            if (t instanceof RetryUtils.RetryFailed) {
                throw new IllegalStateException(formatMondayError("RATE_LIMITED", "Monday request failed after retries", List.of()));
            }
            throw new Exception(t);
        }
    }

    private JsonNode doRequest(String body, Map<String, List<String>> headers) throws Exception {
        var request = HttpRequest.builder()
            .uri(URI.create(apiUrl))
            .method("POST")
            .body(HttpRequest.StringRequestBody.builder()
                .content(body)
                .contentType("application/json")
                .build())
            .headers(HttpHeaders.of(headers, (a, b) -> true))
            .build();

        HttpResponse<String> response = httpClient().request(request, String.class);
        var status = response.getStatus().getCode();
        var raw = response.getBody();

        if (status == 429) {
            logger.warn("Monday rate limit hit (HTTP 429), will retry");
            throw new RetryableMondayException();
        }

        var tree = raw == null || raw.isEmpty() ? MAPPER.createObjectNode() : MAPPER.readTree(raw);
        var errorCode = tree.hasNonNull("error_code") ? tree.get("error_code").asText() : null;

        if ("COMPLEXITY_BUDGET_EXHAUSTED".equals(errorCode)) {
            logger.warn("Monday complexity budget exhausted, will retry");
            throw new RetryableMondayException();
        }

        if (status < 200 || status >= 300 || errorCode != null || hasErrors(tree)) {
            throw buildException(tree);
        }

        return tree.has("data") ? tree.get("data") : MAPPER.createObjectNode();
    }

    private HttpClient httpClient() throws Exception {
        if (httpClient == null) {
            var config = HttpConfiguration.builder()
                .allowFailed(Property.ofValue(true))
                .build();
            httpClient = HttpClient.builder().runContext(runContext).configuration(config).build();
        }
        return httpClient;
    }

    @Override
    public void close() throws Exception {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

    private boolean hasErrors(JsonNode tree) {
        return tree.has("errors") && tree.get("errors").isArray() && !tree.get("errors").isEmpty();
    }

    private IllegalStateException buildException(JsonNode tree) {
        var errorCode = tree.hasNonNull("error_code") ? tree.get("error_code").asText() : null;
        var errorMessage = tree.hasNonNull("error_message") ? truncate(tree.get("error_message").asText(), 500) : null;
        var messages = new ArrayList<String>();
        if (tree.has("errors") && tree.get("errors").isArray()) {
            tree.get("errors").forEach(e -> {
                if (e.isObject() && e.hasNonNull("message")) {
                    messages.add(truncate(e.get("message").asText(), 500));
                } else if (e.isTextual()) {
                    messages.add(truncate(e.asText(), 500));
                }
            });
        }
        return new IllegalStateException(formatMondayError(errorCode, errorMessage, messages));
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String formatMondayError(String code, String message, List<String> errors) {
        var sb = new StringBuilder("Monday API error");
        if (code != null) sb.append(" [").append(code).append("]");
        if (message != null) sb.append(": ").append(message);
        if (errors != null && !errors.isEmpty()) sb.append(" (").append(String.join("; ", errors)).append(")");
        return sb.toString();
    }

    static final class RetryableMondayException extends RuntimeException {
        RetryableMondayException() {
            super(null, null, true, false);
        }
    }
}
