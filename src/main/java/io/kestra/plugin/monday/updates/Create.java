package io.kestra.plugin.monday.updates;

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
@Schema(title = "Post an update (comment) on a Monday item")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Post a comment on an item",
            code = """
                id: monday_create_update
                namespace: company.team

                tasks:
                  - id: comment
                    type: io.kestra.plugin.monday.updates.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    itemId: "9876543210"
                    body: "Pipeline run finished successfully."
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Item id to post the update on")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> itemId;

    @Schema(title = "Body of the update. Supports HTML")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> body;

    @Schema(title = "Parent update id to reply to")
    @PluginProperty(group = "advanced")
    private Property<String> parentId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($itemId: ID!, $body: String!, $parentId: ID) {
              create_update(item_id: $itemId, body: $body, parent_id: $parentId) { id }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rItemId = runContext.render(this.itemId).as(String.class).orElseThrow();
        var rBody = runContext.render(this.body).as(String.class).orElseThrow();
        var rParentId = runContext.render(this.parentId).as(String.class).orElse(null);

        var vars = new HashMap<String, Object>();
        vars.put("itemId", rItemId);
        vars.put("body", rBody);
        vars.put("parentId", rParentId);
        return vars;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().updateId(data.get("create_update").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created update id")
        private final String updateId;
    }
}
