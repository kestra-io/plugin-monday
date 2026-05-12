package io.kestra.plugin.monday.groups;

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
@Schema(title = "Update an attribute of a group on a Monday board")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Rename a group",
            code = """
                id: monday_update_group
                namespace: company.team

                tasks:
                  - id: rename
                    type: io.kestra.plugin.monday.groups.Update
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    groupId: "in_progress"
                    groupAttribute: title
                    newValue: "Doing"
                """
        )
    }
)
public class Update extends AbstractMondayCall<Update.Output> {
    @Schema(title = "Board id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Group id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> groupId;

    @Schema(
        title = "Group attribute",
        description = "One of `title`, `color`, `position`, `relative_position_after`, `relative_position_before`."
    )
    @PluginProperty(group = "main")
    @NotNull
    private Property<GroupAttribute> groupAttribute;

    @Schema(title = "New value for the chosen attribute")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> newValue;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $groupId: String!, $attr: GroupAttributes!, $value: String!) {
              update_group(board_id: $boardId, group_id: $groupId, group_attribute: $attr, new_value: $value) { id title }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rGroupId = runContext.render(this.groupId).as(String.class).orElseThrow();
        var rAttr = runContext.render(this.groupAttribute).as(GroupAttribute.class).orElseThrow();
        var rValue = runContext.render(this.newValue).as(String.class).orElseThrow();
        return Map.of(
            "boardId", rBoardId,
            "groupId", rGroupId,
            "attr", rAttr.value(),
            "value", rValue
        );
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var node = data.get("update_group");
        return Output.builder()
            .groupId(node.get("id").asText())
            .title(node.get("title").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Updated group id")
        private final String groupId;

        @Schema(title = "Group title")
        private final String title;
    }
}
