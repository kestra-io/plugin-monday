package io.kestra.plugin.monday.items;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.triggers.AbstractTrigger;
import io.kestra.core.models.triggers.PollingTriggerInterface;
import io.kestra.core.models.triggers.TriggerContext;
import io.kestra.core.models.triggers.TriggerOutput;
import io.kestra.core.models.triggers.TriggerService;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.monday.MondayClients;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Trigger a flow when items on a Monday board are created or updated",
    description = """
        Polls `items_page` for the board, ordered by `__last_updated__` descending, and
        emits an execution when one or more items have an `updated_at` strictly newer
        than `now - interval` (with a 1-minute overlap buffer to absorb clock drift).
        Fetches up to 100 items per evaluation; if the board is very active, decrease
        the interval or use `items.Query` with STORE mode for full coverage."""
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Run a flow whenever items on a board change",
            code = """
                id: monday_items_trigger
                namespace: company.team

                tasks:
                  - id: log
                    type: io.kestra.plugin.core.log.Log
                    message: "Found {{ trigger.count }} updated items"

                triggers:
                  - id: items
                    type: io.kestra.plugin.monday.items.Trigger
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    interval: PT1M
                """
        )
    }
)
public class Trigger extends AbstractTrigger implements PollingTriggerInterface, TriggerOutput<Trigger.Output> {
    private static final ObjectMapper MAPPER = JacksonMapper.ofJson();
    private static final int ITEMS_PAGE_LIMIT = 100;

    @Schema(
        title = "Monday API token",
        description = """
            Personal API v2 token. Generate it in Monday under Profile, Developers, My Access Tokens.
            Treat the token as a secret and prefer Kestra secrets, e.g. `{{ secret('MONDAY_API_TOKEN') }}`."""
    )
    @PluginProperty(group = "connection", secret = true)
    @NotNull
    private Property<String> apiToken;

    @Schema(
        title = "Monday GraphQL endpoint",
        description = "Override only for proxies or test fixtures (WireMock)."
    )
    @PluginProperty(group = "connection")
    @Builder.Default
    private Property<String> apiUrl = Property.ofValue("https://api.monday.com/v2");

    @Schema(
        title = "Monday API version header",
        description = "Sent as the `API-Version` header. See https://developer.monday.com/api-reference/docs/api-versioning."
    )
    @PluginProperty(group = "connection")
    @Builder.Default
    private Property<String> apiVersion = Property.ofValue("2024-10");

    @Schema(title = "Board id to watch")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Optional group id filter")
    @PluginProperty(group = "processing")
    private Property<String> groupId;

    @Schema(title = "Polling interval")
    @Builder.Default
    private Duration interval = Duration.ofMinutes(5);

    @Schema(
        title = "Maximum retry attempts",
        description = "Number of attempts before giving up on transient Monday API errors (HTTP 429, COMPLEXITY_BUDGET_EXHAUSTED)."
    )
    @PluginProperty(group = "reliability")
    @Builder.Default
    private Property<Integer> maxRetries = Property.ofValue(3);

    @Schema(title = "Initial retry interval", description = "Base wait between retry attempts. Doubles each attempt up to maxRetryInterval.")
    @PluginProperty(group = "reliability")
    @Builder.Default
    private Property<Duration> retryInterval = Property.ofValue(Duration.ofSeconds(5));

    @Schema(title = "Maximum retry interval", description = "Cap on the exponentially-growing retry delay.")
    @PluginProperty(group = "reliability")
    @Builder.Default
    private Property<Duration> retryMaxInterval = Property.ofValue(Duration.ofSeconds(30));

    @Override
    public Optional<Execution> evaluate(ConditionContext conditionContext, TriggerContext context) throws Exception {
        var runContext = conditionContext.getRunContext();

        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rGroupId = runContext.render(this.groupId).as(String.class).orElse(null);

        var since = context.getDate() != null
            ? context.getDate().withZoneSameInstant(ZoneOffset.UTC)
            : ZonedDateTime.now(ZoneOffset.UTC).minus(this.interval).minus(Duration.ofMinutes(1));

        var query = """
            query ($boardId: ID!, $limit: Int!) {
              boards(ids: [$boardId]) {
                items_page(limit: $limit, query_params: { order_by: [{ column_id: "__last_updated__", direction: desc }] }) {
                  items { id name updated_at group { id title } column_values { id text value type } }
                }
              }
            }
            """;

        var matched = new ArrayList<Map<String, Object>>();
        try (var client = MondayClients.build(runContext, this.apiToken, this.apiUrl, this.apiVersion,
            this.maxRetries, this.retryInterval, this.retryMaxInterval)) {
            var data = client.execute(query, Map.of("boardId", rBoardId, "limit", ITEMS_PAGE_LIMIT));
            var boards = data.get("boards");
            if (boards == null || boards.isEmpty()) {
                return Optional.empty();
            }
            JsonNode itemsPage = boards.get(0).get("items_page");
            JsonNode itemsArr = itemsPage == null ? null : itemsPage.get("items");
            if (itemsArr == null || itemsArr.isEmpty()) {
                return Optional.empty();
            }

            if (itemsArr.size() >= ITEMS_PAGE_LIMIT) {
                runContext.logger().warn(
                    "Monday items.Trigger fetched the maximum {} items for board {}. " +
                    "Newer items may have been missed. Decrease the polling interval or use items.Query for full coverage.",
                    ITEMS_PAGE_LIMIT, rBoardId
                );
            }

            for (var node : itemsArr) {
                var updatedAt = node.hasNonNull("updated_at") ? node.get("updated_at").asText() : null;
                if (updatedAt == null || !isAfter(updatedAt, since)) {
                    continue;
                }
                if (rGroupId != null) {
                    var group = node.get("group");
                    var gid = group != null && group.hasNonNull("id") ? group.get("id").asText() : null;
                    if (!rGroupId.equals(gid)) {
                        continue;
                    }
                }
                matched.add(MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {}));
            }
        }

        if (matched.isEmpty()) {
            return Optional.empty();
        }

        var output = Output.builder().items(matched).count(matched.size()).build();
        return Optional.of(TriggerService.generateExecution(this, conditionContext, context, output));
    }

    private boolean isAfter(String iso, ZonedDateTime since) {
        try {
            return Instant.parse(iso).atZone(ZoneOffset.UTC).isAfter(since);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Items changed since the previous evaluation")
        private final List<Map<String, Object>> items;

        @Schema(title = "Number of items returned")
        private final Integer count;
    }
}
