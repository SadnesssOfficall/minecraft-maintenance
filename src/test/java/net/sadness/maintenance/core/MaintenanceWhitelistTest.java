package net.sadness.maintenance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MaintenanceWhitelistTest {

    private static final WhitelistEntry ALICE = new WhitelistEntry(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Alice");
    private static final WhitelistEntry BOB = new WhitelistEntry(UUID.fromString("00000000-0000-0000-0000-000000000002"), "bob");

    private MaintenanceWhitelist whitelist;

    @BeforeEach
    void setUp() {
        whitelist = new MaintenanceWhitelist();
    }

    @Test
    void addReportsNewEntries() {
        assertTrue(whitelist.add(ALICE));
        assertFalse(whitelist.add(ALICE));
        assertEquals(1, whitelist.size());
    }

    @Test
    void containsMatchesByUuidOnly() {
        whitelist.add(ALICE);
        assertTrue(whitelist.contains(ALICE.id()));
        assertFalse(whitelist.contains(BOB.id()));
    }

    @Test
    void addingSameUuidUpdatesName() {
        whitelist.add(ALICE);
        whitelist.add(new WhitelistEntry(ALICE.id(), "AliceRenamed"));
        assertEquals(List.of(new WhitelistEntry(ALICE.id(), "AliceRenamed")), whitelist.entries());
    }

    @Test
    void removeReturnsRemovedEntry() {
        whitelist.add(ALICE);
        assertEquals(Optional.of(ALICE), whitelist.remove(ALICE.id()));
        assertFalse(whitelist.contains(ALICE.id()));
    }

    @Test
    void removeUnknownReturnsEmpty() {
        assertEquals(Optional.empty(), whitelist.remove(ALICE.id()));
    }

    @Test
    void findByNameIsCaseInsensitive() {
        whitelist.add(ALICE);
        assertEquals(Optional.of(ALICE), whitelist.findByName("aLiCe"));
        assertEquals(Optional.empty(), whitelist.findByName("nobody"));
    }

    @Test
    void entriesAreSortedByNameIgnoringCase() {
        whitelist.add(BOB);
        whitelist.add(ALICE);
        assertEquals(List.of(ALICE, BOB), whitelist.entries());
    }

    @Test
    void replaceAllDiscardsPreviousContent() {
        whitelist.add(ALICE);
        whitelist.replaceAll(List.of(BOB));
        assertEquals(List.of(BOB), whitelist.entries());
    }

    @Test
    void entryRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new WhitelistEntry(UUID.randomUUID(), "  "));
    }

    @Test
    void entryRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new WhitelistEntry(null, "x"));
        assertThrows(NullPointerException.class, () -> new WhitelistEntry(UUID.randomUUID(), null));
    }
}
