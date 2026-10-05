package net.sadness.maintenance.plugin;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.sadness.maintenance.config.MaintenanceConfig;
import net.sadness.maintenance.core.AccessPolicy;
import net.sadness.maintenance.message.MessageRenderer;

public record Settings(MaintenanceConfig config, MessageRenderer renderer, AccessPolicy policy, Component motd) {

    public static Settings of(MaintenanceConfig config) {
        MessageRenderer renderer = new MessageRenderer(config.messages());
        return new Settings(
                config,
                renderer,
                new AccessPolicy(config.bypassOperators()),
                renderer.renderTemplate(config.motdTemplate(), Map.of()));
    }
}
