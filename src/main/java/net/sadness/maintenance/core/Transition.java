package net.sadness.maintenance.core;

public enum Transition {
    ENABLED,
    DISABLED,
    ALREADY_ACTIVE,
    ALREADY_INACTIVE;

    public boolean changed() {
        return this == ENABLED || this == DISABLED;
    }
}
