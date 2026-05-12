# Kestra Plugin for Monday

## What

- Provides plugin components under `io.kestra.plugin.monday`.
- Task subpackages: `items`, `subitems`, `boards`, `groups`, `columns`, `updates`, `users`, `workspaces`, `folders`, `teams`, `webhooks`, `notifications`, `query`.
- One trigger: `items.Trigger` (polling).

## Why

- Lets Kestra flows automate Monday workflows: provision boards, groups, columns, workspaces, folders, teams, and webhooks; create or update items and subitems; post comments and notifications; and react to new or updated items via a polling trigger.

## How

### Architecture

Single-module plugin built on a thin GraphQL-only client.

- `AbstractMondayConnection` holds the shared `apiToken`, `apiUrl`, and `apiVersion` properties plus a `protected static final ObjectMapper MAPPER` reused by every subclass.
- `AbstractMondayCall<O>` is the base for one-shot GraphQL calls (queries or mutations). Subclasses implement `buildQuery`, `buildVariables`, and `mapOutput`. The base guards against a null `data` node before calling `mapOutput`.
- `AbstractMondayFetch<T, O>` is the base for paginated reads. It owns the `fetchType` property and routes `FETCH_ONE`, `FETCH`, `STORE`, and `NONE` through `buildOutput`. `STORE` writes Ion via `FileSerde.writeAll`.
- `MondayClient` wraps Kestra's HTTP client (JDK `HttpClient` under the hood). It retries on HTTP 429 and on `COMPLEXITY_BUDGET_EXHAUSTED` errors (up to 3 attempts) using either the API-provided `retry_in_seconds` or an exponential backoff capped at 30 seconds.
- Column values are passed as a `Property<Map<String, Object>>` and serialized to a JSON string before being sent, matching Monday's JSON-in-JSON contract.
- Subitems use the dedicated `create_subitem` mutation; arguments mirror `create_item` minus board and group ids.

### Project Structure

```
plugin-monday/
├── src/main/java/io/kestra/plugin/monday/
│   ├── AbstractMondayConnection.java
│   ├── AbstractMondayCall.java
│   ├── AbstractMondayFetch.java
│   ├── MondayClient.java
│   ├── MondayApiException.java
│   ├── boards/         # Create, Get, Query, Update, Delete, Archive, Duplicate
│   ├── columns/        # Create, Update, Delete
│   ├── folders/        # Create, Delete
│   ├── groups/         # Create, Update, Delete, Archive, Duplicate
│   ├── items/          # Create, Update, Delete, Archive, Get, Query, Move, Duplicate, Trigger
│   ├── notifications/  # Create
│   ├── query/          # Query (generic GraphQL escape hatch)
│   ├── subitems/       # Create
│   ├── teams/          # Create
│   ├── updates/        # Create, Delete, Like
│   ├── users/          # GetMe, Query
│   ├── webhooks/       # Create, Delete
│   └── workspaces/     # Create, Query, Delete
└── src/test/java/io/kestra/plugin/monday/
    ├── MondayClientTest.java
    ├── MondayWireMockTest.java
    └── <subpackage>/*TaskTest.java  # WireMock-backed unit tests
```

### Testing

WireMock-backed unit tests, one per subpackage, run on every build. No live API tests.

## References

- Monday developer center: https://developer.monday.com/api-reference/
- Kestra plugin developer guide: https://kestra.io/docs/plugin-developer-guide
