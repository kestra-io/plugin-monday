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

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(title = "Delete a Monday webhook")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Delete a webhook",
            code = """
                id: monday_delete_webhook
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.monday.webhooks.Delete
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    webhookId: "1234567890"
                """
        )
    }
)
public class Delete extends AbstractMondayCall<Delete.Output> {
    @Schema(title = "Webhook id to delete")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> webhookId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($id: ID!) { delete_webhook(id: $id) { id board_id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rWebhookId = runContext.render(this.webhookId).as(String.class).orElseThrow();
        return Map.of("id", rWebhookId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().webhookId(data.get("delete_webhook").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted webhook id")
        private final String webhookId;
    }
}
