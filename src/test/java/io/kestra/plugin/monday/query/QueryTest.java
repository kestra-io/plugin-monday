package io.kestra.plugin.monday.query;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.monday.MondayWireMockTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
class QueryTest extends MondayWireMockTest {
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
    void runsArbitraryQuery() throws Exception {
        stub("{ \"data\": { \"me\": { \"id\": \"42\" } } }");

        var task = Query.builder()
            .id(randomId())
            .type(Query.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .query(Property.ofValue("query { me { id } }"))
            .variables(Property.ofValue(Map.of("x", "y")))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getData(), notNullValue());
        var me = (Map<?, ?>) output.getData().get("me");
        assertThat(me, hasEntry("id", "42"));
    }
}
