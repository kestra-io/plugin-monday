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
@Schema(
    title = "Delete a group from a Monday board",
    description = "Delete a group from a Monday board. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Delete a group",
            code = """
                id: monday_delete_group
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.monday.groups.Delete
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    groupId: "in_progress"
                """
        )
    }
)
public class Delete extends AbstractMondayCall<Delete.Output> {
    @Schema(title = "Board id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Group id to delete")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> groupId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($boardId: ID!, $groupId: String!) { delete_group(board_id: $boardId, group_id: $groupId) { id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rGroupId = runContext.render(this.groupId).as(String.class).orElseThrow();
        return Map.of("boardId", rBoardId, "groupId", rGroupId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().groupId(data.get("delete_group").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted group id")
        private final String groupId;
    }
}
