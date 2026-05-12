package io.kestra.plugin.monday.users;

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
class UsersTaskTest extends MondayWireMockTest {
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
    void getMe() throws Exception {
        stub("{ \"data\": { \"me\": { \"id\": \"1\", \"name\": \"Ada\", \"email\": \"ada@example.com\", \"is_admin\": true } } }");

        var task = GetMe.builder()
            .id(randomId())
            .type(GetMe.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getUser(), notNullValue());
        assertThat(output.getUser().get("id"), is("1"));
        assertThat(output.getUser().get("email"), is("ada@example.com"));
    }

    @Test
    void queryUsers() throws Exception {
        stub("""
            { "data": { "users": [
            { "id": "1", "name": "a", "email": "a@x", "is_admin": true, "is_guest": false, "enabled": true },
            { "id": "2", "name": "b", "email": "b@x", "is_admin": false, "is_guest": false, "enabled": true }
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
        assertThat(output.getUsers(), hasSize(2));
    }
}
