package io.kestra.plugin.monday.teams;

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
@Schema(title = "Delete a Monday team")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Delete a team",
            code = """
                id: monday_delete_team
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.monday.teams.Delete
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    teamId: "12345"
                """
        )
    }
)
public class Delete extends AbstractMondayCall<Delete.Output> {
    @Schema(title = "Team id to delete")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> teamId;

    @Override
    protected String buildQuery(RunContext runContext) {
        return "mutation ($teamId: ID!) { delete_team(team_id: $teamId) { id } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rTeamId = runContext.render(this.teamId).as(String.class).orElseThrow();
        return Map.of("teamId", rTeamId);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().teamId(data.get("delete_team").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted team id")
        private final String teamId;
    }
}
