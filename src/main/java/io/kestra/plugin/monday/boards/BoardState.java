package io.kestra.plugin.monday.boards;

public enum BoardState {
    ACTIVE,
    ARCHIVED,
    DELETED,
    ALL;

    public String value() {
        return name().toLowerCase();
    }
}
