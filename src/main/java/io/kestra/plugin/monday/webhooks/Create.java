package io.kestra.plugin.monday.webhooks;

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
    title = "Create a webhook on a Monday board",
    description = "Create a webhook on a Monday board. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Subscribe to new item events",
            code = """
                id: monday_create_webhook
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.monday.webhooks.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    url: "https://example.com/webhook"
                    event: create_item
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Board id to subscribe on")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Target URL that will receive POST callbacks")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> url;

    @Schema(
        title = "Webhook event",
        description = """
            One of Monday's documented event types, e.g. `create_item`, `change_column_value`,
            `change_status_column_value`, `item_archived`, `item_deleted`, `item_moved_to_any_group`,
            `create_subitem`, `create_update`, `edit_update`, `delete_update`."""
    )
    @PluginProperty(group = "main")
    @NotNull
    private Property<WebhookEvent> event;

    @Schema(title = "Extra event configuration", description = "Serialized to a JSON string for events that require it. For example, `change_status_column_value` uses: `{\"columnId\": \"status\", \"columnValue\": {\"index\": 1}}`.")
    @PluginProperty(group = "advanced")
    private Property<Map<String, Object>> config;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $url: String!, $event: WebhookEventType!, $config: JSON) {
              create_webhook(board_id: $boardId, url: $url, event: $event, config: $config) { id board_id }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rUrl = runContext.render(this.url).as(String.class).orElseThrow();
        var rEvent = runContext.render(this.event).as(WebhookEvent.class).orElseThrow();
        var rConfig = this.config == null ? Map.<String, Object>of() : runContext.render(this.config).asMap(String.class, Object.class);

        var vars = new HashMap<String, Object>();
        vars.put("boardId", rBoardId);
        vars.put("url", rUrl);
        vars.put("event", rEvent.value());
        vars.put("config", rConfig.isEmpty() ? null : MAPPER.writeValueAsString(rConfig));
        return vars;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var node = data.get("create_webhook");
        return Output.builder()
            .webhookId(node.get("id").asText())
            .boardId(node.get("board_id").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created webhook id")
        private final String webhookId;

        @Schema(title = "Board id the webhook is attached to")
        private final String boardId;
    }
}
