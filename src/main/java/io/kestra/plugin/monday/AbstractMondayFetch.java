package io.kestra.plugin.monday;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Flux;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
public abstract class AbstractMondayFetch<T, O extends io.kestra.core.models.tasks.Output>
    extends AbstractMondayConnection
    implements RunnableTask<O> {

    @Schema(
        title = "Output fetch type",
        description = """
            Controls how results are returned: `FETCH_ONE` returns the first row as a map,
            `FETCH` returns all rows as an in-memory list, `STORE` serialises all rows to
            Kestra internal storage in Ion format and returns a URI, `NONE` discards results
            and returns only the count."""
    )
    @PluginProperty(group = "processing")
    @Builder.Default
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Override
    public final O run(RunContext runContext) throws Exception {
        var rFetchType = runContext.render(fetchType).as(FetchType.class).orElse(FetchType.FETCH);
        var rows = fetchAll(runContext);
        var size = (long) rows.size();
        return switch (rFetchType) {
            case FETCH_ONE -> buildOutput(rows.isEmpty() ? null : rows.getFirst(), null, null, size);
            case FETCH -> buildOutput(null, rows, null, size);
            case STORE -> buildOutput(null, null, storeToInternal(runContext, rows), size);
            case NONE -> buildOutput(null, null, null, size);
        };
    }

    protected abstract List<T> fetchAll(RunContext runContext) throws Exception;

    protected abstract O buildOutput(T row, List<T> items, URI uri, Long size);

    protected URI storeToInternal(RunContext runContext, List<T> rows) throws Exception {
        var tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (var output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8), FileSerde.BUFFER_SIZE)) {
            FileSerde.writeAll(output, Flux.fromIterable(rows)).block();
        }
        return runContext.storage().putFile(tempFile);
    }
}
