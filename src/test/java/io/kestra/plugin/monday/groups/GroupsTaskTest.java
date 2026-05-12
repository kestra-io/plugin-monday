package io.kestra.plugin.monday.groups;

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
class GroupsTaskTest extends MondayWireMockTest {
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
    void createGroup() throws Exception {
        stub("{ \"data\": { \"create_group\": { \"id\": \"g1\", \"title\": \"Open\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .groupName(Property.ofValue("Open"))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getGroupId(), is("g1"));
        assertThat(output.getTitle(), is("Open"));
    }

    @Test
    void deleteGroup() throws Exception {
        stub("{ \"data\": { \"delete_group\": { \"id\": \"g1\" } } }");

        var task = Delete.builder()
            .id(randomId())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .groupId(Property.ofValue("g1"))
            .build();

        assertThat(task.run(runContextFactory.of()).getGroupId(), is("g1"));
    }

    @Test
    void updateGroup() throws Exception {
        stub("{ \"data\": { \"update_group\": { \"id\": \"g1\", \"title\": \"Doing\" } } }");

        var task = Update.builder()
            .id(randomId())
            .type(Update.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .groupId(Property.ofValue("g1"))
            .groupAttribute(Property.ofValue(GroupAttribute.TITLE))
            .newValue(Property.ofValue("Doing"))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getGroupId(), is("g1"));
        assertThat(output.getTitle(), is("Doing"));
    }

    @Test
    void archiveGroup() throws Exception {
        stub("{ \"data\": { \"archive_group\": { \"id\": \"g1\" } } }");

        var task = Archive.builder()
            .id(randomId())
            .type(Archive.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .groupId(Property.ofValue("g1"))
            .build();

        assertThat(task.run(runContextFactory.of()).getGroupId(), is("g1"));
    }

    @Test
    void duplicateGroup() throws Exception {
        stub("{ \"data\": { \"duplicate_group\": { \"id\": \"g1_copy\", \"title\": \"Open (copy)\" } } }");

        var task = Duplicate.builder()
            .id(randomId())
            .type(Duplicate.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .groupId(Property.ofValue("g1"))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getGroupId(), is("g1_copy"));
        assertThat(output.getTitle(), is("Open (copy)"));
    }
}
