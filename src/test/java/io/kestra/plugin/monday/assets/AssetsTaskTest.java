package io.kestra.plugin.monday.assets;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.monday.MondayWireMockTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@KestraTest
class AssetsTaskTest extends MondayWireMockTest {
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
    void getAssetMetadataOnly() throws Exception {
        stub("""
            { "data": { "assets": [ {
                "id": "9876543210",
                "name": "report.pdf",
                "file_extension": "pdf",
                "file_size": 1234,
                "url": "%s/some-presigned-url",
                "created_at": "2024-01-01T00:00:00Z",
                "uploaded_by": { "id": "42", "name": "Ada" }
            } ] } }
            """.formatted(wireMock.baseUrl()));

        var task = Get.builder()
            .id(randomId())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .assetId(Property.ofValue("9876543210"))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getAsset(), notNullValue());
        assertThat(output.getAsset().get("id"), is("9876543210"));
        assertThat(output.getAsset().get("name"), is("report.pdf"));
        assertThat(output.getUri(), nullValue());
    }

    @Test
    void getAssetAndDownload() throws Exception {
        var binary = new byte[]{1, 2, 3, 4, 5};

        stub("""
            { "data": { "assets": [ {
                "id": "9876543210",
                "name": "report.pdf",
                "file_extension": "pdf",
                "file_size": 5,
                "url": "%s/some-presigned-url",
                "created_at": "2024-01-01T00:00:00Z",
                "uploaded_by": { "id": "42", "name": "Ada" }
            } ] } }
            """.formatted(wireMock.baseUrl()));

        wireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo("/some-presigned-url"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/octet-stream")
                    .withBody(binary))
        );

        var task = Get.builder()
            .id(randomId())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .assetId(Property.ofValue("9876543210"))
            .download(Property.ofValue(true))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getAsset(), notNullValue());
        assertThat(output.getUri(), notNullValue());
        assertThat(output.getUri().toString(), containsString("kestra://"));
        wireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/some-presigned-url")));
    }
}
