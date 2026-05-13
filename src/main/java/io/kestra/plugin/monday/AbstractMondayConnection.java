package io.kestra.plugin.monday;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Duration;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
public abstract class AbstractMondayConnection extends Task {
    protected static final ObjectMapper MAPPER = JacksonMapper.ofJson();

    @Schema(
        title = "Monday API token",
        description = """
            Personal API v2 token. Generate it in Monday under Profile, Developers, My Access Tokens.
            Treat the token as a secret and prefer Kestra secrets, e.g. `{{ secret('MONDAY_API_TOKEN') }}`."""
    )
    @PluginProperty(group = "connection", secret = true)
    @NotNull
    protected Property<String> apiToken;

    @Schema(
        title = "Monday GraphQL endpoint",
        description = "Override only for proxies or test fixtures (WireMock)."
    )
    @PluginProperty(group = "connection")
    @Builder.Default
    protected Property<String> apiUrl = Property.ofValue("https://api.monday.com/v2");

    @Schema(
        title = "Monday API version header",
        description = "Sent as the `API-Version` header. See https://developer.monday.com/api-reference/docs/api-versioning."
    )
    @PluginProperty(group = "connection")
    @Builder.Default
    protected Property<String> apiVersion = Property.ofValue("2024-10");

    @Schema(
        title = "Maximum retry attempts",
        description = "Number of attempts before giving up on transient Monday API errors (HTTP 429, COMPLEXITY_BUDGET_EXHAUSTED)."
    )
    @PluginProperty(group = "reliability")
    @Builder.Default
    protected Property<Integer> maxRetries = Property.ofValue(3);

    @Schema(
        title = "Initial retry interval",
        description = "Base wait between retry attempts. Doubles each attempt up to maxRetryInterval."
    )
    @PluginProperty(group = "reliability")
    @Builder.Default
    protected Property<Duration> retryInterval = Property.ofValue(Duration.ofSeconds(5));

    @Schema(
        title = "Maximum retry interval",
        description = "Cap on the exponentially-growing retry delay."
    )
    @PluginProperty(group = "reliability")
    @Builder.Default
    protected Property<Duration> retryMaxInterval = Property.ofValue(Duration.ofSeconds(30));

    public MondayClient client(RunContext runContext) throws Exception {
        return MondayClients.build(runContext, this.apiToken, this.apiUrl, this.apiVersion,
            this.maxRetries, this.retryInterval, this.retryMaxInterval);
    }
}
