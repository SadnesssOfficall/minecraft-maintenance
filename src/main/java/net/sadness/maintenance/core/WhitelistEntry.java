package net.sadness.maintenance.core;

import java.util.Objects;
import java.util.UUID;

public record WhitelistEntry(UUID id, String name) {

    public WhitelistEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
