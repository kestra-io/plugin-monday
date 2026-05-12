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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a Monday team",
    description = "Creating a team requires admin permissions on the Monday account."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Create a team with members",
            code = """
                id: monday_create_team
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.monday.teams.Create
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    name: "Platform"
                    subscriberIds:
                      - "12345"
                      - "67890"
                """
        )
    }
)
public class Create extends AbstractMondayCall<Create.Output> {
    @Schema(title = "Team name")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> name;

    @Schema(title = "Create as a guest team")
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<Boolean> isGuestTeam = Property.ofValue(false);

    @Schema(title = "Parent team id")
    @PluginProperty(group = "main")
    private Property<String> parentTeamId;

    @Schema(title = "Initial subscriber user ids")
    @PluginProperty(group = "main")
    private Property<List<String>> subscriberIds;

    @Override
    protected String buildQuery(RunContext runContext) {
        return """
            mutation ($input: CreateTeamAttributesInput!) {
              create_team(input: $input) { id }
            }
            """;
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) throws Exception {
        var rName = runContext.render(this.name).as(String.class).orElseThrow();
        var rIsGuest = runContext.render(this.isGuestTeam).as(Boolean.class).orElse(false);
        var rParentTeamId = runContext.render(this.parentTeamId).as(String.class).orElse(null);
        var rSubscriberIds = runContext.render(this.subscriberIds).asList(String.class);

        var input = new HashMap<String, Object>();
        input.put("name", rName);
        input.put("is_guest_team", rIsGuest);
        if (rParentTeamId != null) {
            input.put("parent_team_id", rParentTeamId);
        }
        if (!rSubscriberIds.isEmpty()) {
            input.put("subscriber_ids", rSubscriberIds);
        }

        return Map.of("input", input);
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        return Output.builder().teamId(data.get("create_team").get("id").asText()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created team id")
        private final String teamId;
    }
}
