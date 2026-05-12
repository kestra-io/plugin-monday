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

import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Remove users from a Monday team",
    description = "Returns the user ids that were removed successfully and those Monday rejected."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Remove users from a team",
            code = """
                id: monday_remove_users_from_team
                namespace: company.team

                tasks:
                  - id: remove
                    type: io.kestra.plugin.monday.teams.RemoveUsers
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    teamId: "12345"
                    userIds:
                      - "111"
                      - "222"
                """
        )
    }
)
public class RemoveUsers extends AbstractMondayCall<RemoveUsers.Output> {
    @Schema(title = "Team id")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> teamId;

    @Schema(title = "User ids to remove")
    @PluginProperty(group = "main")
    @NotNull
    private Property<List<String>> userIds;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($teamId: ID!, $userIds: [ID!]!) {
              remove_users_from_team(team_id: $teamId, user_ids: $userIds) {
                successful_users { id }
                failed_users { id }
              }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rTeamId = runContext.render(this.teamId).as(String.class).orElseThrow();
        var rUserIds = runContext.render(this.userIds).asList(String.class);
        return Map.of("teamId", rTeamId, "userIds", rUserIds);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var node = data.get("remove_users_from_team");
        return Output.builder()
            .successfulUserIds(AddUsers.extractIds(node, "successful_users"))
            .failedUserIds(AddUsers.extractIds(node, "failed_users"))
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Ids of users removed successfully")
        private final List<String> successfulUserIds;

        @Schema(title = "Ids of users Monday rejected")
        private final List<String> failedUserIds;
    }
}
