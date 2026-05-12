package io.kestra.plugin.monday.boards;

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
@Schema(title = "Create a new Monday board")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Create a public board",
            code = """
                id: monday_create_board
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.monday.boards.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardName: "Q1 leads"
                    boardKind: public
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Board name")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardName;

    @Schema(title = "Board kind", description = "One of `public`, `private`, `share`.")
    @PluginProperty(group = "main")
    @Builder.Default
    private Property<BoardKind> boardKind = Property.ofValue(BoardKind.PUBLIC);

    @Schema(title = "Workspace id to create the board in")
    @PluginProperty(group = "main")
    private Property<String> workspaceId;

    @Schema(title = "Template id to clone from")
    @PluginProperty(group = "advanced")
    private Property<String> templateId;

    @Schema(title = "Board description")
    @PluginProperty(group = "main")
    private Property<String> boardDescription;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardName: String!, $boardKind: BoardKind!, $workspaceId: ID, $templateId: ID, $description: String) {
              create_board(
                board_name: $boardName,
                board_kind: $boardKind,
                workspace_id: $workspaceId,
                template_id: $templateId,
                description: $description
              ) { id }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardName = runContext.render(this.boardName).as(String.class).orElseThrow();
        var rBoardKind = runContext.render(this.boardKind).as(BoardKind.class).orElse(BoardKind.PUBLIC);
        var rWorkspaceId = runContext.render(this.workspaceId).as(String.class).orElse(null);
        var rTemplateId = runContext.render(this.templateId).as(String.class).orElse(null);
        var rDescription = runContext.render(this.boardDescription).as(String.class).orElse(null);

        var vars = new HashMap<String, Object>();
        vars.put("boardName", rBoardName);
        vars.put("boardKind", rBoardKind.value());
        vars.put("workspaceId", rWorkspaceId);
        vars.put("templateId", rTemplateId);
        vars.put("description", rDescription);
        return vars;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder()
            .boardId(data.get("create_board").get("id").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created board id")
        private final String boardId;
    }
}
