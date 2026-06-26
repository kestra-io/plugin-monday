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

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Update a single attribute on a Monday board",
    description = "Update a single attribute on a Monday board. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Rename a board",
            code = """
                id: monday_update_board
                namespace: company.team

                tasks:
                  - id: rename
                    type: io.kestra.plugin.monday.boards.Update
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    boardAttribute: name
                    newValue: "Q2 leads"
                """
        )
    }
)
public class Update extends AbstractMondayCall<Update.Output> {
    @Schema(title = "Board id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Board attribute", description = "One of `name`, `description`, `communication`.")
    @PluginProperty(group = "main")
    @NotNull
    private Property<BoardAttribute> boardAttribute;

    @Schema(title = "New value for the chosen attribute")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> newValue;

    private transient String rBoardId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $attr: BoardAttributes!, $value: String!) {
              update_board(board_id: $boardId, board_attribute: $attr, new_value: $value)
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        this.rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rAttr = runContext.render(this.boardAttribute).as(BoardAttribute.class).orElseThrow();
        var rValue = runContext.render(this.newValue).as(String.class).orElseThrow();
        return Map.of("boardId", rBoardId, "attr", rAttr.value(), "value", rValue);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().boardId(this.rBoardId).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Updated board id")
        private final String boardId;
    }
}
