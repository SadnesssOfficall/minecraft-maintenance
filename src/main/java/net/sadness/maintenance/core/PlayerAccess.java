package net.sadness.maintenance.core;

public record PlayerAccess(boolean whitelisted, boolean operator, boolean bypassPermission) {
}
