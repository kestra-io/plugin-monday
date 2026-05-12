package io.kestra.plugin.monday.workspaces;

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
@Schema(title = "Delete a Monday workspace")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Delete a workspace",
            code = """
                id: monday_delete_workspace
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.monday.workspaces.Delete
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    workspaceId: "1234567890"
                """
        )
    }
)
public class Delete extends AbstractMondayCall<Delete.Output> {
    @Schema(title = "Workspace id to delete")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> workspaceId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($workspaceId: ID!) { delete_workspace(workspace_id: $workspaceId) { id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rWorkspaceId = runContext.render(this.workspaceId).as(String.class).orElseThrow();
        return Map.of("workspaceId", rWorkspaceId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().workspaceId(data.get("delete_workspace").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted workspace id")
        private final String workspaceId;
    }
}
