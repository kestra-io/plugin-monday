package io.kestra.plugin.monday.updates;

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
class UpdatesTaskTest extends MondayWireMockTest {
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
    void createUpdate() throws Exception {
        stub("{ \"data\": { \"create_update\": { \"id\": \"u1\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .itemId(Property.ofValue("9"))
            .body(Property.ofValue("hello"))
            .build();

        assertThat(task.run(runContextFactory.of()).getUpdateId(), is("u1"));
    }

    @Test
    void deleteUpdate() throws Exception {
        stub("{ \"data\": { \"delete_update\": { \"id\": \"u1\" } } }");

        var task = Delete.builder()
            .id(randomId())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .updateId(Property.ofValue("u1"))
            .build();

        assertThat(task.run(runContextFactory.of()).getUpdateId(), is("u1"));
    }

    @Test
    void likeUpdate() throws Exception {
        stub("{ \"data\": { \"like_update\": { \"id\": \"u1\" } } }");

        var task = Like.builder()
            .id(randomId())
            .type(Like.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .updateId(Property.ofValue("u1"))
            .build();

        assertThat(task.run(runContextFactory.of()).getUpdateId(), is("u1"));
    }
}
