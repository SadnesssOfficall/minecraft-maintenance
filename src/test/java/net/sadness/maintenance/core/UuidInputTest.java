package net.sadness.maintenance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UuidInputTest {

    private static final UUID EXPECTED = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @Test
    void parsesDashedUuid() {
        assertEquals(Optional.of(EXPECTED), UuidInput.parse("069a79f4-44e9-4726-a5be-fca90e38aaf5"));
    }

    @Test
    void parsesUppercaseUuid() {
        assertEquals(Optional.of(EXPECTED), UuidInput.parse("069A79F4-44E9-4726-A5BE-FCA90E38AAF5"));
    }

    @Test
    void parsesCompactUuid() {
        assertEquals(Optional.of(EXPECTED), UuidInput.parse("069a79f444e94726a5befca90e38aaf5"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "Notch", "069a79f4-44e9-4726-a5be-fca90e38aaf", "069a79f4-44e9-4726-a5be-fca90e38aaf55",
        "1-1-1-1-1", "069a79f444e94726a5befca90e38aaf", "069a79f4-44e9-4726-a5be-fca90e38aafg",
        " 069a79f4-44e9-4726-a5be-fca90e38aaf5", "069a79f4-44e9-4726-a5be-fca90e38aaf5\n"
    })
    void rejectsNonUuidInput(String input) {
        assertEquals(Optional.empty(), UuidInput.parse(input));
    }
}
