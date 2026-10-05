package net.sadness.maintenance.config;

import java.util.EnumMap;
import java.util.Map;

public final class Messages {

    private final Map<MessageKey, String> overrides;

    public Messages(Map<MessageKey, String> overrides) {
        this.overrides = overrides.isEmpty() ? new EnumMap<>(MessageKey.class) : new EnumMap<>(overrides);
    }

    public static Messages defaults() {
        return new Messages(Map.of());
    }

    public String template(MessageKey key) {
        return overrides.getOrDefault(key, key.defaultTemplate());
    }
}
