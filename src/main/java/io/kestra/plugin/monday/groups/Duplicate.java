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
@Schema(
    title = "Duplicate a group on a Monday board",
    description = "Duplicate a group on a Monday board. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Duplicate a group at the bottom of a board",
            code = """
                id: monday_duplicate_group
                namespace: company.team

                tasks:
                  - id: duplicate
                    type: io.kestra.plugin.monday.groups.Duplicate
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    groupId: "in_progress"
                    groupTitle: "In progress (copy)"
                """
        )
    }
)
public class Duplicate extends AbstractMondayCall<Duplicate.Output> {
    @Schema(title = "Board id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Group id to duplicate")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> groupId;

    @Schema(title = "Place the duplicate at the top of the board")
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<Boolean> addToTop = Property.ofValue(false);

    @Schema(title = "Title for the duplicated group")
    @PluginProperty(group = "main")
    private Property<String> groupTitle;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $groupId: String!, $addToTop: Boolean, $title: String) {
              duplicate_group(board_id: $boardId, group_id: $groupId, add_to_top: $addToTop, group_title: $title) { id title }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rGroupId = runContext.render(this.groupId).as(String.class).orElseThrow();
        var rAddToTop = runContext.render(this.addToTop).as(Boolean.class).orElse(false);
        var rTitle = runContext.render(this.groupTitle).as(String.class).orElse(null);

        var vars = new HashMap<String, Object>();
        vars.put("boardId", rBoardId);
        vars.put("groupId", rGroupId);
        vars.put("addToTop", rAddToTop);
        vars.put("title", rTitle);
        return vars;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var node = data.get("duplicate_group");
        return Output.builder()
            .groupId(node.get("id").asText())
            .title(node.get("title").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Duplicated group id")
        private final String groupId;

        @Schema(title = "Group title")
        private final String title;
    }
}
