package io.kestra.plugin.monday.boards;

public enum DuplicateBoardType {
    WITH_STRUCTURE("duplicate_board_with_structure"),
    WITH_PULSES("duplicate_board_with_pulses"),
    WITH_PULSES_AND_UPDATES("duplicate_board_with_pulses_and_updates");

    private final String value;

    DuplicateBoardType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
