package io.kestra.plugin.monday;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class MondayClientTest extends MondayWireMockTest {
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
    void parsesSuccessfulData() throws Exception {
        stub("""
            { "data": { "me": { "id": "42", "name": "Ada" } }, "account_id": 1 }
            """);

        var runContext = runContextFactory.of();
        try (var client = new MondayClient(runContext, apiUrl(), "test-token", "2024-10", 3, Duration.ofMillis(50), Duration.ofMillis(200))) {
            var data = client.execute("query { me { id } }");
            assertThat(data.get("me").get("id").asText(), is("42"));
        }
    }

    @Test
    void translatesErrorCodeToException() throws Exception {
        stub("""
            { "data": null, "error_code": "InvalidUserId", "error_message": "bad id", "errors": [{"message": "missing"}] }
            """);

        var runContext = runContextFactory.of();
        try (var client = new MondayClient(runContext, apiUrl(), "test-token", "2024-10", 3, Duration.ofMillis(50), Duration.ofMillis(200))) {
            var ex = assertThrows(IllegalStateException.class, () -> client.execute("query { me { id } }"));
            assertThat(ex.getMessage(), containsString("InvalidUserId"));
            assertThat(ex.getMessage(), containsString("bad id"));
            assertThat(ex.getMessage(), containsString("missing"));
        }
    }

    @Test
    void sendsAuthAndVersionHeaders() throws Exception {
        stub("{ \"data\": { \"me\": { \"id\": \"1\" } } }");

        var runContext = runContextFactory.of();
        try (var client = new MondayClient(runContext, apiUrl(), "secret-token", "2024-07", 3, Duration.ofMillis(50), Duration.ofMillis(200))) {
            client.execute("query { me { id } }");
        }

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/v2"))
            .withHeader("Authorization", WireMock.equalTo("secret-token"))
            .withHeader("API-Version", WireMock.equalTo("2024-07"))
            .withRequestBody(WireMock.containing("\"query\"")));
    }

    @Test
    void retriesOn429ThenSucceeds() throws Exception {
        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/v2"))
            .inScenario("retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(WireMock.aResponse().withStatus(429)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"error_data\": { \"retry_in_seconds\": 1 } }"))
            .willSetStateTo("done"));
        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/v2"))
            .inScenario("retry")
            .whenScenarioStateIs("done")
            .willReturn(WireMock.okJson("{ \"data\": { \"me\": { \"id\": \"7\" } } }")));

        var runContext = runContextFactory.of();
        try (var client = new MondayClient(runContext, apiUrl(), "test-token", "2024-10", 3, Duration.ofMillis(50), Duration.ofMillis(200))) {
            var data = client.execute("query { me { id } }");
            assertThat(data, notNullValue());
            assertThat(data.get("me").get("id").asText(), is("7"));
        }
    }

    @Test
    void sendsVariables() throws Exception {
        stub("{ \"data\": { \"items\": [] } }");

        var runContext = runContextFactory.of();
        try (var client = new MondayClient(runContext, apiUrl(), "test-token", "2024-10", 3, Duration.ofMillis(50), Duration.ofMillis(200))) {
            client.execute("query ($id: ID!) { items(ids: [$id]) { id } }", Map.of("id", "9"));
        }

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/v2"))
            .withRequestBody(WireMock.matchingJsonPath("$.variables.id", WireMock.equalTo("9"))));
    }
}
