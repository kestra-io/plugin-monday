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
@Schema(title = "Move an item to a different group on the same board")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Move an item to a group",
            code = """
                id: monday_move_item
                namespace: company.team

                tasks:
                  - id: move
                    type: io.kestra.plugin.monday.items.Move
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    itemId: "9876543210"
                    groupId: "done"
                """
        )
    }
)
public class Move extends AbstractMondayCall<Move.Output> {
    @Schema(title = "Item id to move")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> itemId;

    @Schema(title = "Target group id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> groupId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($itemId: ID!, $groupId: String!) { move_item_to_group(item_id: $itemId, group_id: $groupId) { id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rItemId = runContext.render(this.itemId).as(String.class).orElseThrow();
        var rGroupId = runContext.render(this.groupId).as(String.class).orElseThrow();
        return Map.of("itemId", rItemId, "groupId", rGroupId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder()
            .itemId(data.get("move_item_to_group").get("id").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Moved item id")
        private final String itemId;
    }
}
