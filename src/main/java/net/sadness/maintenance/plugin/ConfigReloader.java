package net.sadness.maintenance.plugin;

import java.io.IOException;
import java.util.Map;
import net.sadness.maintenance.config.ConfigException;
import net.sadness.maintenance.config.ConfigFile;
import net.sadness.maintenance.config.ConfigLoad;
import net.sadness.maintenance.config.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public final class ConfigReloader {

    private final Plugin plugin;
    private final ConfigFile configFile;
    private final MaintenanceService service;

    public ConfigReloader(Plugin plugin, ConfigFile configFile, MaintenanceService service) {
        this.plugin = plugin;
        this.configFile = configFile;
        this.service = service;
    }

    public void reload(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ConfigLoad load = configFile.load();
                Bukkit.getScheduler().runTask(plugin, () -> apply(sender, load));
            } catch (IOException | ConfigException e) {
                String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(
                        service.settings().renderer().render(MessageKey.RELOAD_FAILED, Map.of("error", error))));
            }
        });
    }

    private void apply(CommandSender sender, ConfigLoad load) {
        load.warnings().forEach(warning -> plugin.getLogger().warning("config.yml: " + warning));
        service.applySettings(Settings.of(load.config()));
        if (load.warnings().isEmpty()) {
            sender.sendMessage(service.settings().renderer().render(MessageKey.RELOAD_SUCCESS));
        } else {
            sender.sendMessage(service.settings().renderer().render(
                    MessageKey.RELOAD_WARNINGS, Map.of("count", String.valueOf(load.warnings().size()))));
        }
    }
}
