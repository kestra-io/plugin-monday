package io.kestra.plugin.monday.subitems;

import com.github.tomakehurst.wiremock.client.WireMock;
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
import static org.hamcrest.Matchers.is;

@KestraTest
class SubitemsTaskTest extends MondayWireMockTest {
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
    void createSubitem() throws Exception {
        stub("{ \"data\": { \"create_subitem\": { \"id\": \"888\", \"name\": \"Child\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .parentItemId(Property.ofValue("111"))
            .itemName(Property.ofValue("Child"))
            .columnValues(Property.ofValue(Map.of("status", Map.of("label", "Working on it"))))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getItemId(), is("888"));
        assertThat(output.getItemName(), is("Child"));

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/v2"))
            .withRequestBody(WireMock.matchingJsonPath("$.variables.parentItemId", WireMock.equalTo("111")))
            .withRequestBody(WireMock.matchingJsonPath("$.variables.columnValues",
                WireMock.equalTo("{\"status\":{\"label\":\"Working on it\"}}"))));
    }

    @Test
    void createSubitemWithoutColumns() throws Exception {
        stub("{ \"data\": { \"create_subitem\": { \"id\": \"999\", \"name\": \"Bare\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .parentItemId(Property.ofValue("222"))
            .itemName(Property.ofValue("Bare"))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getItemId(), is("999"));
        assertThat(output.getItemName(), is("Bare"));
    }
}
