package io.kestra.plugin.monday;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.util.UUID;

/**
 * Base helper that boots an HTTP-only WireMock server on a random port for each test class.
 * Each subclass calls {@link #stub(String)} to register a JSON response for the next POST
 * to /v2 (monday's only endpoint). Task ids are randomized via {@link #randomId()} to keep
 * concurrent test runs isolated.
 */
public abstract class MondayWireMockTest {
    protected WireMockServer wireMock;

    protected void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    protected void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    protected String apiUrl() {
        return wireMock.baseUrl() + "/v2";
    }

    protected void stub(String responseBody) {
        wireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v2"))
                .willReturn(WireMock.okJson(responseBody))
        );
    }

    protected void stubStatus(int status, String responseBody) {
        wireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v2"))
                .willReturn(WireMock.aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(responseBody))
        );
    }

    protected String randomId() {
        return "task-" + UUID.randomUUID();
    }
}
