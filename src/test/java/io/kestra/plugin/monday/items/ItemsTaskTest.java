package io.kestra.plugin.monday.items;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.monday.MondayWireMockTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@KestraTest
class ItemsTaskTest extends MondayWireMockTest {
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
    void createItem() throws Exception {
        stub("{ \"data\": { \"create_item\": { \"id\": \"111\", \"name\": \"Hello\" } } }");

        var task = Create.builder()
            .id(randomId())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .itemName(Property.ofValue("Hello"))
            .columnValues(Property.ofValue(Map.of("status", Map.of("label", "Done"))))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getItemId(), is("111"));
        assertThat(output.getItemName(), is("Hello"));

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/v2"))
            .withRequestBody(WireMock.matchingJsonPath("$.variables.columnValues",
                WireMock.equalTo("{\"status\":{\"label\":\"Done\"}}"))));
    }

    @Test
    void updateItem() throws Exception {
        stub("{ \"data\": { \"change_multiple_column_values\": { \"id\": \"222\" } } }");

        var task = Update.builder()
            .id(randomId())
            .type(Update.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .itemId(Property.ofValue("222"))
            .columnValues(Property.ofValue(Map.of("status", "Done")))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getItemId(), is("222"));
    }

    @Test
    void deleteItem() throws Exception {
        stub("{ \"data\": { \"delete_item\": { \"id\": \"333\" } } }");

        var task = Delete.builder()
            .id(randomId())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .itemId(Property.ofValue("333"))
            .build();

        assertThat(task.run(runContextFactory.of()).getItemId(), is("333"));
    }

    @Test
    void archiveItem() throws Exception {
        stub("{ \"data\": { \"archive_item\": { \"id\": \"444\" } } }");

        var task = Archive.builder()
            .id(randomId())
            .type(Archive.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .itemId(Property.ofValue("444"))
            .build();

        assertThat(task.run(runContextFactory.of()).getItemId(), is("444"));
    }

    @Test
    void getItem() throws Exception {
        stub("""
            { "data": { "items": [{ "id": "555", "name": "lead", "state": "active",
            "board": { "id": "1" }, "group": { "id": "g1", "title": "Open" },
            "column_values": [{ "id": "status", "text": "Done", "value": null, "type": "status" }] }] } }"""
            );

        var task = Get.builder()
            .id(randomId())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .itemId(Property.ofValue("555"))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getItem(), notNullValue());
        assertThat(output.getItem().get("id"), is("555"));
    }

    @Test
    void getItemReturnsNullWhenMissing() throws Exception {
        stub("{ \"data\": { \"items\": [] } }");

        var task = Get.builder()
            .id(randomId())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .itemId(Property.ofValue("missing"))
            .build();

        assertThat(task.run(runContextFactory.of()).getItem(), nullValue());
    }

    @Test
    void moveItemToGroup() throws Exception {
        stub("{ \"data\": { \"move_item_to_group\": { \"id\": \"666\" } } }");

        var task = Move.builder()
            .id(randomId())
            .type(Move.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .itemId(Property.ofValue("666"))
            .groupId(Property.ofValue("done"))
            .build();

        assertThat(task.run(runContextFactory.of()).getItemId(), is("666"));
    }

    @Test
    void duplicateItem() throws Exception {
        stub("{ \"data\": { \"duplicate_item\": { \"id\": \"777\", \"name\": \"Copy\" } } }");

        var task = Duplicate.builder()
            .id(randomId())
            .type(Duplicate.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .itemId(Property.ofValue("9"))
            .withUpdates(Property.ofValue(true))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getItemId(), is("777"));
        assertThat(output.getItemName(), is("Copy"));
    }

    @Test
    void queryItemsFetch() throws Exception {
        stub("""
            { "data": { "boards": [{ "items_page": {
            "cursor": null,
            "items": [
              { "id": "1", "name": "a", "state": "active", "created_at": "2025-01-01T00:00:00Z", "updated_at": "2025-01-02T00:00:00Z", "group": {"id":"g","title":"G"}, "column_values": [] },
              { "id": "2", "name": "b", "state": "active", "created_at": "2025-01-01T00:00:00Z", "updated_at": "2025-01-02T00:00:00Z", "group": {"id":"g","title":"G"}, "column_values": [] }
            ]
          } }] } }"""
            );

        var task = Query.builder()
            .id(randomId())
            .type(Query.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getSize(), is(2L));
        assertThat(output.getItems(), hasSize(2));
    }

    @Test
    void queryItemsFetchOne() throws Exception {
        stub("""
            { "data": { "boards": [{ "items_page": {
            "cursor": "abc",
            "items": [{ "id": "1", "name": "a", "state": "active", "created_at": "2025-01-01T00:00:00Z", "updated_at": "2025-01-02T00:00:00Z", "group": {"id":"g","title":"G"}, "column_values": [] }]
          } }] } }"""
            );

        var task = Query.builder()
            .id(randomId())
            .type(Query.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .fetchType(Property.ofValue(FetchType.FETCH_ONE))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getSize(), is(1L));
        assertThat(output.getRow().get("id"), is("1"));
    }

    @Test
    void queryItemsStore() throws Exception {
        stub("""
            { "data": { "boards": [{ "items_page": {
            "cursor": null,
            "items": [{ "id": "1", "name": "a", "state": "active", "created_at": "2025-01-01T00:00:00Z", "updated_at": "2025-01-02T00:00:00Z", "group": {"id":"g","title":"G"}, "column_values": [] }]
          } }] } }"""
            );

        var task = Query.builder()
            .id(randomId())
            .type(Query.class.getName())
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(apiUrl()))
            .boardId(Property.ofValue("1"))
            .fetchType(Property.ofValue(FetchType.STORE))
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getSize(), is(1L));
        assertThat(output.getUri(), notNullValue());
        assertThat(output.getUri().toString(), containsString("kestra://"));
    }
}
