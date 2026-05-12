package io.kestra.plugin.monday.columns;

public enum ColumnAttribute {
    TITLE,
    DESCRIPTION;

    public String value() {
        return name().toLowerCase();
    }
}
