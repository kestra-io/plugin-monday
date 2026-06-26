package io.kestra.plugin.monday.boards;

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
    title = "Query Monday boards with paging and filters",
    description = "Query Monday boards with paging and filters. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "List boards in a workspace",
            code = """
                id: monday_query_boards
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.monday.boards.Query
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    workspaceIds:
                      - "1234"
                    fetchType: FETCH
                """
        )
    },
    metrics = {
        @Metric(name = "boards.fetched", type = Counter.TYPE, description = "Total boards fetched.")
    }
)
public class Query extends AbstractMondayFetch<Map<String, Object>, Query.Output> {
    private static final int MAX_PAGES = 1000;

    @Schema(title = "Page size (monday caps at ~100)")
    @PluginProperty(group = "processing")
    @Builder.Default
    private Property<Integer> limit = Property.ofValue(50);

    @Schema(title = "Workspace ids to filter on")
    @PluginProperty(group = "processing")
    private Property<List<String>> workspaceIds;

    @Schema(title = "Board kind", description = "One of `public`, `private`, `share`.")
    @PluginProperty(group = "processing")
    private Property<BoardKind> boardKind;

    @Schema(title = "Board state", description = "One of `active`, `archived`, `deleted`, `all`.")
    @PluginProperty(group = "processing")
    private Property<BoardState> state;

    @Override
    protected List<Map<String, Object>> fetchAll(RunContext runContext) throws Exception {
        var rLimit = runContext.render(this.limit).as(Integer.class).orElse(50);
        var rWorkspaceIds = runContext.render(this.workspaceIds).asList(String.class);
        var rBoardKind = runContext.render(this.boardKind).as(BoardKind.class).orElse(null);
        var rState = runContext.render(this.state).as(BoardState.class).orElse(null);

        var query = """
            query ($limit: Int!, $page: Int!, $workspaceIds: [ID!], $boardKind: BoardKind, $state: State) {
              boards(limit: $limit, page: $page, workspace_ids: $workspaceIds, board_kind: $boardKind, state: $state) {
                id name description state workspace_id
                columns { id title type }
                groups { id title }
              }
            }
            """;

        var collected = new ArrayList<Map<String, Object>>();
        try (var client = client(runContext)) {
            var page = 1;
            while (true) {
                var vars = new HashMap<String, Object>();
                vars.put("limit", rLimit);
                vars.put("page", page);
                vars.put("workspaceIds", rWorkspaceIds.isEmpty() ? null : rWorkspaceIds);
                vars.put("boardKind", rBoardKind == null ? null : rBoardKind.value());
                vars.put("state", rState == null ? null : rState.value());

                var data = client.execute(query, vars);
                var boards = data.get("boards");
                if (boards == null || boards.isEmpty()) {
                    break;
                }
                var pageCount = 0;
                for (var node : boards) {
                    collected.add(MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {}));
                    pageCount++;
                }
                if (pageCount < rLimit) {
                    break;
                }
                page++;
                if (page > MAX_PAGES) {
                    runContext.logger().warn("boards.Query reached MAX_PAGES ({}) safeguard; results truncated", MAX_PAGES);
                    break;
                }
            }
        }

        runContext.metric(Counter.of("boards.fetched", (long) collected.size()));
        return collected;
    }

    @Override
    protected Output buildOutput(Map<String, Object> row, List<Map<String, Object>> items, URI uri, Long size) {
        return Output.builder().row(row).boards(items).uri(uri).size(size).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Boards (FETCH mode)")
        private final List<Map<String, Object>> boards;

        @Schema(title = "First board (FETCH_ONE mode)")
        private final Map<String, Object> row;

        @Schema(title = "Internal storage URI (STORE mode)")
        private final URI uri;

        @Schema(title = "Number of boards fetched")
        private final Long size;
    }
}
