package io.kestra.plugin.monday.subitems;

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
    title = "Create a subitem under a parent Monday item",
    description = """
        Calls the `create_subitem` mutation. Subitems live on a connected subitems board
        attached to the parent item's board. `columnValues` accepts a structured map and
        is encoded as a JSON string before being sent, matching Monday's JSON-in-JSON
        column values contract."""
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Create a subitem under an existing item",
            code = """
                id: monday_create_subitem
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.monday.subitems.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    parentItemId: "9876543210"
                    itemName: "Implementation"
                    columnValues:
                      status: { label: "Working on it" }
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Parent item id the subitem is attached to")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> parentItemId;

    @Schema(title = "Subitem name")
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
            mutation ($parentItemId: ID!, $itemName: String!, $columnValues: JSON, $createLabelsIfMissing: Boolean) {
              create_subitem(
                parent_item_id: $parentItemId,
                item_name: $itemName,
                column_values: $columnValues,
                create_labels_if_missing: $createLabelsIfMissing
              ) { id name }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rParentItemId = runContext.render(this.parentItemId).as(String.class).orElseThrow();
        var rItemName = runContext.render(this.itemName).as(String.class).orElseThrow();
        var rColumnValues = this.columnValues == null
            ? Map.<String, Object>of()
            : runContext.render(this.columnValues).asMap(String.class, Object.class);
        var rCreateLabels = runContext.render(this.createLabelsIfMissing).as(Boolean.class).orElse(false);

        var variables = new HashMap<String, Object>();
        variables.put("parentItemId", rParentItemId);
        variables.put("itemName", rItemName);
        variables.put("columnValues", rColumnValues.isEmpty() ? null : MAPPER.writeValueAsString(rColumnValues));
        variables.put("createLabelsIfMissing", rCreateLabels);
        return variables;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var node = data.get("create_subitem");
        return Output.builder()
            .itemId(node.get("id").asText())
            .itemName(node.get("name").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created subitem id")
        private final String itemId;

        @Schema(title = "Created subitem name")
        private final String itemName;
    }
}
