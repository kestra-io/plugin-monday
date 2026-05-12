package io.kestra.plugin.monday.teams;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.monday.MondayWireMockTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

@KestraTest
class TeamsTaskTest extends MondayWireMockTest {
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
    void createTeam() throws Exception {
        stub("{ \"data\": { \"create_team\": { \"id\": \"t1\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .name(Property.ofValue("Platform"))
            .build();

        assertThat(task.run(runContextFactory.of()).getTeamId(), is("t1"));
    }

    @Test
    void addUsersToTeam() throws Exception {
        stub("{ \"data\": { \"add_users_to_team\": { \"successful_users\": [{ \"id\": \"111\" }], \"failed_users\": [{ \"id\": \"222\" }] } } }");

        var task = AddUsers.builder()
            .id(randomId())
            .type(AddUsers.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .teamId(Property.ofValue("t1"))
            .userIds(Property.ofValue(List.of("111", "222")))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getSuccessfulUserIds(), contains("111"));
        assertThat(output.getFailedUserIds(), contains("222"));
    }

    @Test
    void removeUsersFromTeam() throws Exception {
        stub("{ \"data\": { \"remove_users_from_team\": { \"successful_users\": [{ \"id\": \"111\" }], \"failed_users\": [] } } }");

        var task = RemoveUsers.builder()
            .id(randomId())
            .type(RemoveUsers.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .teamId(Property.ofValue("t1"))
            .userIds(Property.ofValue(List.of("111")))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getSuccessfulUserIds(), contains("111"));
        assertThat(output.getFailedUserIds().isEmpty(), is(true));
    }

    @Test
    void deleteTeam() throws Exception {
        stub("{ \"data\": { \"delete_team\": { \"id\": \"t1\" } } }");

        var task = Delete.builder()
            .id(randomId())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .teamId(Property.ofValue("t1"))
            .build();

        assertThat(task.run(runContextFactory.of()).getTeamId(), is("t1"));
    }
}
