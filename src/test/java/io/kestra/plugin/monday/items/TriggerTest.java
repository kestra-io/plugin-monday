package io.kestra.plugin.monday.items;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.flows.Flow;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.triggers.TriggerContext;
import io.kestra.core.runners.DefaultRunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.monday.MondayWireMockTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@KestraTest
class TriggerTest extends MondayWireMockTest {
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
    void emptyResponseReturnsEmpty() throws Exception {
        stub("{ \"data\": { \"boards\": [{ \"items_page\": { \"items\": [] } }] } }");

        var trigger = Trigger.builder()
            .id(randomId())
            .type(Trigger.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .interval(Duration.ofMinutes(1))
            .build();

        var conditionContext = conditionContext(trigger, triggerContext(trigger));
        var triggerContext = triggerContext(trigger);
        assertThat(trigger.evaluate(conditionContext, triggerContext).isPresent(), is(false));
    }

    @Test
    void recentItemsProduceExecution() throws Exception {
        var future = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1).toInstant().toString();
        stub("""
            { "data": { "boards": [{ "items_page": { "items": [
            { "id": "1", "name": "a", "updated_at": "%s", "group": { "id": "g", "title": "G" }, "column_values": [] }
          ] } }] } }"""
            .formatted(future));

        var trigger = Trigger.builder()
            .id(randomId())
            .type(Trigger.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .interval(Duration.ofMinutes(1))
            .build();

        var execution = trigger.evaluate(conditionContext(trigger, triggerContext(trigger)), triggerContext(trigger));
        assertThat(execution.isPresent(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    void groupIdFilterIncludesOnlyMatchingGroup() throws Exception {
        var future = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1).toInstant().toString();
        stub("""
            { "data": { "boards": [{ "items_page": { "items": [
              { "id": "1", "name": "in-topics", "updated_at": "%s", "group": { "id": "topics", "title": "Topics" }, "column_values": [] },
              { "id": "2", "name": "in-other", "updated_at": "%s", "group": { "id": "other", "title": "Other" }, "column_values": [] }
            ] } }] } }"""
            .formatted(future, future));

        var trigger = Trigger.builder()
            .id(randomId())
            .type(Trigger.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .groupId(Property.ofValue("topics"))
            .interval(Duration.ofMinutes(1))
            .build();

        var execution = trigger.evaluate(conditionContext(trigger, triggerContext(trigger)), triggerContext(trigger));
        assertThat(execution.isPresent(), is(true));

        var outputs = execution.get().getTrigger().getVariables();
        var items = (List<Map<String, Object>>) outputs.get("items");
        assertThat(items, hasSize(1));
        var group = (Map<String, Object>) items.getFirst().get("group");
        assertThat(group.get("id"), is("topics"));
    }

    private ConditionContext conditionContext(Trigger trigger, TriggerContext triggerContext) {
        var flow = Flow.builder().id("f").namespace("io.kestra.test").build();
        var runContext = (DefaultRunContext) runContextFactory.of(flow, trigger);
        runContextFactory.initializer().forScheduler(runContext, triggerContext, trigger);
        return ConditionContext.builder().runContext(runContext).flow(flow).build();
    }

    private TriggerContext triggerContext(Trigger trigger) {
        return TriggerContext.builder()
            .triggerId(trigger.getId())
            .flowId("f")
            .namespace("io.kestra.test")
            .date(ZonedDateTime.now(ZoneOffset.UTC).minusHours(1))
            .build();
    }
}
