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

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(title = "Create a group on a Monday board")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Create a group at the top of a board",
            code = """
                id: monday_create_group
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.monday.groups.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    groupName: "In progress"
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Board id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Group name")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> groupName;

    @Schema(title = "Position method", description = "One of `before_at`, `after_at`. Must be combined with `relativeTo`.")
    @PluginProperty(group = "advanced")
    private Property<PositionRelativeMethod> positionRelativeMethod;

    @Schema(title = "Existing group id to position relative to", description = "Requires `positionRelativeMethod` to be set.")
    @PluginProperty(group = "advanced")
    private Property<String> relativeTo;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $groupName: String!, $positionMethod: PositionRelative, $relativeTo: String) {
              create_group(
                board_id: $boardId,
                group_name: $groupName,
                position_relative_method: $positionMethod,
                relative_to: $relativeTo
              ) { id title }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rGroupName = runContext.render(this.groupName).as(String.class).orElseThrow();
        var rMethod = runContext.render(this.positionRelativeMethod).as(PositionRelativeMethod.class).orElse(null);
        var rRelative = runContext.render(this.relativeTo).as(String.class).orElse(null);

        var vars = new HashMap<String, Object>();
        vars.put("boardId", rBoardId);
        vars.put("groupName", rGroupName);
        vars.put("positionMethod", rMethod == null ? null : rMethod.value());
        vars.put("relativeTo", rRelative);
        return vars;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var node = data.get("create_group");
        return Output.builder()
            .groupId(node.get("id").asText())
            .title(node.get("title").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created group id")
        private final String groupId;

        @Schema(title = "Group title")
        private final String title;
    }
}
