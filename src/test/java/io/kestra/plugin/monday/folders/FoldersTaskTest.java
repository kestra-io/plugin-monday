package io.kestra.plugin.monday.folders;

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
class FoldersTaskTest extends MondayWireMockTest {
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
    void createFolder() throws Exception {
        stub("{ \"data\": { \"create_folder\": { \"id\": \"f1\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .name(Property.ofValue("Campaigns"))
            .workspaceId(Property.ofValue("w1"))
            .build();

        assertThat(task.run(runContextFactory.of()).getFolderId(), is("f1"));
    }

    @Test
    void createFolderWithColor() throws Exception {
        stub("{ \"data\": { \"create_folder\": { \"id\": \"f2\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .name(Property.ofValue("Campaigns"))
            .workspaceId(Property.ofValue("w1"))
            .color(Property.ofValue(FolderColor.DONE_GREEN))
            .build();

        assertThat(task.run(runContextFactory.of()).getFolderId(), is("f2"));
    }

    @Test
    void deleteFolder() throws Exception {
        stub("{ \"data\": { \"delete_folder\": { \"id\": \"f1\" } } }");

        var task = Delete.builder()
            .id(randomId())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .folderId(Property.ofValue("f1"))
            .build();

        assertThat(task.run(runContextFactory.of()).getFolderId(), is("f1"));
    }
}
