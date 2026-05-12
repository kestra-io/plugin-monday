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

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(title = "Create a Monday workspace")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Create an open workspace",
            code = """
                id: monday_create_workspace
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.monday.workspaces.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    name: "Marketing"
                    kind: open
                    workspaceDescription: "Campaign tracking."
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Workspace name")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> name;

    @Schema(title = "Workspace kind", description = "One of `open`, `closed`.")
    @PluginProperty(group = "main")
    @NotNull
    private Property<WorkspaceKind> kind;

    @Schema(title = "Workspace description")
    @PluginProperty(group = "main")
    private Property<String> workspaceDescription;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($name: String!, $kind: WorkspaceKind!, $description: String) {
              create_workspace(name: $name, kind: $kind, description: $description) { id }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rName = runContext.render(this.name).as(String.class).orElseThrow();
        var rKind = runContext.render(this.kind).as(WorkspaceKind.class).orElseThrow();
        var rDescription = runContext.render(this.workspaceDescription).as(String.class).orElse(null);

        var vars = new HashMap<String, Object>();
        vars.put("name", rName);
        vars.put("kind", rKind.value());
        vars.put("description", rDescription);
        return vars;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().workspaceId(data.get("create_workspace").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created workspace id")
        private final String workspaceId;
    }
}
