package net.sadness.maintenance.core;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class DurationFormatter {

    private record Segment(ChronoUnit unit, String suffix) {
    }

    private static final List<Segment> SEGMENTS = List.of(
            new Segment(ChronoUnit.DAYS, "d"),
            new Segment(ChronoUnit.HOURS, "h"),
            new Segment(ChronoUnit.MINUTES, "m"),
            new Segment(ChronoUnit.SECONDS, "s"));

    private static final String ZERO = "0s";

    private DurationFormatter() {
    }

    public static String format(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            return ZERO;
        }
        long remaining = duration.getSeconds() + (duration.getNano() > 0 ? 1 : 0);
        StringBuilder out = new StringBuilder();
        for (Segment segment : SEGMENTS) {
            long unitSeconds = segment.unit().getDuration().getSeconds();
            long count = remaining / unitSeconds;
            remaining %= unitSeconds;
            if (count > 0) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(count).append(segment.suffix());
            }
        }
        return out.toString();
    }
}
