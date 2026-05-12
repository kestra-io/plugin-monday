package io.kestra.plugin.monday.groups;

public enum PositionRelativeMethod {
    BEFORE_AT,
    AFTER_AT;

    public String value() {
        return name().toLowerCase();
    }
}
