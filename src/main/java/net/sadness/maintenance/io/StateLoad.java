package net.sadness.maintenance.io;

import java.util.List;
import net.sadness.maintenance.core.MaintenanceStatus;

public record StateLoad(MaintenanceStatus status, List<String> warnings) {

    public StateLoad {
        warnings = List.copyOf(warnings);
    }
}
