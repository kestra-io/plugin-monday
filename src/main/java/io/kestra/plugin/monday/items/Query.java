package io.kestra.plugin.monday.items;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Metric;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.monday.AbstractMondayFetch;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Query items on a Monday board",
    description = """
        Uses `items_page` with cursor pagination. Supports three fetch modes:
        FETCH_ONE returns a single row, FETCH returns the full in-memory list, STORE
        writes each row as a JSON line to internal storage and returns a URI."""
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Fetch all items on a board",
            code = """
                id: monday_query_items
                namespace: company.team

                tasks:
                  - id: query
                    type: io.kestra.plugin.monday.items.Query
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    fetchType: FETCH
                """
        )
    },
    metrics = {
        @Metric(name = "items.fetched", type = Counter.TYPE, description = "Total items fetched.")
    }
)
public class Query extends AbstractMondayFetch<Map<String, Object>, Query.Output> {
    private static final int MAX_PAGES = 1000;

    @Schema(title = "Board id to query")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(
        title = "Optional `query_params` map",
        description = "Forwarded as-is to monday's `items_page(query_params: ...)` argument (rules, order_by, operator)."
    )
    @PluginProperty(group = "processing")
    private Property<Map<String, Object>> queryParams;

    @Schema(title = "Max items per page (monday caps at 500)")
    @PluginProperty(group = "processing")
    @Builder.Default
    private Property<Integer> limit = Property.ofValue(500);

    @Override
    protected List<Map<String, Object>> fetchAll(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rLimit = runContext.render(this.limit).as(Integer.class).orElse(500);
        var rQueryParams = this.queryParams == null
            ? null
            : runContext.render(this.queryParams).asMap(String.class, Object.class);

        var firstPageQuery = """
            query ($boardId: ID!, $limit: Int!, $queryParams: ItemsQuery) {
              boards(ids: [$boardId]) {
                items_page(limit: $limit, query_params: $queryParams) {
                  cursor
                  items { id name state created_at updated_at
                    group { id title }
                    column_values { id text value type }
                  }
                }
              }
            }
            """;
        var nextPageQuery = """
            query ($cursor: String!, $limit: Int!) {
              next_items_page(cursor: $cursor, limit: $limit) {
                cursor
                items { id name state created_at updated_at
                  group { id title }
                  column_values { id text value type }
                }
              }
            }
            """;

        var collected = new ArrayList<Map<String, Object>>();
        try (var client = client(runContext)) {
            String cursor = null;
            var firstPage = true;
            var pagesFetched = 0;

            while (true) {
                if (pagesFetched >= MAX_PAGES) {
                    runContext.logger().warn("items.Query reached MAX_PAGES ({}) safeguard; results truncated", MAX_PAGES);
                    break;
                }
                Map<String, Object> vars;
                String query;
                if (firstPage) {
                    vars = new HashMap<>();
                    vars.put("boardId", rBoardId);
                    vars.put("limit", rLimit);
                    if (rQueryParams != null && !rQueryParams.isEmpty()) {
                        vars.put("queryParams", rQueryParams);
                    } else {
                        vars.put("queryParams", null);
                    }
                    query = firstPageQuery;
                } else {
                    vars = Map.of("cursor", cursor, "limit", rLimit);
                    query = nextPageQuery;
                }

                var data = client.execute(query, vars);
                pagesFetched++;
                var pageNode = firstPage
                    ? (data.get("boards") != null && !data.get("boards").isEmpty()
                        ? data.get("boards").get(0).get("items_page")
                        : null)
                    : data.get("next_items_page");

                if (pageNode == null) {
                    break;
                }

                var itemsArr = pageNode.get("items");
                if (itemsArr != null) {
                    for (var node : itemsArr) {
                        collected.add(MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {}));
                    }
                }

                cursor = pageNode.hasNonNull("cursor") ? pageNode.get("cursor").asText() : null;
                if (cursor == null || cursor.isEmpty()) {
                    break;
                }
                firstPage = false;
            }
        }

        runContext.metric(Counter.of("items.fetched", (long) collected.size()));
        return collected;
    }

    @Override
    protected Output buildOutput(Map<String, Object> row, List<Map<String, Object>> items, URI uri, Long size) {
        return Output.builder().row(row).items(items).uri(uri).size(size).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Items (FETCH mode)")
        private final List<Map<String, Object>> items;

        @Schema(title = "First item (FETCH_ONE mode)")
        private final Map<String, Object> row;

        @Schema(title = "Internal storage URI to a JSON-lines file (STORE mode)")
        private final URI uri;

        @Schema(title = "Number of items fetched")
        private final Long size;
    }
}
