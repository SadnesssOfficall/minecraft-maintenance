package net.sadness.maintenance.plugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.sadness.maintenance.config.MessageKey;
import net.sadness.maintenance.core.MaintenanceWhitelist;
import net.sadness.maintenance.core.UuidInput;
import net.sadness.maintenance.core.WhitelistEntry;
import net.sadness.maintenance.message.MessageRenderer;
import org.bukkit.command.CommandSender;

public final class WhitelistCommand {

    static final String ADD = "add";
    static final String REMOVE = "remove";
    static final String LIST = "list";

    private final MaintenanceService service;
    private final MaintenanceWhitelist whitelist;
    private final PlayerResolver resolver;

    public WhitelistCommand(MaintenanceService service, PlayerResolver resolver) {
        this.service = service;
        this.whitelist = service.whitelist();
        this.resolver = resolver;
    }

    public void execute(CommandSender sender, List<String> args) {
        MessageRenderer renderer = service.settings().renderer();
        if (args.isEmpty()) {
            sender.sendMessage(renderer.render(MessageKey.USAGE));
            return;
        }
        switch (args.get(0).toLowerCase(Locale.ROOT)) {
            case LIST -> list(sender, renderer);
            case ADD -> {
                if (args.size() == 2) {
                    add(sender, renderer, args.get(1));
                } else {
                    sender.sendMessage(renderer.render(MessageKey.USAGE));
                }
            }
            case REMOVE -> {
                if (args.size() == 2) {
                    remove(sender, renderer, args.get(1));
                } else {
                    sender.sendMessage(renderer.render(MessageKey.USAGE));
                }
            }
            default -> sender.sendMessage(renderer.render(MessageKey.USAGE));
        }
    }

    private void list(CommandSender sender, MessageRenderer renderer) {
        List<WhitelistEntry> entries = whitelist.entries();
        if (entries.isEmpty()) {
            sender.sendMessage(renderer.render(MessageKey.WHITELIST_EMPTY));
            return;
        }
        sender.sendMessage(renderer.render(MessageKey.WHITELIST_HEADER, Map.of("count", String.valueOf(entries.size()))));
        entries.forEach(entry ->
                sender.sendMessage(renderer.render(MessageKey.WHITELIST_ENTRY, Map.of("player", entry.name()))));
    }

    private void add(CommandSender sender, MessageRenderer renderer, String input) {
        Optional<WhitelistEntry> entry = resolver.resolve(input);
        if (entry.isEmpty()) {
            sender.sendMessage(renderer.render(MessageKey.PLAYER_UNKNOWN, Map.of("player", input)));
            return;
        }
        Map<String, String> values = Map.of("player", entry.get().name());
        if (!whitelist.add(entry.get())) {
            sender.sendMessage(renderer.render(MessageKey.WHITELIST_ALREADY, values));
            return;
        }
        service.whitelistChanged();
        sender.sendMessage(renderer.render(MessageKey.WHITELIST_ADDED, values));
    }

    private void remove(CommandSender sender, MessageRenderer renderer, String input) {
        Optional<WhitelistEntry> removed = UuidInput.parse(input)
                .flatMap(whitelist::remove)
                .or(() -> whitelist.findByName(input).flatMap(found -> whitelist.remove(found.id())));
        if (removed.isEmpty()) {
            sender.sendMessage(renderer.render(MessageKey.WHITELIST_NOT_LISTED, Map.of("player", input)));
            return;
        }
        service.whitelistChanged();
        sender.sendMessage(renderer.render(MessageKey.WHITELIST_REMOVED, Map.of("player", removed.get().name())));
    }
}
