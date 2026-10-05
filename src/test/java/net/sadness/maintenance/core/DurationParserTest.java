package net.sadness.maintenance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import net.sadness.maintenance.core.InvalidDurationException.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DurationParserTest {

    @ParameterizedTest
    @CsvSource({
        "30s,30",
        "30m,1800",
        "2h,7200",
        "1d,86400",
        "1h30m,5400",
        "1d2h3m4s,93784",
        "90s,90",
        "0h5m,300",
        "30M,1800",
        "2H,7200",
        "  15m  ,900",
        "007m,420",
        "30d,2592000",
        "720h,2592000",
        "43200m,2592000",
        "2592000s,2592000",
        "5s5m,305"
    })
    void parsesValidInput(String input, long expectedSeconds) throws InvalidDurationException {
        assertEquals(Duration.ofSeconds(expectedSeconds), DurationParser.parse(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "   \n"})
    void rejectsEmptyInput(String input) {
        assertEquals(Reason.EMPTY, reasonFor(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "abc", "m", "30", "30x", "30 m", "1h 30m", "1.5h", "1,5h", "m30", "30mm", "1m1m", "1h2h",
        "-5m", "+5m", "--5m", "5m-", "30m!", "１２m", "٣m", "0x10m", "1e3s", "30ms", "1w", "h1"
    })
    void rejectsMalformedInput(String input) {
        assertEquals(Reason.MALFORMED, reasonFor(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "0m", "0h", "0d", "000s", "0h0m0s", "00d00h"})
    void rejectsZero(String input) {
        assertEquals(Reason.ZERO, reasonFor(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "31d", "721h", "43201m", "2592001s", "30d1s",
        "9223372036854775807s", "9223372036854775808s", "99999999999999999999999999s",
        "9223372036854775807d", "106751991167301d", "999999999999999999h", "1000000000000000000m",
        "29d25h", "29d23h59m61s"
    })
    void rejectsTooLarge(String input) {
        assertEquals(Reason.TOO_LARGE, reasonFor(input));
    }

    @Test
    void acceptsExactMaximum() throws InvalidDurationException {
        assertEquals(DurationParser.MAX_DURATION, DurationParser.parse("30d"));
    }

    @Test
    void sumOverflowIsReportedAsTooLarge() {
        assertEquals(Reason.TOO_LARGE, reasonFor("1m9223372036854775807s"));
    }

    @Test
    void messageContainsOffendingInput() {
        InvalidDurationException e = assertThrows(InvalidDurationException.class, () -> DurationParser.parse("zzz"));
        assertEquals(Reason.MALFORMED, e.reason());
        assertEquals("expected a value such as 30m, 1h30m or 45s: 'zzz'", e.getMessage());
    }

    private static Reason reasonFor(String input) {
        return assertThrows(InvalidDurationException.class, () -> DurationParser.parse(input)).reason();
    }
}
