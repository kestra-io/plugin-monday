package io.kestra.plugin.monday.columns;

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
    title = "Create a column on a Monday board",
    description = """
        `columnType` accepts standard Monday column types: `text`, `long_text`,
        `numbers`, `status`, `date`, `people`, `dropdown`, `checkbox`, and others."""
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Add a status column to a board",
            code = """
                id: monday_create_column
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.monday.columns.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    boardId: "1234567890"
                    title: "Stage"
                    columnType: status
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Board id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> boardId;

    @Schema(title = "Column title")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> title;

    @Schema(
        title = "Column type",
        description = """
            Monday column type. Common values: `text`, `long_text`, `numbers`, `status`, `date`,
            `dropdown`, `checkbox`, `email`, `phone`, `link`, `people`, `timeline`, `tags`, `file`,
            `rating`, `vote`. See the Monday column types reference for the full list."""
    )
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> columnType;

    @Schema(title = "Column description")
    @PluginProperty(group = "main")
    private Property<String> columnDescription;

    @Schema(title = "Defaults map", description = "Serialized to a JSON string.")
    @PluginProperty(group = "advanced")
    private Property<Map<String, Object>> defaults;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($boardId: ID!, $title: String!, $columnType: ColumnType!, $description: String, $defaults: JSON) {
              create_column(
                board_id: $boardId,
                title: $title,
                column_type: $columnType,
                description: $description,
                defaults: $defaults
              ) { id title }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rBoardId = runContext.render(this.boardId).as(String.class).orElseThrow();
        var rTitle = runContext.render(this.title).as(String.class).orElseThrow();
        var rType = runContext.render(this.columnType).as(String.class).orElseThrow();
        var rDescription = runContext.render(this.columnDescription).as(String.class).orElse(null);
        var rDefaults = this.defaults == null ? Map.<String, Object>of() : runContext.render(this.defaults).asMap(String.class, Object.class);

        var vars = new HashMap<String, Object>();
        vars.put("boardId", rBoardId);
        vars.put("title", rTitle);
        vars.put("columnType", rType);
        vars.put("description", rDescription);
        vars.put("defaults", rDefaults.isEmpty() ? null : MAPPER.writeValueAsString(rDefaults));
        return vars;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var node = data.get("create_column");
        return Output.builder()
            .columnId(node.get("id").asText())
            .title(node.get("title").asText())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created column id")
        private final String columnId;

        @Schema(title = "Column title")
        private final String title;
    }
}
