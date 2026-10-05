package net.sadness.maintenance.core;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class UuidInput {

    private static final Pattern DASHED = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern COMPACT = Pattern.compile("[0-9a-fA-F]{32}");

    private static final int TIME_LOW_END = 8;
    private static final int TIME_MID_END = 12;
    private static final int TIME_HIGH_END = 16;
    private static final int CLOCK_SEQ_END = 20;

    private UuidInput() {
    }

    public static Optional<UUID> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        if (DASHED.matcher(input).matches()) {
            return Optional.of(UUID.fromString(input));
        }
        if (COMPACT.matcher(input).matches()) {
            String dashed = input.substring(0, TIME_LOW_END)
                    + '-' + input.substring(TIME_LOW_END, TIME_MID_END)
                    + '-' + input.substring(TIME_MID_END, TIME_HIGH_END)
                    + '-' + input.substring(TIME_HIGH_END, CLOCK_SEQ_END)
                    + '-' + input.substring(CLOCK_SEQ_END);
            return Optional.of(UUID.fromString(dashed));
        }
        return Optional.empty();
    }
}
