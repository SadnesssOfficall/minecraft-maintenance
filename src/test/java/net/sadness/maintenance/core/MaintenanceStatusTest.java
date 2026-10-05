package net.sadness.maintenance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MaintenanceStatusTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");

    @Test
    void remainingIsEmptyWhenInactive() {
        assertEquals(Optional.empty(), MaintenanceStatus.inactive().remaining(NOW));
    }

    @Test
    void remainingIsEmptyWhenPermanent() {
        assertEquals(Optional.empty(), MaintenanceStatus.permanent().remaining(NOW));
    }

    @Test
    void remainingCountsDownToEnd() {
        assertEquals(Optional.of(Duration.ofSeconds(90)), MaintenanceStatus.until(NOW.plusSeconds(90)).remaining(NOW));
    }

    @Test
    void remainingNeverGoesNegative() {
        assertEquals(Optional.of(Duration.ZERO), MaintenanceStatus.until(NOW).remaining(NOW.plusSeconds(10)));
    }

    @Test
    void expiryBoundaryIsInclusive() {
        MaintenanceStatus status = MaintenanceStatus.until(NOW);
        assertFalse(status.isExpiredAt(NOW.minusMillis(1)));
        assertTrue(status.isExpiredAt(NOW));
    }

    @Test
    void untilRejectsNull() {
        assertThrows(NullPointerException.class, () -> MaintenanceStatus.until(null));
    }

    @Test
    void equalityComparesActivityAndEnd() {
        assertEquals(MaintenanceStatus.until(NOW), MaintenanceStatus.until(NOW));
        assertEquals(MaintenanceStatus.until(NOW).hashCode(), MaintenanceStatus.until(NOW).hashCode());
        assertNotEquals(MaintenanceStatus.until(NOW), MaintenanceStatus.until(NOW.plusSeconds(1)));
        assertNotEquals(MaintenanceStatus.permanent(), MaintenanceStatus.inactive());
        assertNotEquals(MaintenanceStatus.permanent(), MaintenanceStatus.until(NOW));
    }
}
