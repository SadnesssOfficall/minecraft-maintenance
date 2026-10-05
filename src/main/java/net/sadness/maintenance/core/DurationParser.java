package net.sadness.maintenance.core;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DurationParser {

    public static final Duration MAX_DURATION = Duration.ofDays(30);

    private static final Map<Character, ChronoUnit> UNITS = Map.of(
            's', ChronoUnit.SECONDS,
            'm', ChronoUnit.MINUTES,
            'h', ChronoUnit.HOURS,
            'd', ChronoUnit.DAYS);

    private DurationParser() {
    }

    public static Duration parse(String input) throws InvalidDurationException {
        if (input == null) {
            throw new InvalidDurationException(InvalidDurationException.Reason.EMPTY, "null");
        }
        String text = input.strip().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            throw new InvalidDurationException(InvalidDurationException.Reason.EMPTY, input);
        }

        Set<ChronoUnit> seen = EnumSet.noneOf(ChronoUnit.class);
        long totalSeconds = 0;
        int index = 0;
        while (index < text.length()) {
            int digitsStart = index;
            while (index < text.length() && isAsciiDigit(text.charAt(index))) {
                index++;
            }
            if (index == digitsStart || index == text.length()) {
                throw malformed(input);
            }
            ChronoUnit unit = UNITS.get(text.charAt(index));
            if (unit == null || !seen.add(unit)) {
                throw malformed(input);
            }
            long segmentSeconds = toSeconds(text.substring(digitsStart, index), unit, input);
            totalSeconds = add(totalSeconds, segmentSeconds, input);
            index++;
        }

        if (totalSeconds == 0) {
            throw new InvalidDurationException(InvalidDurationException.Reason.ZERO, input);
        }
        return Duration.ofSeconds(totalSeconds);
    }

    private static long toSeconds(String digits, ChronoUnit unit, String input) throws InvalidDurationException {
        try {
            return Math.multiplyExact(Long.parseLong(digits), unit.getDuration().getSeconds());
        } catch (NumberFormatException | ArithmeticException e) {
            throw tooLarge(input);
        }
    }

    private static long add(long total, long segment, String input) throws InvalidDurationException {
        long sum;
        try {
            sum = Math.addExact(total, segment);
        } catch (ArithmeticException e) {
            throw tooLarge(input);
        }
        if (sum > MAX_DURATION.getSeconds()) {
            throw tooLarge(input);
        }
        return sum;
    }

    private static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static InvalidDurationException malformed(String input) {
        return new InvalidDurationException(InvalidDurationException.Reason.MALFORMED, input);
    }

    private static InvalidDurationException tooLarge(String input) {
        return new InvalidDurationException(InvalidDurationException.Reason.TOO_LARGE, input);
    }
}
