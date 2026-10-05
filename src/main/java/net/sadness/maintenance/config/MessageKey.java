package net.sadness.maintenance.config;

public enum MessageKey {
    PREFIX("prefix", "<gray>[<gold>Maintenance</gold>]</gray>"),
    ENABLED("enabled", "<prefix> <yellow>Maintenance mode is now <green>enabled</green>.</yellow>"),
    ENABLED_TIMED("enabled-timed",
            "<prefix> <yellow>Maintenance mode is now <green>enabled</green> for <duration>.</yellow>"),
    DISABLED("disabled", "<prefix> <yellow>Maintenance mode is now <red>disabled</red>.</yellow>"),
    ALREADY_ENABLED("already-enabled", "<prefix> <red>Maintenance mode is already enabled.</red>"),
    ALREADY_DISABLED("already-disabled", "<prefix> <red>Maintenance mode is already disabled.</red>"),
    STATUS_OFF("status-off", "<prefix> <yellow>Maintenance mode is <red>off</red>.</yellow>"),
    STATUS_ON("status-on", "<prefix> <yellow>Maintenance mode is <green>on</green>.</yellow>"),
    STATUS_ON_TIMED("status-on-timed",
            "<prefix> <yellow>Maintenance mode is <green>on</green> and ends in <remaining>.</yellow>"),
    KICK("kick",
            "<red><bold>Server maintenance</bold></red>\n<gray>The server entered maintenance mode. Please come back later.</gray>"),
    LOGIN_DENIED("login-denied",
            "<red><bold>Server under maintenance</bold></red>\n<gray>Please try again later.</gray>"),
    COUNTDOWN("countdown", "<prefix> <yellow>Maintenance ends in <remaining>.</yellow>"),
    EXPIRED("expired", "<prefix> <green>Maintenance is over. The server is open again.</green>"),
    NO_PERMISSION("no-permission", "<red>You do not have permission to do that.</red>"),
    USAGE("usage",
            "<prefix> <yellow>Usage:</yellow>\n<gray>/maintenance on [duration]\n/maintenance off\n/maintenance status\n/maintenance reload\n/maintenance whitelist (add|remove|list) [player]</gray>"),
    INVALID_DURATION("invalid-duration", "<prefix> <red>Invalid duration '<input>': <reason>.</red>"),
    RELOAD_SUCCESS("reload-success", "<prefix> <green>Configuration reloaded.</green>"),
    RELOAD_WARNINGS("reload-warnings",
            "<prefix> <green>Configuration reloaded</green> <yellow>with <count> warning(s). See the console.</yellow>"),
    RELOAD_FAILED("reload-failed",
            "<prefix> <red>Reload failed, the previous configuration is still active: <error></red>"),
    WHITELIST_ADDED("whitelist-added", "<prefix> <green><player> was added to the maintenance whitelist.</green>"),
    WHITELIST_ALREADY("whitelist-already", "<prefix> <red><player> is already on the maintenance whitelist.</red>"),
    WHITELIST_REMOVED("whitelist-removed",
            "<prefix> <yellow><player> was removed from the maintenance whitelist.</yellow>"),
    WHITELIST_NOT_LISTED("whitelist-not-listed",
            "<prefix> <red><player> is not on the maintenance whitelist.</red>"),
    PLAYER_UNKNOWN("player-unknown",
            "<prefix> <red>Unknown player '<player>'. Use an online player, a player who joined before, or a UUID.</red>"),
    WHITELIST_HEADER("whitelist-header", "<prefix> <yellow>Maintenance whitelist (<count>):</yellow>"),
    WHITELIST_ENTRY("whitelist-entry", "<gray> - <white><player></white></gray>"),
    WHITELIST_EMPTY("whitelist-empty", "<prefix> <yellow>The maintenance whitelist is empty.</yellow>");

    private final String path;
    private final String defaultTemplate;

    MessageKey(String path, String defaultTemplate) {
        this.path = path;
        this.defaultTemplate = defaultTemplate;
    }

    public String path() {
        return path;
    }

    public String defaultTemplate() {
        return defaultTemplate;
    }

    public boolean allowsBlank() {
        return this == PREFIX;
    }
}
