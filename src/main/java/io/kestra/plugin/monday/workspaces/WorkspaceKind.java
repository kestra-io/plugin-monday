package io.kestra.plugin.monday.workspaces;

public enum WorkspaceKind {
    OPEN,
    CLOSED;

    public String value() {
        return name().toLowerCase();
    }
}
