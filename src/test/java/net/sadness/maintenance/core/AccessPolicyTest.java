package net.sadness.maintenance.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccessPolicyTest {

    private static final PlayerAccess NORMAL = new PlayerAccess(false, false, false);
    private static final PlayerAccess WHITELISTED = new PlayerAccess(true, false, false);
    private static final PlayerAccess OPERATOR = new PlayerAccess(false, true, false);
    private static final PlayerAccess BYPASS = new PlayerAccess(false, false, true);

    private final AccessPolicy operatorsBypass = new AccessPolicy(true);
    private final AccessPolicy operatorsRestricted = new AccessPolicy(false);

    @Test
    void everyoneMayJoinWhenMaintenanceIsOff() {
        assertTrue(operatorsBypass.allows(false, NORMAL));
        assertTrue(operatorsRestricted.allows(false, NORMAL));
    }

    @Test
    void normalPlayerIsDeniedDuringMaintenance() {
        assertFalse(operatorsBypass.allows(true, NORMAL));
        assertFalse(operatorsRestricted.allows(true, NORMAL));
    }

    @Test
    void whitelistedPlayerIsAllowedDuringMaintenance() {
        assertTrue(operatorsBypass.allows(true, WHITELISTED));
        assertTrue(operatorsRestricted.allows(true, WHITELISTED));
    }

    @Test
    void bypassPermissionIsAllowedDuringMaintenance() {
        assertTrue(operatorsBypass.allows(true, BYPASS));
        assertTrue(operatorsRestricted.allows(true, BYPASS));
    }

    @Test
    void operatorIsAllowedWhenOperatorsBypass() {
        assertTrue(operatorsBypass.allows(true, OPERATOR));
    }

    @Test
    void operatorIsDeniedWhenOperatorBypassIsDisabled() {
        assertFalse(operatorsRestricted.allows(true, OPERATOR));
    }

    @Test
    void operatorWithBypassPermissionIsAllowedEvenWhenOperatorBypassIsDisabled() {
        assertTrue(operatorsRestricted.allows(true, new PlayerAccess(false, true, true)));
    }

    @Test
    void anyCombinationOfExemptionsIsAllowed() {
        assertTrue(operatorsBypass.allows(true, new PlayerAccess(true, true, true)));
    }
}
