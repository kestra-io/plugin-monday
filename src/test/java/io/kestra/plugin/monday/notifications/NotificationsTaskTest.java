package io.kestra.plugin.monday.notifications;

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
class NotificationsTaskTest extends MondayWireMockTest {
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
    void createNotification() throws Exception {
        stub("{ \"data\": { \"create_notification\": { \"id\": \"n1\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .userId(Property.ofValue("12345"))
            .targetId(Property.ofValue("67890"))
            .text(Property.ofValue("hello"))
            .targetType(Property.ofValue(NotificationTargetType.PROJECT))
            .build();

        assertThat(task.run(runContextFactory.of()).getNotificationId(), is("n1"));
    }
}
