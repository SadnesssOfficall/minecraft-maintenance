package net.sadness.maintenance.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicFileWriterTest {

    @TempDir
    Path directory;

    @Test
    void writesNewFile() throws IOException {
        Path target = directory.resolve("state.yml");
        AtomicFileWriter.write(target, "active: true\n");
        assertEquals("active: true\n", Files.readString(target));
    }

    @Test
    void replacesExistingFile() throws IOException {
        Path target = directory.resolve("state.yml");
        AtomicFileWriter.write(target, "first");
        AtomicFileWriter.write(target, "second");
        assertEquals("second", Files.readString(target));
    }

    @Test
    void leavesNoTemporaryFileBehind() throws IOException {
        AtomicFileWriter.write(directory.resolve("state.yml"), "x");
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void createsMissingParentDirectories() throws IOException {
        Path target = directory.resolve("nested").resolve("deeper").resolve("state.yml");
        AtomicFileWriter.write(target, "x");
        assertTrue(Files.exists(target));
    }

    @Test
    void writesUtf8() throws IOException {
        Path target = directory.resolve("names.yml");
        AtomicFileWriter.write(target, "name: çığ\n");
        assertEquals("name: çığ\n", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
    }

    @Test
    void failureKeepsPreviousContentAndCleansUp() throws IOException {
        Path target = directory.resolve("state.yml");
        AtomicFileWriter.write(target, "original");
        Path blocker = directory.resolve("blocked");
        Files.createDirectory(blocker);
        Files.writeString(blocker.resolve("child"), "keep");
        assertThrows(IOException.class, () -> AtomicFileWriter.write(blocker, "data"));
        assertEquals("original", Files.readString(target));
        assertFalse(Files.exists(directory.resolve("blocked.tmp")));
        assertTrue(Files.isDirectory(blocker));
    }
}
