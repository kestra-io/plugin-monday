package io.kestra.plugin.monday.items;

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
    title = "Fetch a single item by id, including its column values",
    description = "Fetch a single item by id, including its column values. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Fetch an item",
            code = """
                id: monday_get_item
                namespace: company.team

                tasks:
                  - id: get
                    type: io.kestra.plugin.monday.items.Get
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    itemId: "9876543210"
                """
        )
    }
)
public class Get extends AbstractMondayCall<Get.Output> {
    @Schema(title = "Item id to fetch")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> itemId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            query ($ids: [ID!]) {
              items(ids: $ids) {
                id name state
                board { id }
                group { id title }
                column_values { id text value type }
              }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rItemId = runContext.render(this.itemId).as(String.class).orElseThrow();
        return Map.of("ids", List.of(rItemId));
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var items = data.get("items");
        if (items == null || items.isEmpty()) {
            return Output.builder().item(null).build();
        }
        var item = MAPPER.convertValue(items.get(0), new TypeReference<Map<String, Object>>() {});
        return Output.builder().item(item).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Item payload returned by Monday")
        private final Map<String, Object> item;
    }
}
