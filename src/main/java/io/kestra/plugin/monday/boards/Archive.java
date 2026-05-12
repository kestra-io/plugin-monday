package io.kestra.plugin.monday.boards;

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
@Schema(title = "Archive a Monday board")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Archive a board",
            code = """
                id: monday_archive_board
                namespace: company.team

                tasks:
                  - id: archive
                    type: io.kestra.plugin.monday.boards.Archive
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                """
        )
    }
)
public class Archive extends AbstractMondayCall<Archive.Output> {
    @Schema(title = "Board id to archive")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($boardId: ID!) { archive_board(board_id: $boardId) { id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        return Map.of("boardId", rBoardId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().boardId(data.get("archive_board").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Archived board id")
        private final String boardId;
    }
}
