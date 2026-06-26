package io.kestra.plugin.monday.users;

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
    title = "Query Monday users",
    description = "Query Monday users. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Fetch the first 50 active users",
            code = """
                id: monday_query_users
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.monday.users.Query
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    limit: 50
                    fetchType: FETCH
                """
        )
    },
    metrics = {
        @Metric(name = "users.fetched", type = Counter.TYPE, description = "Total users fetched.")
    }
)
public class Query extends AbstractMondayFetch<Map<String, Object>, Query.Output> {
    @Schema(title = "Page size")
    @PluginProperty(group = "processing")
    @Builder.Default
    private Property<Integer> limit = Property.ofValue(50);

    @Schema(title = "User ids filter")
    @PluginProperty(group = "processing")
    private Property<List<String>> ids;

    @Schema(title = "User kind", description = "One of `all`, `non_guests`, `guests`, `non_pending`.")
    @PluginProperty(group = "processing")
    private Property<UserKind> kind;

    @Override
    protected List<Map<String, Object>> fetchAll(RunContext runContext) throws Exception {
        var rLimit = runContext.render(this.limit).as(Integer.class).orElse(50);
        var rIds = runContext.render(this.ids).asList(String.class);
        var rKind = runContext.render(this.kind).as(UserKind.class).orElse(null);

        var query = """
            query ($limit: Int!, $ids: [ID!], $kind: UserKind) {
              users(limit: $limit, ids: $ids, kind: $kind) {
                id name email is_admin is_guest enabled
              }
            }
            """;
        var vars = new HashMap<String, Object>();
        vars.put("limit", rLimit);
        vars.put("ids", rIds.isEmpty() ? null : rIds);
        vars.put("kind", rKind == null ? null : rKind.value());

        var collected = new ArrayList<Map<String, Object>>();
        try (var client = client(runContext)) {
            var data = client.execute(query, vars);
            var users = data.get("users");

            if (users != null) {
                for (var node : users) {
                    collected.add(MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {}));
                }
            }
        }
        runContext.metric(Counter.of("users.fetched", (long) collected.size()));
        return collected;
    }

    @Override
    protected Output buildOutput(Map<String, Object> row, List<Map<String, Object>> items, URI uri, Long size) {
        return Output.builder().row(row).users(items).uri(uri).size(size).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Users (FETCH mode)")
        private final List<Map<String, Object>> users;

        @Schema(title = "First user (FETCH_ONE mode)")
        private final Map<String, Object> row;

        @Schema(title = "Internal storage URI (STORE mode)")
        private final URI uri;

        @Schema(title = "Number of users fetched")
        private final Long size;
    }
}
