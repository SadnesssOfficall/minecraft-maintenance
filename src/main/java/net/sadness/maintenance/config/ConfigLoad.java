package net.sadness.maintenance.config;

import java.util.List;

public record ConfigLoad(MaintenanceConfig config, List<String> warnings) {

    public ConfigLoad {
        warnings = List.copyOf(warnings);
    }
}
