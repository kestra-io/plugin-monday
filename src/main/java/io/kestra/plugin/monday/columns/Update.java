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
@Schema(
    title = "Update a column's title or description on a Monday board",
    description = "Uses Monday's `change_column_metadata` mutation. Pick the attribute to change via `attribute` and provide the new value via `value`."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Rename a column",
            code = """
                id: monday_rename_column
                namespace: company.team

                tasks:
                  - id: rename
                    type: io.kestra.plugin.monday.columns.Update
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    columnId: "status"
                    attribute: TITLE
                    value: "Stage"
                """
        ),
        @Example(
            full = true,
            title = "Update a column description",
            code = """
                id: monday_describe_column
                namespace: company.team

                tasks:
                  - id: describe
                    type: io.kestra.plugin.monday.columns.Update
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    columnId: "status"
                    attribute: DESCRIPTION
                    value: "Current pipeline stage for this lead."
                """
        )
    }
)
public class Update extends AbstractMondayCall<Update.Output> {
    @Schema(title = "Board id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Column id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> columnId;

    @Schema(title = "Column attribute to update", description = "One of `TITLE` or `DESCRIPTION`.")
    @PluginProperty(group = "main")
    @NotNull
    private Property<ColumnAttribute> attribute;

    @Schema(title = "New value for the chosen attribute")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> value;

    private transient String rColumnId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $columnId: String!, $attr: ColumnProperty!, $value: JSON!) {
              change_column_metadata(board_id: $boardId, column_id: $columnId, column_property: $attr, value: $value) { id }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        this.rColumnId = runContext.render(this.columnId).as(String.class).orElseThrow();
        var rAttr = runContext.render(this.attribute).as(ColumnAttribute.class).orElseThrow();
        var rValue = runContext.render(this.value).as(String.class).orElseThrow();
        // Monday expects `value` as a JSON-encoded string (JSON scalar wraps any JSON value).
        var jsonValue = MAPPER.writeValueAsString(rValue);
        return Map.of(
            "boardId", rBoardId,
            "columnId", rColumnId,
            "attr", rAttr.value(),
            "value", jsonValue
        );
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().columnId(this.rColumnId).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Updated column id")
        private final String columnId;
    }
}
