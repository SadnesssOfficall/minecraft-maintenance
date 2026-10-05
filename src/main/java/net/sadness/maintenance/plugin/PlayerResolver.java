package net.sadness.maintenance.plugin;

import java.util.Optional;
import java.util.UUID;
import net.sadness.maintenance.core.UuidInput;
import net.sadness.maintenance.core.WhitelistEntry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class PlayerResolver {

    public Optional<WhitelistEntry> resolve(String input) {
        Optional<UUID> uuid = UuidInput.parse(input);
        if (uuid.isPresent()) {
            return Optional.of(new WhitelistEntry(uuid.get(), displayName(uuid.get())));
        }
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return Optional.of(new WhitelistEntry(online.getUniqueId(), online.getName()));
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(input);
        if (cached != null && cached.getName() != null) {
            return Optional.of(new WhitelistEntry(cached.getUniqueId(), cached.getName()));
        }
        return Optional.empty();
    }

    private static String displayName(UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            return online.getName();
        }
        String known = Bukkit.getOfflinePlayer(id).getName();
        return known != null ? known : id.toString();
    }
}
