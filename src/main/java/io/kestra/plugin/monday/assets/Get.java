package io.kestra.plugin.monday.assets;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.monday.AbstractMondayConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Fetch Monday asset metadata, optionally downloading the file",
    description = "Returns the asset metadata (name, size, extension, url, uploader). When `download` is true the binary content is streamed to Kestra internal storage and the resulting URI is exposed."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Fetch asset metadata",
            code = """
                id: monday_get_asset
                namespace: company.team

                tasks:
                  - id: get
                    type: io.kestra.plugin.monday.assets.Get
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    assetId: "9876543210"
                """
        ),
        @Example(
            full = true,
            title = "Download an asset to internal storage",
            code = """
                id: monday_download_asset
                namespace: company.team

                tasks:
                  - id: download
                    type: io.kestra.plugin.monday.assets.Get
                    apiToken: "{{ secret('MONDAY_API_TOKEN') }}"
                    assetId: "9876543210"
                    download: true
                """
        )
    }
)
public class Get extends AbstractMondayConnection implements RunnableTask<Get.Output> {
    @Schema(title = "Asset id to fetch")
    @PluginProperty(group = "main")
    @NotNull
    private Property<String> assetId;

    @Schema(
        title = "Download the asset content",
        description = "When true, fetches the binary from the asset URL and stores it in Kestra internal storage."
    )
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<Boolean> download = Property.ofValue(false);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rAssetId = runContext.render(this.assetId).as(String.class).orElseThrow();
        var rDownload = runContext.render(this.download).as(Boolean.class).orElse(false);

        Map<String, Object> asset;
        try (var client = client(runContext)) {
            var data = client.execute(
                """
                    query ($assetId: [ID!]!) {
                      assets(ids: $assetId) {
                        id name file_extension file_size url created_at uploaded_by { id name }
                      }
                    }
                    """,
                Map.of("assetId", List.of(rAssetId))
            );
            var assets = data == null ? null : data.get("assets");
            if (assets == null || assets.isEmpty()) {
                return Output.builder().asset(null).uri(null).build();
            }
            asset = MAPPER.convertValue(assets.get(0), new TypeReference<Map<String, Object>>() {});
        }

        URI uri = null;
        if (Boolean.TRUE.equals(rDownload)) {
            var url = asset.get("url");
            if (url instanceof String s && !s.isBlank()) {
                uri = downloadToInternalStorage(runContext, s);
            } else {
                runContext.logger().warn("Asset {} has no downloadable url", rAssetId);
            }
        }

        return Output.builder().asset(asset).uri(uri).build();
    }

    private URI downloadToInternalStorage(RunContext runContext, String url) throws Exception {
        var rToken = runContext.render(this.apiToken).as(String.class).orElseThrow();
        var headers = new HashMap<String, List<String>>();
        headers.put("Authorization", List.of(rToken));

        var request = HttpRequest.builder()
            .uri(URI.create(url))
            .method("GET")
            .headers(HttpHeaders.of(headers, (a, b) -> true))
            .build();

        var tempFile = runContext.workingDir().createTempFile().toFile();

        var config = HttpConfiguration.builder().allowFailed(Property.ofValue(true)).build();
        try (var client = HttpClient.builder().runContext(runContext).configuration(config).build()) {
            client.request(request, response -> {
                var status = response.getStatus().getCode();
                if (status < 200 || status >= 300) {
                    throw new RuntimeException("Asset download failed with HTTP " + status);
                }
                try (var body = response.getBody(); OutputStream out = Files.newOutputStream(tempFile.toPath())) {
                    if (body != null) {
                        body.transferTo(out);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return runContext.storage().putFile(tempFile);
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Asset metadata returned by Monday")
        private final Map<String, Object> asset;

        @Schema(title = "Internal storage URI of the downloaded file (only when `download` is true)")
        private final URI uri;
    }
}
