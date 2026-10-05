package net.sadness.maintenance.core;

public final class InvalidDurationException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum Reason {
        EMPTY("no duration given"),
        MALFORMED("expected a value such as 30m, 1h30m or 45s"),
        ZERO("duration must be greater than zero"),
        TOO_LARGE("duration exceeds the maximum");

        private final String description;

        Reason(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    private final Reason reason;

    public InvalidDurationException(Reason reason, String input) {
        super(reason.description() + ": '" + input + "'");
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
