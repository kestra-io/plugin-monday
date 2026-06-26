package io.kestra.plugin.monday.folders;

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
    title = "Delete a Monday folder",
    description = "Delete a Monday folder. Uses the Monday.com GraphQL API."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Delete a folder",
            code = """
                id: monday_delete_folder
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.monday.folders.Delete
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    folderId: "1234567890"
                """
        )
    }
)
public class Delete extends AbstractMondayCall<Delete.Output> {
    @Schema(title = "Folder id to delete")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> folderId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($folderId: ID!) { delete_folder(folder_id: $folderId) { id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rFolderId = runContext.render(this.folderId).as(String.class).orElseThrow();
        return Map.of("folderId", rFolderId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().folderId(data.get("delete_folder").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted folder id")
        private final String folderId;
    }
}
