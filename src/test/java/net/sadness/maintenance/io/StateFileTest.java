package net.sadness.maintenance.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import net.sadness.maintenance.core.MaintenanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StateFileTest {

    private static final Instant END = Instant.parse("2026-10-05T12:30:00Z");

    @TempDir
    Path directory;

    private Path path;
    private StateFile stateFile;

    @BeforeEach
    void setUp() {
        path = directory.resolve("state.yml");
        stateFile = new StateFile(path);
    }

    @Test
    void missingFileMeansInactiveWithoutWarnings() {
        StateLoad load = stateFile.load();
        assertEquals(MaintenanceStatus.inactive(), load.status());
        assertTrue(load.warnings().isEmpty());
    }

    @Test
    void roundTripsInactive() throws IOException {
        stateFile.save(MaintenanceStatus.inactive());
        StateLoad load = stateFile.load();
        assertEquals(MaintenanceStatus.inactive(), load.status());
        assertTrue(load.warnings().isEmpty());
    }

    @Test
    void roundTripsPermanent() throws IOException {
        stateFile.save(MaintenanceStatus.permanent());
        StateLoad load = stateFile.load();
        assertEquals(MaintenanceStatus.permanent(), load.status());
        assertTrue(load.warnings().isEmpty());
    }

    @Test
    void roundTripsTimedWithMillisecondPrecision() throws IOException {
        Instant precise = END.plusMillis(123);
        stateFile.save(MaintenanceStatus.until(precise));
        assertEquals(MaintenanceStatus.until(precise), stateFile.load().status());
    }

    @Test
    void savedFileIsPlainYaml() throws IOException {
        stateFile.save(MaintenanceStatus.until(END));
        String content = Files.readString(path);
        assertTrue(content.contains("active: true"));
        assertTrue(content.contains("ends-at: " + END.toEpochMilli()));
    }

    @Test
    void inactiveStateIgnoresStaleEndTime() throws IOException {
        Files.writeString(path, "active: false\nends-at: 1\n");
        StateLoad load = stateFile.load();
        assertEquals(MaintenanceStatus.inactive(), load.status());
        assertTrue(load.warnings().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   \n",
        "{{{{ not yaml",
        "- a\n- b\n",
        "just a string",
        "active: yes please\n",
        "active: 1\n",
        "active: \"true\"\n",
        "ends-at: 123\n",
        "active: true\nends-at: soon\n",
        "active: true\nends-at: 1.5\n",
        "active: true\nends-at: 99999999999999999999999999\n",
        "active: true\nactive: false\n",
        "active: !!java.lang.Runtime {}\n"
    })
    void corruptContentFallsBackToInactiveWithWarning(String content) throws IOException {
        Files.writeString(path, content);
        StateLoad load = stateFile.load();
        assertFalse(load.status().isActive());
        assertEquals(1, load.warnings().size());
        assertTrue(load.warnings().get(0).contains("maintenance mode starts disabled"));
    }

    @Test
    void binaryGarbageFallsBackToInactiveWithWarning() throws IOException {
        Files.write(path, new byte[] {(byte) 0xFF, (byte) 0xFE, 0x00, (byte) 0xC3});
        StateLoad load = stateFile.load();
        assertFalse(load.status().isActive());
        assertEquals(1, load.warnings().size());
    }

    @Test
    void oversizedFileFallsBackToInactiveWithWarning() throws IOException {
        Files.write(path, new byte[(int) BoundedFileReader.MAX_BYTES + 1]);
        StateLoad load = stateFile.load();
        assertFalse(load.status().isActive());
        assertEquals(1, load.warnings().size());
    }

    @Test
    void directoryInPlaceOfFileFallsBackToInactiveWithWarning() throws IOException {
        Files.createDirectory(path);
        StateLoad load = stateFile.load();
        assertFalse(load.status().isActive());
        assertEquals(1, load.warnings().size());
    }

    @Test
    void saveOverwritesCorruptFile() throws IOException {
        Files.writeString(path, "{{{{ not yaml");
        stateFile.save(MaintenanceStatus.permanent());
        assertEquals(MaintenanceStatus.permanent(), stateFile.load().status());
    }
}
