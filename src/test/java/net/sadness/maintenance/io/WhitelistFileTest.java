package net.sadness.maintenance.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.sadness.maintenance.core.WhitelistEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WhitelistFileTest {

    private static final UUID FIRST = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final UUID SECOND = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");

    @TempDir
    Path directory;

    private Path path;
    private WhitelistFile file;

    @BeforeEach
    void setUp() {
        path = directory.resolve("whitelist.yml");
        file = new WhitelistFile(path);
    }

    @Test
    void missingFileMeansEmptyWithoutWarnings() {
        WhitelistLoad load = file.load();
        assertTrue(load.entries().isEmpty());
        assertTrue(load.warnings().isEmpty());
    }

    @Test
    void roundTripsEntries() throws IOException {
        List<WhitelistEntry> entries = List.of(new WhitelistEntry(FIRST, "Notch"), new WhitelistEntry(SECOND, "jeb_"));
        file.save(entries);
        WhitelistLoad load = file.load();
        assertEquals(entries, load.entries());
        assertTrue(load.warnings().isEmpty());
    }

    @Test
    void roundTripsNamesThatLookLikeOtherYamlTypes() throws IOException {
        List<WhitelistEntry> entries = List.of(
                new WhitelistEntry(FIRST, "12345"),
                new WhitelistEntry(SECOND, "true"));
        file.save(entries);
        assertEquals(entries, file.load().entries());
    }

    @Test
    void roundTripsEmptyWhitelist() throws IOException {
        file.save(List.of());
        WhitelistLoad load = file.load();
        assertTrue(load.entries().isEmpty());
        assertTrue(load.warnings().isEmpty());
    }

    @Test
    void emptyDocumentMeansEmptyWhitelist() throws IOException {
        Files.writeString(path, "");
        WhitelistLoad load = file.load();
        assertTrue(load.entries().isEmpty());
        assertTrue(load.warnings().isEmpty());
    }

    @Test
    void invalidEntriesAreSkippedWithWarnings() throws IOException {
        Files.writeString(path, "players:\n"
                + "  069a79f4-44e9-4726-a5be-fca90e38aaf5: Notch\n"
                + "  not-a-uuid: Broken\n"
                + "  853c80ef-3c37-49fd-aa49-938b674adae6:\n"
                + "  00000000-0000-0000-0000-000000000003: \"  \"\n");
        WhitelistLoad load = file.load();
        assertEquals(List.of(new WhitelistEntry(FIRST, "Notch")), load.entries());
        assertEquals(3, load.warnings().size());
    }

    @Test
    void playersThatIsNotAMappingStartsEmptyWithWarning() throws IOException {
        Files.writeString(path, "players: [a, b]\n");
        WhitelistLoad load = file.load();
        assertTrue(load.entries().isEmpty());
        assertEquals(1, load.warnings().size());
    }

    @Test
    void corruptFileStartsEmptyWithWarning() throws IOException {
        Files.writeString(path, "{{{{ not yaml");
        WhitelistLoad load = file.load();
        assertTrue(load.entries().isEmpty());
        assertEquals(1, load.warnings().size());
    }

    @Test
    void missingPlayersKeyMeansEmpty() throws IOException {
        Files.writeString(path, "other: 1\n");
        WhitelistLoad load = file.load();
        assertTrue(load.entries().isEmpty());
        assertTrue(load.warnings().isEmpty());
    }
}
