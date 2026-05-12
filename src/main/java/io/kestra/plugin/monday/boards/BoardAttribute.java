package io.kestra.plugin.monday.boards;

public enum BoardAttribute {
    DESCRIPTION,
    NAME,
    COMMUNICATION;

    public String value() {
        return name().toLowerCase();
    }
}
