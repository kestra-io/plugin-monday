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

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(title = "Duplicate an item on a Monday board")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Duplicate an item including its updates",
            code = """
                id: monday_duplicate_item
                namespace: company.team

                tasks:
                  - id: duplicate
                    type: io.kestra.plugin.monday.items.Duplicate
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    itemId: "9876543210"
                    withUpdates: true
                """
        )
    }
)
public class Duplicate extends AbstractMondayCall<Duplicate.Output> {
    @Schema(title = "Board id of the source item")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Source item id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> itemId;

    @Schema(title = "Whether to copy the item's updates")
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<Boolean> withUpdates = Property.ofValue(false);

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $itemId: ID!, $withUpdates: Boolean) {
              duplicate_item(board_id: $boardId, item_id: $itemId, with_updates: $withUpdates) { id name }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rItemId = runContext.render(this.itemId).as(String.class).orElseThrow();
        var rWithUpdates = runContext.render(this.withUpdates).as(Boolean.class).orElse(false);
        return Map.of("boardId", rBoardId, "itemId", rItemId, "withUpdates", rWithUpdates);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var node = data.get("duplicate_item");
        return Output.builder()
            .itemId(node.get("id").asText())
            .itemName(node.get("name").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Duplicated item id")
        private final String itemId;

        @Schema(title = "Duplicated item name")
        private final String itemName;
    }
}
