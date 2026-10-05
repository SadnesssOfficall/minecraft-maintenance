package net.sadness.maintenance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationFormatterTest {

    @Test
    void formatsSecondsOnly() {
        assertEquals("45s", DurationFormatter.format(Duration.ofSeconds(45)));
    }

    @Test
    void formatsAllUnits() {
        assertEquals("1d 2h 3m 4s", DurationFormatter.format(Duration.ofSeconds(93784)));
    }

    @Test
    void skipsZeroUnits() {
        assertEquals("1h 5s", DurationFormatter.format(Duration.ofSeconds(3605)));
        assertEquals("2d", DurationFormatter.format(Duration.ofDays(2)));
    }

    @Test
    void zeroAndNegativeFormatAsZeroSeconds() {
        assertEquals("0s", DurationFormatter.format(Duration.ZERO));
        assertEquals("0s", DurationFormatter.format(Duration.ofSeconds(-30)));
    }

    @Test
    void partialSecondsRoundUp() {
        assertEquals("1s", DurationFormatter.format(Duration.ofMillis(1)));
        assertEquals("10s", DurationFormatter.format(Duration.ofMillis(9_001)));
    }

    @Test
    void roundTripsParserOutput() throws InvalidDurationException {
        assertEquals("1h 30m", DurationFormatter.format(DurationParser.parse("90m")));
    }
}
