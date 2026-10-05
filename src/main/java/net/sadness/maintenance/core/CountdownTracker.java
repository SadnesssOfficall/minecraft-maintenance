package net.sadness.maintenance.core;

import java.time.Duration;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

public final class CountdownTracker {

    private final NavigableSet<Duration> pending = new TreeSet<>();

    public CountdownTracker(Collection<Duration> thresholds, Duration initialRemaining) {
        for (Duration threshold : thresholds) {
            if (threshold.compareTo(initialRemaining) < 0) {
                pending.add(threshold);
            }
        }
    }

    public Optional<Duration> poll(Duration remaining) {
        NavigableSet<Duration> due = pending.tailSet(remaining, true);
        if (due.isEmpty()) {
            return Optional.empty();
        }
        Duration announce = due.first();
        due.clear();
        return Optional.of(announce);
    }
}
