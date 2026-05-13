package io.kestra.plugin.monday.folders;

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
@Schema(title = "Create a folder inside a Monday workspace")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Create a folder in a workspace",
            code = """
                id: monday_create_folder
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.monday.folders.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    name: "Campaigns"
                    workspaceId: "1234567890"
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Folder name")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> name;

    @Schema(title = "Folder color", description = "Monday accepts the uppercase enum name, e.g. `DONE_GREEN`, `BRIGHT_BLUE`, `PURPLE`. See `FolderColor` for all available values.")
    @PluginProperty(group = "main")
    private Property<FolderColor> color;

    @Schema(title = "Workspace id that owns the folder")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> workspaceId;

    @Schema(title = "Parent folder id for nested folders")
    @PluginProperty(group = "main")
    private Property<String> parentFolderId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($name: String!, $color: FolderColor, $workspaceId: ID!, $parentFolderId: ID) {
              create_folder(name: $name, color: $color, workspace_id: $workspaceId, parent_folder_id: $parentFolderId) { id }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rName = runContext.render(this.name).as(String.class).orElseThrow();
        var rColor = runContext.render(this.color).as(FolderColor.class).map(FolderColor::value).orElse(null);
        var rWorkspaceId = runContext.render(this.workspaceId).as(String.class).orElseThrow();
        var rParentFolderId = runContext.render(this.parentFolderId).as(String.class).orElse(null);

        var vars = new HashMap<String, Object>();
        vars.put("name", rName);
        vars.put("color", rColor);
        vars.put("workspaceId", rWorkspaceId);
        vars.put("parentFolderId", rParentFolderId);
        return vars;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().folderId(data.get("create_folder").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created folder id")
        private final String folderId;
    }
}
