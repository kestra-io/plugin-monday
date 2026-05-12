package io.kestra.plugin.monday.notifications;

public enum NotificationTargetType {
    PROJECT("Project"),
    POST("Post");

    private final String value;

    NotificationTargetType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
