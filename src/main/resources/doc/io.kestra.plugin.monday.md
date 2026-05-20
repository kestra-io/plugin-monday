# How to use the Monday.com plugin

Manage boards, items, groups, columns, and more on Monday.com from Kestra flows.

## Authentication

Set `apiToken` to your Monday.com API token. Store it in a [secret](https://kestra.io/docs/concepts/secret) and apply it globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

**Items** — `items.Create` creates an item on a `boardId` in a `groupId` with an `itemName` and optional `columnValues`. `items.Update` updates column values on an item by `itemId`. `items.Get` retrieves an item by `itemId`. `items.Query` lists items on a `boardId` — control result handling with `fetchType` and bound results with `limit` (default 500). `items.Delete` and `items.Archive` remove or archive an item by `itemId`. `items.Move` moves an item to a new `groupId`. `items.Trigger` polls a board on a schedule and starts one execution per batch of new or changed items.

**Boards** — `boards.Create` creates a board with `boardName`, `boardKind`, and optional `workspaceId` or `templateId`. `boards.Query` lists boards — filter by `workspaceIds`, `boardKind`, or `state`. `boards.Get`, `boards.Update`, `boards.Delete`, `boards.Archive`, and `boards.Duplicate` manage individual boards.

**Groups** — `groups.Create` adds a group to a `boardId` with a `groupName`. `groups.Update`, `groups.Delete`, `groups.Archive`, and `groups.Duplicate` manage existing groups.

**Columns** — `columns.Create` adds a column to a `boardId` with a `title` and `columnType`. `columns.Update` and `columns.Delete` manage existing columns.

**Subitems** — `subitems.Create` adds a subitem under a `parentItemId`.

**Updates** — `updates.Create` posts a comment on an item by `itemId` with a `body`. `updates.Delete` removes an update and `updates.Like` likes one.

**Other** — `workspaces.Query`/`Create`/`Delete`, `users.Query`/`GetMe`, `notifications.Create`, `webhooks.Create`/`Delete`, `teams.Create`/`AddUsers`/`RemoveUsers`/`Delete`, `folders.Create`/`Delete`, `assets.Get`. Use `query.Query` to send a raw GraphQL `query` with `variables` as an escape hatch.
