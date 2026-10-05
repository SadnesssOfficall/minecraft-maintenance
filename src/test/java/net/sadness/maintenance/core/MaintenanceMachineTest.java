package net.sadness.maintenance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MaintenanceMachineTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");

    private MaintenanceMachine machine;

    @BeforeEach
    void setUp() {
        machine = new MaintenanceMachine();
    }

    @Test
    void startsInactive() {
        assertFalse(machine.status().isActive());
    }

    @Test
    void enableActivatesPermanently() {
        assertEquals(Transition.ENABLED, machine.enable());
        assertTrue(machine.status().isActive());
        assertEquals(Optional.empty(), machine.status().endsAt());
    }

    @Test
    void enableTwiceReportsAlreadyActive() {
        machine.enable();
        assertEquals(Transition.ALREADY_ACTIVE, machine.enable());
    }

    @Test
    void enableUntilStoresEndTime() {
        Instant end = NOW.plusSeconds(60);
        assertEquals(Transition.ENABLED, machine.enableUntil(end));
        assertEquals(Optional.of(end), machine.status().endsAt());
    }

    @Test
    void enableUntilRejectsNull() {
        assertThrows(NullPointerException.class, () -> machine.enableUntil(null));
    }

    @Test
    void enablingActiveMachineKeepsExistingSchedule() {
        Instant end = NOW.plusSeconds(60);
        machine.enableUntil(end);
        assertEquals(Transition.ALREADY_ACTIVE, machine.enableUntil(NOW.plusSeconds(600)));
        assertEquals(Optional.of(end), machine.status().endsAt());
    }

    @Test
    void disableDeactivates() {
        machine.enable();
        assertEquals(Transition.DISABLED, machine.disable());
        assertFalse(machine.status().isActive());
    }

    @Test
    void disableWhenInactiveReportsAlreadyInactive() {
        assertEquals(Transition.ALREADY_INACTIVE, machine.disable());
    }

    @Test
    void disableClearsSchedule() {
        machine.enableUntil(NOW.plusSeconds(60));
        machine.disable();
        assertEquals(Optional.empty(), machine.status().endsAt());
    }

    @Test
    void canReenableAfterDisable() {
        machine.enable();
        machine.disable();
        assertEquals(Transition.ENABLED, machine.enable());
    }

    @Test
    void expireDoesNothingBeforeEnd() {
        machine.enableUntil(NOW.plusSeconds(60));
        assertFalse(machine.expire(NOW.plusSeconds(59)));
        assertTrue(machine.status().isActive());
    }

    @Test
    void expireDeactivatesExactlyAtEnd() {
        machine.enableUntil(NOW.plusSeconds(60));
        assertTrue(machine.expire(NOW.plusSeconds(60)));
        assertFalse(machine.status().isActive());
    }

    @Test
    void expireDeactivatesAfterEnd() {
        machine.enableUntil(NOW.plusSeconds(60));
        assertTrue(machine.expire(NOW.plusSeconds(3600)));
    }

    @Test
    void expireNeverAffectsPermanentMaintenance() {
        machine.enable();
        assertFalse(machine.expire(NOW.plus(Duration.ofDays(3650))));
        assertTrue(machine.status().isActive());
    }

    @Test
    void expireOnInactiveMachineReturnsFalse() {
        assertFalse(machine.expire(NOW));
    }

    @Test
    void expireOnlyFiresOnce() {
        machine.enableUntil(NOW);
        assertTrue(machine.expire(NOW));
        assertFalse(machine.expire(NOW));
    }

    @Test
    void restoreAppliesActiveStatus() {
        MaintenanceStatus restored = MaintenanceStatus.until(NOW.plusSeconds(30));
        machine.restore(restored, NOW);
        assertEquals(restored, machine.status());
    }

    @Test
    void restoreDropsAlreadyExpiredStatus() {
        machine.restore(MaintenanceStatus.until(NOW.minusSeconds(1)), NOW);
        assertFalse(machine.status().isActive());
    }

    @Test
    void restoreKeepsPermanentStatus() {
        machine.restore(MaintenanceStatus.permanent(), NOW);
        assertTrue(machine.status().isActive());
    }

    @Test
    void restoreRejectsNull() {
        assertThrows(NullPointerException.class, () -> machine.restore(null, NOW));
    }

    @Test
    void transitionChangedFlag() {
        assertTrue(Transition.ENABLED.changed());
        assertTrue(Transition.DISABLED.changed());
        assertFalse(Transition.ALREADY_ACTIVE.changed());
        assertFalse(Transition.ALREADY_INACTIVE.changed());
    }
}
