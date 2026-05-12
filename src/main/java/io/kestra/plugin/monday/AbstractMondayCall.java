package io.kestra.plugin.monday;

import com.fasterxml.jackson.databind.JsonNode;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
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
public abstract class AbstractMondayCall<O extends io.kestra.core.models.tasks.Output>
    extends AbstractMondayConnection
    implements RunnableTask<O> {

    @Override
    public final O run(RunContext runContext) throws Exception {
        var query = buildQuery(runContext);
        var variables = buildVariables(runContext);
        try (var client = client(runContext)) {
            var data = variables == null ? client.execute(query) : client.execute(query, variables);
            if (data == null || data.isNull()) {
                throw new IllegalStateException("Monday API error [NULL_RESPONSE]: returned a null data node");
            }
            return mapOutput(data, variables);
        }
    }

    protected abstract String buildQuery(RunContext runContext) throws Exception;

    protected abstract Map<String, Object> buildVariables(RunContext runContext) throws Exception;

    protected abstract O mapOutput(JsonNode data, Map<String, Object> variables) throws Exception;
}
