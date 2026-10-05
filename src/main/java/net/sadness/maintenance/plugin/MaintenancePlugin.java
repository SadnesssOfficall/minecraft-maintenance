package net.sadness.maintenance.plugin;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.logging.Level;
import net.sadness.maintenance.config.ConfigException;
import net.sadness.maintenance.config.ConfigFile;
import net.sadness.maintenance.config.ConfigLoad;
import net.sadness.maintenance.config.MaintenanceConfig;
import net.sadness.maintenance.core.MaintenanceWhitelist;
import net.sadness.maintenance.io.Persistence;
import net.sadness.maintenance.io.StateFile;
import net.sadness.maintenance.io.StateLoad;
import net.sadness.maintenance.io.WhitelistFile;
import net.sadness.maintenance.io.WhitelistLoad;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MaintenancePlugin extends JavaPlugin {

    private static final String CONFIG_FILE = "config.yml";
    private static final String STATE_FILE = "state.yml";
    private static final String WHITELIST_FILE = "whitelist.yml";
    private static final String COMMAND = "maintenance";

    private MaintenanceService service;
    private Persistence persistence;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Path dataDirectory = getDataFolder().toPath();
        ConfigFile configFile = new ConfigFile(dataDirectory.resolve(CONFIG_FILE));
        MaintenanceConfig config = loadInitialConfig(configFile);

        StateFile stateFile = new StateFile(dataDirectory.resolve(STATE_FILE));
        WhitelistFile whitelistFile = new WhitelistFile(dataDirectory.resolve(WHITELIST_FILE));
        persistence = new Persistence(stateFile, whitelistFile, getLogger());

        MaintenanceWhitelist whitelist = new MaintenanceWhitelist();
        WhitelistLoad whitelistLoad = whitelistFile.load();
        whitelistLoad.warnings().forEach(getLogger()::warning);
        whitelist.replaceAll(whitelistLoad.entries());

        service = new MaintenanceService(this, whitelist, persistence, Clock.systemUTC(), Settings.of(config));
        if (config.persistState()) {
            StateLoad stateLoad = stateFile.load();
            stateLoad.warnings().forEach(getLogger()::warning);
            service.restore(stateLoad.status());
        }

        PluginCommand command = Objects.requireNonNull(getCommand(COMMAND), "command missing from plugin.yml");
        MaintenanceCommand executor = new MaintenanceCommand(
                service,
                new WhitelistCommand(service, new PlayerResolver()),
                new ConfigReloader(this, configFile, service));
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new LoginListener(service), this);
        getServer().getPluginManager().registerEvents(new PingListener(service), this);
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.shutdown();
        }
        if (persistence != null) {
            persistence.close();
        }
    }

    private MaintenanceConfig loadInitialConfig(ConfigFile configFile) {
        try {
            ConfigLoad load = configFile.load();
            load.warnings().forEach(warning -> getLogger().warning("config.yml: " + warning));
            return load.config();
        } catch (IOException | ConfigException e) {
            getLogger().log(Level.SEVERE, "Could not load config.yml, using built-in defaults", e);
            return MaintenanceConfig.defaults();
        }
    }
}
