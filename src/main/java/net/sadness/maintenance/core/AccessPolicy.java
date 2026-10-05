package net.sadness.maintenance.core;

public final class AccessPolicy {

    private final boolean operatorsBypass;

    public AccessPolicy(boolean operatorsBypass) {
        this.operatorsBypass = operatorsBypass;
    }

    public boolean allows(boolean maintenanceActive, PlayerAccess access) {
        if (!maintenanceActive) {
            return true;
        }
        return access.whitelisted()
                || access.bypassPermission()
                || (operatorsBypass && access.operator());
    }
}
