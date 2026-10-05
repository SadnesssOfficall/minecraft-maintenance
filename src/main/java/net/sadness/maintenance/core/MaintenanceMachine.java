package net.sadness.maintenance.core;

import java.time.Instant;
import java.util.Objects;

public final class MaintenanceMachine {

    private volatile MaintenanceStatus status = MaintenanceStatus.inactive();

    public MaintenanceStatus status() {
        return status;
    }

    public synchronized Transition enable() {
        return activate(MaintenanceStatus.permanent());
    }

    public synchronized Transition enableUntil(Instant endsAt) {
        return activate(MaintenanceStatus.until(endsAt));
    }

    public synchronized Transition disable() {
        if (!status.isActive()) {
            return Transition.ALREADY_INACTIVE;
        }
        status = MaintenanceStatus.inactive();
        return Transition.DISABLED;
    }

    public synchronized boolean expire(Instant now) {
        if (!status.isExpiredAt(now)) {
            return false;
        }
        status = MaintenanceStatus.inactive();
        return true;
    }

    public synchronized void restore(MaintenanceStatus restored, Instant now) {
        Objects.requireNonNull(restored, "restored");
        status = restored.isExpiredAt(now) ? MaintenanceStatus.inactive() : restored;
    }

    private Transition activate(MaintenanceStatus target) {
        if (status.isActive()) {
            return Transition.ALREADY_ACTIVE;
        }
        status = target;
        return Transition.ENABLED;
    }
}
