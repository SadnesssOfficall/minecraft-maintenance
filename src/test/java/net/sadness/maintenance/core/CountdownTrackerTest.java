package net.sadness.maintenance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CountdownTrackerTest {

    private static final List<Duration> THRESHOLDS = List.of(
            Duration.ofMinutes(10), Duration.ofMinutes(1), Duration.ofSeconds(10));

    @Test
    void announcesNothingWhileAboveAllThresholds() {
        CountdownTracker tracker = new CountdownTracker(THRESHOLDS, Duration.ofMinutes(30));
        assertEquals(Optional.empty(), tracker.poll(Duration.ofMinutes(29)));
    }

    @Test
    void announcesThresholdWhenReached() {
        CountdownTracker tracker = new CountdownTracker(THRESHOLDS, Duration.ofMinutes(30));
        assertEquals(Optional.of(Duration.ofMinutes(10)), tracker.poll(Duration.ofMinutes(10)));
    }

    @Test
    void announcesEachThresholdOnlyOnce() {
        CountdownTracker tracker = new CountdownTracker(THRESHOLDS, Duration.ofMinutes(30));
        tracker.poll(Duration.ofMinutes(10));
        assertEquals(Optional.empty(), tracker.poll(Duration.ofMinutes(9)));
        assertEquals(Optional.empty(), tracker.poll(Duration.ofMinutes(10)));
    }

    @Test
    void announcesNextThresholdLater() {
        CountdownTracker tracker = new CountdownTracker(THRESHOLDS, Duration.ofMinutes(30));
        tracker.poll(Duration.ofMinutes(10));
        assertEquals(Optional.of(Duration.ofMinutes(1)), tracker.poll(Duration.ofSeconds(59)));
    }

    @Test
    void skippedThresholdsCollapseIntoClosestOne() {
        CountdownTracker tracker = new CountdownTracker(THRESHOLDS, Duration.ofMinutes(30));
        assertEquals(Optional.of(Duration.ofMinutes(1)), tracker.poll(Duration.ofSeconds(50)));
        assertEquals(Optional.empty(), tracker.poll(Duration.ofSeconds(49)));
        assertEquals(Optional.of(Duration.ofSeconds(10)), tracker.poll(Duration.ofSeconds(5)));
    }

    @Test
    void thresholdsAtOrAboveInitialRemainingAreNeverAnnounced() {
        CountdownTracker tracker = new CountdownTracker(THRESHOLDS, Duration.ofMinutes(10));
        assertEquals(Optional.empty(), tracker.poll(Duration.ofMinutes(10)));
        assertEquals(Optional.of(Duration.ofMinutes(1)), tracker.poll(Duration.ofMinutes(1)));
    }

    @Test
    void emptyThresholdsNeverAnnounce() {
        CountdownTracker tracker = new CountdownTracker(List.of(), Duration.ofMinutes(5));
        assertEquals(Optional.empty(), tracker.poll(Duration.ZERO));
    }

    @Test
    void zeroRemainingFlushesRemainingThreshold() {
        CountdownTracker tracker = new CountdownTracker(THRESHOLDS, Duration.ofSeconds(30));
        assertEquals(Optional.of(Duration.ofSeconds(10)), tracker.poll(Duration.ZERO));
    }

    @Test
    void duplicateThresholdsAreAnnouncedOnce() {
        CountdownTracker tracker = new CountdownTracker(
                List.of(Duration.ofSeconds(10), Duration.ofSeconds(10)), Duration.ofMinutes(1));
        assertEquals(Optional.of(Duration.ofSeconds(10)), tracker.poll(Duration.ofSeconds(10)));
        assertEquals(Optional.empty(), tracker.poll(Duration.ofSeconds(9)));
    }
}
