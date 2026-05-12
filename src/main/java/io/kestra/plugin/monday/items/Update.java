package io.kestra.plugin.monday.items;

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
@Schema(
    title = "Update one or more column values on an item",
    description = """
        Calls `change_multiple_column_values`. `columnValues` is serialized to a JSON
        string before being sent, matching Monday's JSON-in-JSON contract."""
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Set a status column",
            code = """
                id: monday_update_item
                namespace: company.team

                tasks:
                  - id: update
                    type: io.kestra.plugin.monday.items.Update
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    itemId: "9876543210"
                    columnValues:
                      status: { label: "Done" }
                """
        )
    }
)
public class Update extends AbstractMondayCall<Update.Output> {
    @Schema(title = "Board id of the item")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Item id to update")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> itemId;

    @Schema(title = "Column values to set, keyed by column id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<Map<String, Object>> columnValues;

    @Schema(title = "Create new label entries on status/dropdown columns when missing")
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<Boolean> createLabelsIfMissing = Property.ofValue(false);

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $itemId: ID!, $columnValues: JSON!, $createLabels: Boolean) {
              change_multiple_column_values(
                board_id: $boardId,
                item_id: $itemId,
                column_values: $columnValues,
                create_labels_if_missing: $createLabels
              ) { id }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rItemId = runContext.render(this.itemId).as(String.class).orElseThrow();
        var rColumnValues = runContext.render(this.columnValues).asMap(String.class, Object.class);
        var rCreateLabels = runContext.render(this.createLabelsIfMissing).as(Boolean.class).orElse(false);

        var variables = new HashMap<String, Object>();
        variables.put("boardId", rBoardId);
        variables.put("itemId", rItemId);
        variables.put("columnValues", MAPPER.writeValueAsString(rColumnValues));
        variables.put("createLabels", rCreateLabels);
        return variables;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var node = data.get("change_multiple_column_values");
        return Output.builder()
            .itemId(node.get("id").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Updated item id")
        private final String itemId;
    }
}
