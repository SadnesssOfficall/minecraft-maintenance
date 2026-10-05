package net.sadness.maintenance.plugin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.sadness.maintenance.config.MessageKey;
import net.sadness.maintenance.core.CountdownTracker;
import net.sadness.maintenance.core.DurationFormatter;
import net.sadness.maintenance.core.MaintenanceMachine;
import net.sadness.maintenance.core.MaintenanceStatus;
import net.sadness.maintenance.core.MaintenanceWhitelist;
import net.sadness.maintenance.core.PlayerAccess;
import net.sadness.maintenance.core.Transition;
import net.sadness.maintenance.io.Persistence;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class MaintenanceService {

    private static final long TICKS_PER_SECOND = 20L;

    private final Plugin plugin;
    private final MaintenanceMachine machine = new MaintenanceMachine();
    private final MaintenanceWhitelist whitelist;
    private final Persistence persistence;
    private final Clock clock;

    private volatile Settings settings;
    private BukkitTask countdownTask;
    private CountdownTracker tracker;

    public MaintenanceService(
            Plugin plugin, MaintenanceWhitelist whitelist, Persistence persistence, Clock clock, Settings settings) {
        this.plugin = plugin;
        this.whitelist = whitelist;
        this.persistence = persistence;
        this.clock = clock;
        this.settings = settings;
    }

    public Settings settings() {
        return settings;
    }

    public MaintenanceStatus status() {
        return machine.status();
    }

    public Optional<Duration> remaining() {
        return machine.status().remaining(clock.instant());
    }

    public MaintenanceWhitelist whitelist() {
        return whitelist;
    }

    public Transition enable() {
        return afterEnable(machine.enable());
    }

    public Transition enable(Duration duration) {
        return afterEnable(machine.enableUntil(clock.instant().plus(duration)));
    }

    public Transition disable() {
        Transition transition = machine.disable();
        if (transition.changed()) {
            cancelCountdown();
            persistState();
            plugin.getLogger().info("Maintenance mode disabled");
        }
        return transition;
    }

    public void restore(MaintenanceStatus restored) {
        Instant now = clock.instant();
        machine.restore(restored, now);
        if (restored.isExpiredAt(now)) {
            plugin.getLogger().info("Timed maintenance ended while the server was offline");
            persistState();
        }
        startCountdown();
    }

    public boolean mayJoin(Player player) {
        PlayerAccess access = new PlayerAccess(
                whitelist.contains(player.getUniqueId()),
                player.isOp(),
                player.hasPermission(Permissions.BYPASS));
        return settings.policy().allows(machine.status().isActive(), access);
    }

    public void applySettings(Settings updated) {
        settings = updated;
        persistState();
        startCountdown();
    }

    public void whitelistChanged() {
        persistence.saveWhitelist(whitelist.entries());
        kickDisallowed();
    }

    public void shutdown() {
        cancelCountdown();
    }

    private Transition afterEnable(Transition transition) {
        if (transition.changed()) {
            persistState();
            kickDisallowed();
            startCountdown();
            plugin.getLogger().info("Maintenance mode enabled");
        }
        return transition;
    }

    private void persistState() {
        if (settings.config().persistState()) {
            persistence.saveState(machine.status());
        }
    }

    private void kickDisallowed() {
        if (!machine.status().isActive()) {
            return;
        }
        Component message = settings.renderer().render(MessageKey.KICK);
        for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
            if (!mayJoin(player)) {
                player.kick(message);
            }
        }
    }

    private void startCountdown() {
        cancelCountdown();
        Optional<Duration> remaining = machine.status().remaining(clock.instant());
        if (remaining.isEmpty()) {
            return;
        }
        tracker = new CountdownTracker(settings.config().countdownAnnouncements(), remaining.get());
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICKS_PER_SECOND, TICKS_PER_SECOND);
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        tracker = null;
    }

    private void tick() {
        Instant now = clock.instant();
        if (machine.expire(now)) {
            cancelCountdown();
            persistState();
            plugin.getLogger().info("Timed maintenance ended");
            Bukkit.broadcast(settings.renderer().render(MessageKey.EXPIRED));
            return;
        }
        Optional<Duration> remaining = machine.status().remaining(now);
        if (remaining.isEmpty() || tracker == null) {
            cancelCountdown();
            return;
        }
        tracker.poll(remaining.get()).ifPresent(threshold -> Bukkit.broadcast(
                settings.renderer().render(
                        MessageKey.COUNTDOWN, Map.of("remaining", DurationFormatter.format(threshold)))));
    }
}
