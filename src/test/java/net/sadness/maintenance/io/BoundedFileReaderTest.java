package net.sadness.maintenance.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoundedFileReaderTest {

    @TempDir
    Path directory;

    @Test
    void readsSmallFile() throws IOException {
        Path file = directory.resolve("a.yml");
        Files.writeString(file, "hello");
        assertEquals("hello", BoundedFileReader.readUtf8(file));
    }

    @Test
    void rejectsOversizedFile() throws IOException {
        Path file = directory.resolve("big.yml");
        Files.write(file, new byte[(int) BoundedFileReader.MAX_BYTES + 1]);
        assertThrows(IOException.class, () -> BoundedFileReader.readUtf8(file));
    }

    @Test
    void acceptsFileAtLimit() throws IOException {
        Path file = directory.resolve("limit.yml");
        Files.write(file, "a".repeat((int) BoundedFileReader.MAX_BYTES).getBytes());
        assertEquals(BoundedFileReader.MAX_BYTES, BoundedFileReader.readUtf8(file).length());
    }

    @Test
    void rejectsInvalidUtf8() throws IOException {
        Path file = directory.resolve("bad.yml");
        Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28});
        assertThrows(IOException.class, () -> BoundedFileReader.readUtf8(file));
    }

    @Test
    void missingFileFails() {
        assertThrows(IOException.class, () -> BoundedFileReader.readUtf8(directory.resolve("none.yml")));
    }
}
