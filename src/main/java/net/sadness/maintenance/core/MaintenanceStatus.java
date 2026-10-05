package net.sadness.maintenance.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class MaintenanceStatus {

    private static final MaintenanceStatus INACTIVE = new MaintenanceStatus(false, null);
    private static final MaintenanceStatus PERMANENT = new MaintenanceStatus(true, null);

    private final boolean active;
    private final Instant endsAt;

    private MaintenanceStatus(boolean active, Instant endsAt) {
        this.active = active;
        this.endsAt = endsAt;
    }

    public static MaintenanceStatus inactive() {
        return INACTIVE;
    }

    public static MaintenanceStatus permanent() {
        return PERMANENT;
    }

    public static MaintenanceStatus until(Instant endsAt) {
        return new MaintenanceStatus(true, Objects.requireNonNull(endsAt, "endsAt"));
    }

    public boolean isActive() {
        return active;
    }

    public Optional<Instant> endsAt() {
        return Optional.ofNullable(endsAt);
    }

    public boolean isExpiredAt(Instant now) {
        return active && endsAt != null && !now.isBefore(endsAt);
    }

    public Optional<Duration> remaining(Instant now) {
        if (!active || endsAt == null) {
            return Optional.empty();
        }
        Duration remaining = Duration.between(now, endsAt);
        return Optional.of(remaining.isNegative() ? Duration.ZERO : remaining);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MaintenanceStatus that)) {
            return false;
        }
        return active == that.active && Objects.equals(endsAt, that.endsAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, endsAt);
    }

    @Override
    public String toString() {
        if (!active) {
            return "MaintenanceStatus[inactive]";
        }
        return endsAt == null ? "MaintenanceStatus[permanent]" : "MaintenanceStatus[until=" + endsAt + "]";
    }
}
