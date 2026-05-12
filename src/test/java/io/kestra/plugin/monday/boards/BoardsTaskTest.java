package io.kestra.plugin.monday.boards;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.monday.MondayWireMockTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
class BoardsTaskTest extends MondayWireMockTest {
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
    void createBoard() throws Exception {
        stub("{ \"data\": { \"create_board\": { \"id\": \"10\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardName(Property.ofValue("Q1"))
            .build();

        assertThat(task.run(runContextFactory.of()).getBoardId(), is("10"));
    }

    @Test
    void getBoard() throws Exception {
        stub("{ \"data\": { \"boards\": [{ \"id\": \"10\", \"name\": \"Q1\", \"description\": null, \"state\": \"active\", \"workspace_id\": \"1\", \"columns\": [], \"groups\": [] }] } }");

        var task = Get.builder()
            .id(randomId())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("10"))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getBoard(), notNullValue());
        assertThat(output.getBoard().get("id"), is("10"));
    }

    @Test
    void queryBoards() throws Exception {
        stub("""
            { "data": { "boards": [
            { "id": "1", "name": "a", "description": null, "state": "active", "workspace_id": "1", "columns": [], "groups": [] },
            { "id": "2", "name": "b", "description": null, "state": "active", "workspace_id": "1", "columns": [], "groups": [] }
          ] } }"""
            );

        var task = Query.builder()
            .id(randomId())
            .type(Query.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .limit(Property.ofValue(50))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getSize(), is(2L));
        assertThat(output.getBoards(), hasSize(2));
    }

    @Test
    void deleteBoard() throws Exception {
        stub("{ \"data\": { \"delete_board\": { \"id\": \"10\" } } }");

        var task = Delete.builder()
            .id(randomId())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("10"))
            .build();

        assertThat(task.run(runContextFactory.of()).getBoardId(), is("10"));
    }

    @Test
    void duplicateBoard() throws Exception {
        stub("{ \"data\": { \"duplicate_board\": { \"board\": { \"id\": \"20\" } } } }");

        var task = Duplicate.builder()
            .id(randomId())
            .type(Duplicate.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("10"))
            .build();

        assertThat(task.run(runContextFactory.of()).getBoardId(), is("20"));
    }

    @Test
    void updateBoard() throws Exception {
        stub("{ \"data\": { \"update_board\": \"true\" } }");

        var task = Update.builder()
            .id(randomId())
            .type(Update.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("10"))
            .boardAttribute(Property.ofValue(BoardAttribute.NAME))
            .newValue(Property.ofValue("Q2"))
            .build();

        assertThat(task.run(runContextFactory.of()).getBoardId(), is("10"));
    }

    @Test
    void archiveBoard() throws Exception {
        stub("{ \"data\": { \"archive_board\": { \"id\": \"10\" } } }");

        var task = Archive.builder()
            .id(randomId())
            .type(Archive.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("10"))
            .build();

        assertThat(task.run(runContextFactory.of()).getBoardId(), is("10"));
    }
}
