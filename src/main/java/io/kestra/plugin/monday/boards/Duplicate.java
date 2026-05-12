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
@Schema(title = "Duplicate a Monday board")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Duplicate a board with its structure",
            code = """
                id: monday_duplicate_board
                namespace: company.team

                tasks:
                  - id: duplicate
                    type: io.kestra.plugin.monday.boards.Duplicate
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    duplicateType: duplicate_board_with_structure
                """
        )
    }
)
public class Duplicate extends AbstractMondayCall<Duplicate.Output> {
    @Schema(title = "Source board id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Duplicate type", description = "One of `duplicate_board_with_structure`, `duplicate_board_with_pulses`, `duplicate_board_with_pulses_and_updates`.")
    @PluginProperty(group = "main")
    @Builder.Default
    private Property<DuplicateBoardType> duplicateType = Property.ofValue(DuplicateBoardType.WITH_STRUCTURE);

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $type: DuplicateBoardType!) {
              duplicate_board(board_id: $boardId, duplicate_type: $type) { board { id } }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rType = runContext.render(this.duplicateType).as(DuplicateBoardType.class).orElse(DuplicateBoardType.WITH_STRUCTURE);
        return Map.of("boardId", rBoardId, "type", rType.value());
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder()
            .boardId(data.get("duplicate_board").get("board").get("id").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Duplicated board id")
        private final String boardId;
    }
}
