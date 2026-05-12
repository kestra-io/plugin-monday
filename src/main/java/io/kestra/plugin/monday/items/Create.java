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
    title = "Create an item on a Monday board",
    description = """
        Calls the `create_item` mutation. `columnValues` accepts a structured map and is
        encoded as a JSON string before being sent, matching Monday's JSON-in-JSON
        column values contract."""
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Create a simple item with a status and date column",
            code = """
                id: monday_create_item
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.monday.items.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    itemName: "New lead"
                    columnValues:
                      status: { label: "Working on it" }
                      date4: { date: "2025-01-01" }
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Target board id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Group id where the item is created", description = "Optional. Defaults to the board's top group.")
    @PluginProperty(group = "main")
    private Property<String> groupId;

    @Schema(title = "Item name")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> itemName;

    @Schema(title = "Column values", description = "Map of column id to column value object. Serialized to JSON.")
    @PluginProperty(group = "main")
    private Property<Map<String, Object>> columnValues;

    @Schema(title = "Create new label entries on status/dropdown columns when missing")
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<Boolean> createLabelsIfMissing = Property.ofValue(false);

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $itemName: String!, $groupId: String, $columnValues: JSON, $createLabels: Boolean) {
              create_item(
                board_id: $boardId,
                item_name: $itemName,
                group_id: $groupId,
                column_values: $columnValues,
                create_labels_if_missing: $createLabels
              ) { id name }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rItemName = runContext.render(this.itemName).as(String.class).orElseThrow();
        var rGroupId = runContext.render(this.groupId).as(String.class).orElse(null);
        var rColumnValues = this.columnValues == null
            ? Map.<String, Object>of()
            : runContext.render(this.columnValues).asMap(String.class, Object.class);
        var rCreateLabels = runContext.render(this.createLabelsIfMissing).as(Boolean.class).orElse(false);

        var variables = new HashMap<String, Object>();
        variables.put("boardId", rBoardId);
        variables.put("itemName", rItemName);
        variables.put("groupId", rGroupId);
        variables.put("columnValues", rColumnValues.isEmpty() ? null : MAPPER.writeValueAsString(rColumnValues));
        variables.put("createLabels", rCreateLabels);
        return variables;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var node = data.get("create_item");
        return Output.builder()
            .itemId(node.get("id").asText())
            .itemName(node.get("name").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created item id")
        private final String itemId;

        @Schema(title = "Created item name")
        private final String itemName;
    }
}
