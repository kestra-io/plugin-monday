package io.kestra.plugin.monday.webhooks;

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
class WebhooksTaskTest extends MondayWireMockTest {
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
    void createWebhook() throws Exception {
        stub("{ \"data\": { \"create_webhook\": { \"id\": \"wh1\", \"board_id\": \"10\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("10"))
            .url(Property.ofValue("https://example.com/webhook"))
            .event(Property.ofValue(WebhookEvent.CREATE_ITEM))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getWebhookId(), is("wh1"));
        assertThat(output.getBoardId(), is("10"));
    }

    @Test
    void deleteWebhook() throws Exception {
        stub("{ \"data\": { \"delete_webhook\": { \"id\": \"wh1\", \"board_id\": \"10\" } } }");

        var task = Delete.builder()
            .id(randomId())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .webhookId(Property.ofValue("wh1"))
            .build();

        assertThat(task.run(runContextFactory.of()).getWebhookId(), is("wh1"));
    }
}
