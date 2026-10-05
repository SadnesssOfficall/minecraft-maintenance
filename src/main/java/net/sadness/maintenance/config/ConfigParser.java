package net.sadness.maintenance.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sadness.maintenance.core.DurationParser;
import net.sadness.maintenance.core.InvalidDurationException;
import net.sadness.maintenance.io.YamlCodec;
import net.sadness.maintenance.io.YamlFormatException;

public final class ConfigParser {

    static final String KEY_PERSIST_STATE = "persist-state";
    static final String KEY_BYPASS_OPERATORS = "bypass-operators";
    static final String KEY_MOTD = "motd";
    static final String KEY_MOTD_ENABLED = "enabled";
    static final String KEY_MOTD_TEXT = "text";
    static final String KEY_COUNTDOWN = "countdown-announcements";
    static final String KEY_MESSAGES = "messages";

    private static final Set<String> TOP_LEVEL_KEYS = Set.of(
            KEY_PERSIST_STATE, KEY_BYPASS_OPERATORS, KEY_MOTD, KEY_COUNTDOWN, KEY_MESSAGES);

    private final List<String> warnings = new ArrayList<>();

    private ConfigParser() {
    }

    public static ConfigLoad parse(String yaml) throws ConfigException {
        Map<String, Object> root;
        try {
            root = YamlCodec.parseMap(yaml);
        } catch (YamlFormatException e) {
            throw new ConfigException(e.getMessage(), e);
        }
        return new ConfigParser().build(root);
    }

    private ConfigLoad build(Map<String, Object> root) {
        root.keySet().stream()
                .filter(key -> !TOP_LEVEL_KEYS.contains(key))
                .forEach(key -> warnings.add("unknown option '" + key + "' ignored"));

        boolean persistState = readBoolean(root, KEY_PERSIST_STATE, MaintenanceConfig.DEFAULT_PERSIST_STATE);
        boolean bypassOperators = readBoolean(root, KEY_BYPASS_OPERATORS, MaintenanceConfig.DEFAULT_BYPASS_OPERATORS);

        Map<String, Object> motd = readSection(root, KEY_MOTD);
        boolean motdEnabled = readBoolean(motd, KEY_MOTD + "." + KEY_MOTD_ENABLED, KEY_MOTD_ENABLED,
                MaintenanceConfig.DEFAULT_MOTD_ENABLED);
        String motdTemplate = readText(motd, KEY_MOTD + "." + KEY_MOTD_TEXT, KEY_MOTD_TEXT,
                MaintenanceConfig.DEFAULT_MOTD_TEMPLATE, false);

        List<Duration> announcements = readAnnouncements(root);
        Messages messages = readMessages(readSection(root, KEY_MESSAGES));

        MaintenanceConfig config = new MaintenanceConfig(
                persistState, bypassOperators, motdEnabled, motdTemplate, announcements, messages);
        return new ConfigLoad(config, warnings);
    }

    private boolean readBoolean(Map<String, Object> section, String key, boolean fallback) {
        return readBoolean(section, key, key, fallback);
    }

    private boolean readBoolean(Map<String, Object> section, String path, String key, boolean fallback) {
        Object value = section.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        warnings.add("'" + path + "' must be true or false; using " + fallback);
        return fallback;
    }

    private String readText(Map<String, Object> section, String path, String key, String fallback, boolean allowBlank) {
        Object value = section.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof String text && (allowBlank || !text.isBlank())) {
            return text;
        }
        warnings.add("'" + path + "' must be a non-blank string; using the default");
        return fallback;
    }

    private Map<String, Object> readSection(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> section = new LinkedHashMap<>();
            raw.forEach((k, v) -> section.put(String.valueOf(k), v));
            return section;
        }
        warnings.add("'" + key + "' must be a section; using defaults");
        return Map.of();
    }

    private List<Duration> readAnnouncements(Map<String, Object> root) {
        Object value = root.get(KEY_COUNTDOWN);
        if (value == null) {
            return MaintenanceConfig.DEFAULT_COUNTDOWN_ANNOUNCEMENTS;
        }
        if (!(value instanceof List<?> items)) {
            warnings.add("'" + KEY_COUNTDOWN + "' must be a list; using the defaults");
            return MaintenanceConfig.DEFAULT_COUNTDOWN_ANNOUNCEMENTS;
        }
        List<Duration> durations = new ArrayList<>();
        for (Object item : items) {
            try {
                durations.add(DurationParser.parse(String.valueOf(item)));
            } catch (InvalidDurationException e) {
                warnings.add("'" + KEY_COUNTDOWN + "' entry ignored: " + e.getMessage());
            }
        }
        return durations;
    }

    private Messages readMessages(Map<String, Object> section) {
        Map<MessageKey, String> overrides = new EnumMap<>(MessageKey.class);
        Set<String> known = new HashSet<>();
        for (MessageKey key : MessageKey.values()) {
            known.add(key.path());
            String path = KEY_MESSAGES + "." + key.path();
            String template = readText(section, path, key.path(), null, key.allowsBlank());
            if (template != null) {
                overrides.put(key, template);
            }
        }
        section.keySet().stream()
                .filter(name -> !known.contains(name))
                .forEach(name -> warnings.add("unknown message '" + KEY_MESSAGES + "." + name + "' ignored"));
        return new Messages(overrides);
    }
}
