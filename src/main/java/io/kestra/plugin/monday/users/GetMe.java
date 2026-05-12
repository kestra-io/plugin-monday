package io.kestra.plugin.monday.users;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.monday.AbstractMondayCall;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(title = "Fetch the user behind the API token")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Fetch the authenticated user. Useful as a connection sanity check",
            code = """
                id: monday_get_me
                namespace: company.team

                tasks:
                  - id: me
                    type: io.kestra.plugin.monday.users.GetMe
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                """
        )
    }
)
public class GetMe extends AbstractMondayCall<GetMe.Output> {
    @Override
    protected String buildQuery(RunContext runContext) {
        return "query { me { id name email is_admin } }";
    }

    @Override
    protected Map<String, Object> buildVariables(RunContext runContext) {
        return null;
    }

    @Override
    protected Output mapOutput(JsonNode data, Map<String, Object> variables) {
        var user = MAPPER.convertValue(data.get("me"), new TypeReference<Map<String, Object>>() {});
        return Output.builder().user(user).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Authenticated user payload")
        private final Map<String, Object> user;
    }
}
