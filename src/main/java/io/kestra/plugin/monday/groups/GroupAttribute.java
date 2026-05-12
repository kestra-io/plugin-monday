package io.kestra.plugin.monday.groups;

public enum GroupAttribute {
    TITLE,
    COLOR,
    POSITION,
    RELATIVE_POSITION_AFTER,
    RELATIVE_POSITION_BEFORE;

    public String value() {
        return name().toLowerCase();
    }
}
