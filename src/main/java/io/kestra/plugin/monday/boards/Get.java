package io.kestra.plugin.monday.boards;

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

import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Fetch a single board with its columns and groups",
    description = "Fetch a single board with its columns and groups. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Fetch a board",
            code = """
                id: monday_get_board
                namespace: company.team

                tasks:
                  - id: get
                    type: io.kestra.plugin.monday.boards.Get
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                """
        )
    }
)
public class Get extends AbstractMondayCall<Get.Output> {
    @Schema(title = "Board id to fetch")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            query ($ids: [ID!]) {
              boards(ids: $ids) {
                id name description state workspace_id
                columns { id title type }
                groups { id title }
              }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        return Map.of("ids", List.of(rBoardId));
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var boards = data.get("boards");
        if (boards == null || boards.isEmpty()) {
            return Output.builder().board(null).build();
        }
        var board = MAPPER.convertValue(boards.get(0), new TypeReference<Map<String, Object>>() {});
        return Output.builder().board(board).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Board payload returned by Monday")
        private final Map<String, Object> board;
    }
}
