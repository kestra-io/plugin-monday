package io.kestra.plugin.monday.columns;

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
@Schema(title = "Delete a column from a Monday board")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Delete a column",
            code = """
                id: monday_delete_column
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.monday.columns.Delete
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    columnId: "status"
                """
        )
    }
)
public class Delete extends AbstractMondayCall<Delete.Output> {
    @Schema(title = "Board id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Column id to delete")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> columnId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($boardId: ID!, $columnId: String!) { delete_column(board_id: $boardId, column_id: $columnId) { id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rColumnId = runContext.render(this.columnId).as(String.class).orElseThrow();
        return Map.of("boardId", rBoardId, "columnId", rColumnId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().columnId(data.get("delete_column").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted column id")
        private final String columnId;
    }
}
