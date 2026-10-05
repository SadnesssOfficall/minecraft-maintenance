package net.sadness.maintenance.plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.sadness.maintenance.config.MessageKey;
import net.sadness.maintenance.core.DurationFormatter;
import net.sadness.maintenance.core.DurationParser;
import net.sadness.maintenance.core.InvalidDurationException;
import net.sadness.maintenance.core.MaintenanceStatus;
import net.sadness.maintenance.core.Transition;
import net.sadness.maintenance.core.WhitelistEntry;
import net.sadness.maintenance.message.MessageRenderer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class MaintenanceCommand implements TabExecutor {

    private static final String ON = "on";
    private static final String OFF = "off";
    private static final String STATUS = "status";
    private static final String RELOAD = "reload";
    private static final String WHITELIST = "whitelist";
    private static final List<String> DURATION_SUGGESTIONS = List.of("15m", "30m", "1h", "2h");
    private static final List<String> WHITELIST_ACTIONS = List.of(
            WhitelistCommand.ADD, WhitelistCommand.REMOVE, WhitelistCommand.LIST);

    private final MaintenanceService service;
    private final WhitelistCommand whitelistCommand;
    private final ConfigReloader reloader;

    public MaintenanceCommand(MaintenanceService service, WhitelistCommand whitelistCommand, ConfigReloader reloader) {
        this.service = service;
        this.whitelistCommand = whitelistCommand;
        this.reloader = reloader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageRenderer renderer = service.settings().renderer();
        if (args.length == 0) {
            sender.sendMessage(renderer.render(MessageKey.USAGE));
            return true;
        }
        List<String> rest = List.of(args).subList(1, args.length);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case ON -> guarded(sender, renderer, Permissions.MANAGE, () -> enable(sender, renderer, rest));
            case OFF -> guarded(sender, renderer, Permissions.MANAGE, () -> disable(sender, renderer));
            case STATUS -> guarded(sender, renderer, Permissions.MANAGE, () -> status(sender, renderer));
            case WHITELIST -> guarded(sender, renderer, Permissions.MANAGE, () -> whitelistCommand.execute(sender, rest));
            case RELOAD -> guarded(sender, renderer, Permissions.RELOAD, () -> reloader.reload(sender));
            default -> sender.sendMessage(renderer.render(MessageKey.USAGE));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> candidates = candidates(sender, args);
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private List<String> candidates(CommandSender sender, String[] args) {
        boolean canManage = sender.hasPermission(Permissions.MANAGE);
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            if (canManage) {
                subcommands.addAll(List.of(ON, OFF, STATUS, WHITELIST));
            }
            if (sender.hasPermission(Permissions.RELOAD)) {
                subcommands.add(RELOAD);
            }
            return subcommands;
        }
        if (!canManage) {
            return List.of();
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (subcommand.equals(ON)) {
                return DURATION_SUGGESTIONS;
            }
            return subcommand.equals(WHITELIST) ? WHITELIST_ACTIONS : List.of();
        }
        if (args.length == 3 && subcommand.equals(WHITELIST)) {
            return whitelistTargets(args[1].toLowerCase(Locale.ROOT));
        }
        return List.of();
    }

    private List<String> whitelistTargets(String action) {
        if (action.equals(WhitelistCommand.ADD)) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(player -> !service.whitelist().contains(player.getUniqueId()))
                    .map(Player::getName)
                    .toList();
        }
        if (action.equals(WhitelistCommand.REMOVE)) {
            return service.whitelist().entries().stream().map(WhitelistEntry::name).toList();
        }
        return List.of();
    }

    private void guarded(CommandSender sender, MessageRenderer renderer, String permission, Runnable action) {
        if (sender.hasPermission(permission)) {
            action.run();
        } else {
            sender.sendMessage(renderer.render(MessageKey.NO_PERMISSION));
        }
    }

    private void enable(CommandSender sender, MessageRenderer renderer, List<String> args) {
        if (args.size() > 1) {
            sender.sendMessage(renderer.render(MessageKey.USAGE));
            return;
        }
        if (args.isEmpty()) {
            report(sender, renderer, service.enable(), MessageKey.ENABLED, Map.of());
            return;
        }
        Duration duration;
        try {
            duration = DurationParser.parse(args.get(0));
        } catch (InvalidDurationException e) {
            sender.sendMessage(renderer.render(MessageKey.INVALID_DURATION,
                    Map.of("input", args.get(0), "reason", e.reason().description())));
            return;
        }
        report(sender, renderer, service.enable(duration), MessageKey.ENABLED_TIMED,
                Map.of("duration", DurationFormatter.format(duration)));
    }

    private void disable(CommandSender sender, MessageRenderer renderer) {
        Transition transition = service.disable();
        sender.sendMessage(renderer.render(
                transition == Transition.DISABLED ? MessageKey.DISABLED : MessageKey.ALREADY_DISABLED));
    }

    private void report(
            CommandSender sender, MessageRenderer renderer, Transition transition, MessageKey success,
            Map<String, String> values) {
        sender.sendMessage(renderer.render(
                transition == Transition.ENABLED ? success : MessageKey.ALREADY_ENABLED, values));
    }

    private void status(CommandSender sender, MessageRenderer renderer) {
        MaintenanceStatus status = service.status();
        if (!status.isActive()) {
            sender.sendMessage(renderer.render(MessageKey.STATUS_OFF));
            return;
        }
        service.remaining().ifPresentOrElse(
                remaining -> sender.sendMessage(renderer.render(
                        MessageKey.STATUS_ON_TIMED, Map.of("remaining", DurationFormatter.format(remaining)))),
                () -> sender.sendMessage(renderer.render(MessageKey.STATUS_ON)));
    }
}
