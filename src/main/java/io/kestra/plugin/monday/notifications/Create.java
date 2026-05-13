package io.kestra.plugin.monday.notifications;

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
@Schema(title = "Send a notification to a Monday user")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Notify a user about an item update",
            code = """
                id: monday_create_notification
                namespace: company.team

                tasks:
                  - id: notify
                    type: io.kestra.plugin.monday.notifications.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    userId: "12345"
                    targetId: "67890"
                    text: "Item updated by the pipeline."
                    targetType: Project
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Recipient user id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> userId;

    @Schema(title = "Target id", description = "Item id when `targetType` is `Project`, or update id when `targetType` is `Post`.")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> targetId;

    @Schema(title = "Notification body text")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> text;

    @Schema(title = "Target type", description = "One of `Project` (item) or `Post` (update). Note: Monday uses the name `Project` to refer to an item in this API.")
    @PluginProperty(group = "main")
    @Builder.Default
    private Property<NotificationTargetType> targetType = Property.ofValue(NotificationTargetType.PROJECT);

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($userId: ID!, $targetId: ID!, $text: String!, $targetType: NotificationTargetType!) {
              create_notification(user_id: $userId, target_id: $targetId, text: $text, target_type: $targetType) { id }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rUserId = runContext.render(this.userId).as(String.class).orElseThrow();
        var rTargetId = runContext.render(this.targetId).as(String.class).orElseThrow();
        var rText = runContext.render(this.text).as(String.class).orElseThrow();
        var rTargetType = runContext.render(this.targetType).as(NotificationTargetType.class).orElse(NotificationTargetType.PROJECT);

        var vars = new HashMap<String, Object>();
        vars.put("userId", rUserId);
        vars.put("targetId", rTargetId);
        vars.put("text", rText);
        vars.put("targetType", rTargetType.value());
        return vars;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().notificationId(data.get("create_notification").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created notification id")
        private final String notificationId;
    }
}
