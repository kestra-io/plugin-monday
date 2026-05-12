package io.kestra.plugin.monday.columns;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.monday.MondayWireMockTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KestraTest
class ColumnsTaskTest extends MondayWireMockTest {
    @Inject
    private RunContextFactory runContextFactory;

    @BeforeEach
    void setUp() {
        startWireMock();
    }

    @AfterEach
    void tearDown() {
        stopWireMock();
    }

    @Test
    void createColumn() throws Exception {
        stub("{ \"data\": { \"create_column\": { \"id\": \"status\", \"title\": \"Stage\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .title(Property.ofValue("Stage"))
            .columnType(Property.ofValue("status"))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getColumnId(), is("status"));
        assertThat(output.getTitle(), is("Stage"));
    }

    @Test
    void updateColumnTitle() throws Exception {
        stub("{ \"data\": { \"change_column_metadata\": { \"id\": \"1\" } } }");

        var task = Update.builder()
            .id(randomId())
            .type(Update.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .columnId(Property.ofValue("status"))
            .attribute(Property.ofValue(ColumnAttribute.TITLE))
            .value(Property.ofValue("Stage"))
            .build();

        assertThat(task.run(runContextFactory.of()).getColumnId(), is("status"));
    }

    @Test
    void updateColumnDescription() throws Exception {
        stub("{ \"data\": { \"change_column_metadata\": { \"id\": \"1\" } } }");

        var task = Update.builder()
            .id(randomId())
            .type(Update.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .columnId(Property.ofValue("status"))
            .attribute(Property.ofValue(ColumnAttribute.DESCRIPTION))
            .value(Property.ofValue("Current pipeline stage."))
            .build();

        assertThat(task.run(runContextFactory.of()).getColumnId(), is("status"));
    }

    @Test
    void deleteColumn() throws Exception {
        stub("{ \"data\": { \"delete_column\": { \"id\": \"status\" } } }");

        var task = Delete.builder()
            .id(randomId())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .columnId(Property.ofValue("status"))
            .build();

        assertThat(task.run(runContextFactory.of()).getColumnId(), is("status"));
    }
}
