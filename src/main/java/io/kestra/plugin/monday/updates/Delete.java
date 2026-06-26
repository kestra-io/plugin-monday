package io.kestra.plugin.monday.updates;

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
    title = "Delete a comment from a Monday item",
    description = "Delete a comment from a Monday item. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Delete an update",
            code = """
                id: monday_delete_update
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.monday.updates.Delete
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    updateId: "1234567890"
                """
        )
    }
)
public class Delete extends AbstractMondayCall<Delete.Output> {
    @Schema(title = "Update id to delete")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> updateId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($id: ID!) { delete_update(id: $id) { id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rUpdateId = runContext.render(this.updateId).as(String.class).orElseThrow();
        return Map.of("id", rUpdateId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().updateId(data.get("delete_update").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted update id")
        private final String updateId;
    }
}
