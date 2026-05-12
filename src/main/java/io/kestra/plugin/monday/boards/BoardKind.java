package io.kestra.plugin.monday.boards;

public enum BoardKind {
    PUBLIC,
    PRIVATE,
    SHARE;

    public String value() {
        return name().toLowerCase();
    }
}
