package io.kestra.plugin.monday.users;

public enum UserKind {
    ALL,
    NON_GUESTS,
    GUESTS,
    NON_PENDING;

    public String value() {
        return name().toLowerCase();
    }
}
