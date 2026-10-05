package net.sadness.maintenance.config;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

public record MaintenanceConfig(
        boolean persistState,
        boolean bypassOperators,
        boolean motdEnabled,
        String motdTemplate,
        List<Duration> countdownAnnouncements,
        Messages messages) {

    public static final boolean DEFAULT_PERSIST_STATE = true;
    public static final boolean DEFAULT_BYPASS_OPERATORS = true;
    public static final boolean DEFAULT_MOTD_ENABLED = true;
    public static final String DEFAULT_MOTD_TEMPLATE =
            "<red><bold>Server under maintenance</bold></red>\n<gray>We will be back soon.</gray>";
    public static final List<Duration> DEFAULT_COUNTDOWN_ANNOUNCEMENTS = List.of(
            Duration.ofMinutes(10),
            Duration.ofMinutes(5),
            Duration.ofMinutes(1),
            Duration.ofSeconds(30),
            Duration.ofSeconds(10),
            Duration.ofSeconds(5),
            Duration.ofSeconds(3),
            Duration.ofSeconds(2),
            Duration.ofSeconds(1));

    public MaintenanceConfig {
        countdownAnnouncements = countdownAnnouncements.stream()
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    public static MaintenanceConfig defaults() {
        return new MaintenanceConfig(
                DEFAULT_PERSIST_STATE,
                DEFAULT_BYPASS_OPERATORS,
                DEFAULT_MOTD_ENABLED,
                DEFAULT_MOTD_TEMPLATE,
                DEFAULT_COUNTDOWN_ANNOUNCEMENTS,
                Messages.defaults());
    }
}
