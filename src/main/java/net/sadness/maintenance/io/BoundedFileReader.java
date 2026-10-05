package net.sadness.maintenance.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BoundedFileReader {

    public static final long MAX_BYTES = 256L * 1024L;

    private BoundedFileReader() {
    }

    public static String readUtf8(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_BYTES) {
            throw new IOException("file is larger than " + MAX_BYTES + " bytes: " + path.getFileName());
        }
        byte[] bytes = Files.readAllBytes(path);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new IOException("file is not valid UTF-8: " + path.getFileName(), e);
        }
    }
}
