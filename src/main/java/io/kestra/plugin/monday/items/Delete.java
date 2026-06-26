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
@Schema(
    title = "Delete an item from a Monday board",
    description = "Delete an item from a Monday board. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Delete an item",
            code = """
                id: monday_delete_item
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.monday.items.Delete
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    itemId: "9876543210"
                """
        )
    }
)
public class Delete extends AbstractMondayCall<Delete.Output> {
    @Schema(title = "Item id to delete")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> itemId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($itemId: ID!) { delete_item(item_id: $itemId) { id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rItemId = runContext.render(this.itemId).as(String.class).orElseThrow();
        return Map.of("itemId", rItemId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder()
            .itemId(data.get("delete_item").get("id").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted item id")
        private final String itemId;
    }
}
