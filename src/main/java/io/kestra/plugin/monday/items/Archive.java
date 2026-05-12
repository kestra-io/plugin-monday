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
@Schema(title = "Archive an item on a Monday board")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Archive an item",
            code = """
                id: monday_archive_item
                namespace: company.team

                tasks:
                  - id: archive
                    type: io.kestra.plugin.monday.items.Archive
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    itemId: "9876543210"
                """
        )
    }
)
public class Archive extends AbstractMondayCall<Archive.Output> {
    @Schema(title = "Item id to archive")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> itemId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($itemId: ID!) { archive_item(item_id: $itemId) { id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rItemId = runContext.render(this.itemId).as(String.class).orElseThrow();
        return Map.of("itemId", rItemId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder()
            .itemId(data.get("archive_item").get("id").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Archived item id")
        private final String itemId;
    }
}
