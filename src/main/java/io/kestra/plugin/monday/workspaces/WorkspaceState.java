package io.kestra.plugin.monday.workspaces;

public enum WorkspaceState {
    ACTIVE,
    ARCHIVED,
    DELETED,
    ALL;

    public String value() {
        return name().toLowerCase();
    }
}
