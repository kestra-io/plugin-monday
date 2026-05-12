package io.kestra.plugin.monday.workspaces;

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

@KestraTest
class WorkspacesTaskTest extends MondayWireMockTest {
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
    void createWorkspace() throws Exception {
        stub("{ \"data\": { \"create_workspace\": { \"id\": \"w1\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .name(Property.ofValue("Marketing"))
            .kind(Property.ofValue(WorkspaceKind.OPEN))
            .build();

        assertThat(task.run(runContextFactory.of()).getWorkspaceId(), is("w1"));
    }

    @Test
    void deleteWorkspace() throws Exception {
        stub("{ \"data\": { \"delete_workspace\": { \"id\": \"w1\" } } }");

        var task = Delete.builder()
            .id(randomId())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .workspaceId(Property.ofValue("w1"))
            .build();

        assertThat(task.run(runContextFactory.of()).getWorkspaceId(), is("w1"));
    }

    @Test
    void queryWorkspaces() throws Exception {
        stub("""
            { "data": { "workspaces": [
              { "id": "w1", "name": "Marketing", "kind": "open", "description": null, "state": "active" },
              { "id": "w2", "name": "Engineering", "kind": "closed", "description": null, "state": "active" }
            ] } }
            """);

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
        assertThat(output.getWorkspaces(), hasSize(2));
    }
}
