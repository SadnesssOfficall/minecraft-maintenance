package net.sadness.maintenance.io;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sadness.maintenance.core.MaintenanceStatus;
import net.sadness.maintenance.core.WhitelistEntry;

public final class Persistence implements AutoCloseable {

    private final AsyncWriter writer = new AsyncWriter();
    private final StateFile stateFile;
    private final WhitelistFile whitelistFile;
    private final Logger logger;

    public Persistence(StateFile stateFile, WhitelistFile whitelistFile, Logger logger) {
        this.stateFile = stateFile;
        this.whitelistFile = whitelistFile;
        this.logger = logger;
    }

    public void saveState(MaintenanceStatus status) {
        submit(() -> stateFile.save(status), "maintenance state");
    }

    public void saveWhitelist(List<WhitelistEntry> entries) {
        submit(() -> whitelistFile.save(entries), "maintenance whitelist");
    }

    @Override
    public void close() {
        writer.close();
    }

    private void submit(AsyncWriter.IoAction action, String what) {
        boolean accepted = writer.submit(action, e -> logger.log(Level.SEVERE, "Could not save the " + what, e));
        if (!accepted) {
            logger.warning("The " + what + " was not saved because the plugin is shutting down");
        }
    }
}
