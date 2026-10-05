package net.sadness.maintenance.plugin;

import net.sadness.maintenance.config.MessageKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public final class LoginListener implements Listener {

    private final MaintenanceService service;

    public LoginListener(MaintenanceService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED || service.mayJoin(event.getPlayer())) {
            return;
        }
        event.disallow(
                PlayerLoginEvent.Result.KICK_OTHER,
                service.settings().renderer().render(MessageKey.LOGIN_DENIED));
    }
}
