package io.kestra.plugin.monday.workspaces;

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
    title = "Query Monday workspaces with paging and filters",
    description = "Query Monday workspaces with paging and filters. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "List the first 50 workspaces",
            code = """
                id: monday_query_workspaces
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.monday.workspaces.Query
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    limit: 50
                    fetchType: FETCH
                """
        )
    },
    metrics = {
        @Metric(name = "workspaces.fetched", type = Counter.TYPE, description = "Total workspaces fetched.")
    }
)
public class Query extends AbstractMondayFetch<Map<String, Object>, Query.Output> {
    private static final int MAX_PAGES = 1000;

    @Schema(title = "Page size")
    @PluginProperty(group = "processing")
    @Builder.Default
    private Property<Integer> limit = Property.ofValue(50);

    @Schema(title = "Workspace ids filter")
    @PluginProperty(group = "processing")
    private Property<List<String>> ids;

    @Schema(title = "Workspace kind", description = "One of `open`, `closed`.")
    @PluginProperty(group = "processing")
    private Property<WorkspaceKind> kind;

    @Schema(title = "Workspace state", description = "One of `active`, `archived`, `deleted`, `all`.")
    @PluginProperty(group = "processing")
    private Property<WorkspaceState> state;

    @Override
    protected List<Map<String, Object>> fetchAll(RunContext runContext) throws Exception {
        var rLimit = runContext.render(this.limit).as(Integer.class).orElse(50);
        var rIds = runContext.render(this.ids).asList(String.class);
        var rKind = runContext.render(this.kind).as(WorkspaceKind.class).orElse(null);
        var rState = runContext.render(this.state).as(WorkspaceState.class).orElse(null);

        var query = """
            query ($limit: Int!, $page: Int!, $ids: [ID!], $kind: WorkspaceKind, $state: State) {
              workspaces(limit: $limit, page: $page, ids: $ids, kind: $kind, state: $state) {
                id name kind description state
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
                vars.put("ids", rIds.isEmpty() ? null : rIds);
                vars.put("kind", rKind == null ? null : rKind.value());
                vars.put("state", rState == null ? null : rState.value());

                var data = client.execute(query, vars);
                var workspaces = data.get("workspaces");
                if (workspaces == null || workspaces.isEmpty()) {
                    break;
                }
                var pageCount = 0;
                for (var node : workspaces) {
                    collected.add(MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {}));
                    pageCount++;
                }
                if (pageCount < rLimit) {
                    break;
                }
                page++;
                if (page > MAX_PAGES) {
                    runContext.logger().warn("workspaces.Query reached MAX_PAGES ({}) safeguard; results truncated", MAX_PAGES);
                    break;
                }
            }
        }
        runContext.metric(Counter.of("workspaces.fetched", (long) collected.size()));
        return collected;
    }

    @Override
    protected Output buildOutput(Map<String, Object> row, List<Map<String, Object>> items, URI uri, Long size) {
        return Output.builder().row(row).workspaces(items).uri(uri).size(size).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Workspaces (FETCH mode)")
        private final List<Map<String, Object>> workspaces;

        @Schema(title = "First workspace (FETCH_ONE mode)")
        private final Map<String, Object> row;

        @Schema(title = "Internal storage URI (STORE mode)")
        private final URI uri;

        @Schema(title = "Number of workspaces fetched")
        private final Long size;
    }
}
