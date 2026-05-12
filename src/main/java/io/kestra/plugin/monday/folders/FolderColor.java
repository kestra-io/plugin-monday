package io.kestra.plugin.monday.folders;

public enum FolderColor {
    DONE_GREEN,
    WORKING_ORANGE,
    DARK_RED,
    DARK_PURPLE,
    DARK_ORANGE,
    BRIGHT_BLUE,
    BRIGHT_GREEN,
    PURPLE,
    CHILI_BLUE,
    LIPSTICK,
    AQUAMARINE,
    INDIGO,
    SOFIA_PINK,
    STUCK_RED,
    SUNSET;

    public String value() {
        return name();
    }
}
