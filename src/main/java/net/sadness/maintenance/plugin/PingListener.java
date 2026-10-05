package net.sadness.maintenance.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public final class PingListener implements Listener {

    private final MaintenanceService service;

    public PingListener(MaintenanceService service) {
        this.service = service;
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        Settings settings = service.settings();
        if (settings.config().motdEnabled() && service.status().isActive()) {
            event.motd(settings.motd());
        }
    }
}
