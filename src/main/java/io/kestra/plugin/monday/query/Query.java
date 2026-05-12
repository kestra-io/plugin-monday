package io.kestra.plugin.monday.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.monday.AbstractMondayCall;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Run a raw Monday GraphQL query",
    description = """
        Escape hatch for operations not covered by the typed tasks. Sends the supplied
        `query` and optional `variables` to the Monday GraphQL endpoint and returns
        the raw `data` node as a map."""
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Fetch the authenticated user",
            code = """
                id: monday_query
                namespace: company.team

                tasks:
                  - id: me
                    type: io.kestra.plugin.monday.query.Query
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    query: |
                      query { me { id name email } }
                """
        )
    }
)
public class Query extends AbstractMondayCall<Query.Output> {
    @Schema(
        title = "GraphQL query string",
        description = "Runs arbitrary GraphQL against Monday, including destructive mutations such as delete_board and delete_item. Restrict use of this task to trusted namespaces and users."
    )
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> query;

    @Schema(title = "GraphQL variables")
    @PluginProperty(group = "main")
    private Property<Map<String, Object>> variables;

    @Override
    protected String buildQuery(RunContext runContext) throws Exception {
        var rendered = runContext.render(this.query).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("query is required"));
        if (rendered.contains("{{") && rendered.contains("}}")) {
            throw new IllegalArgumentException(
                "Rendered GraphQL query still contains unrendered Pebble template markers '{{ }}'. " +
                "This usually means an input variable was missing. Refusing to send to Monday API."
            );
        }
        return rendered;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        return this.variables == null
            ? Map.of()
            : runContext.render(this.variables).asMap(String.class, Object.class);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var asMap = MAPPER.convertValue(data, new TypeReference<Map<String, Object>>() {});
        return Output.builder()
            .data(asMap)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "GraphQL `data` payload returned by Monday")
        private final Map<String, Object> data;
    }
}
