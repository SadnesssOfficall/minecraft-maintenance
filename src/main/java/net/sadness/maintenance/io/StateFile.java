package net.sadness.maintenance.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sadness.maintenance.core.MaintenanceStatus;

public final class StateFile {

    static final String KEY_ACTIVE = "active";
    static final String KEY_ENDS_AT = "ends-at";

    private final Path path;

    public StateFile(Path path) {
        this.path = path;
    }

    public StateLoad load() {
        if (Files.notExists(path)) {
            return new StateLoad(MaintenanceStatus.inactive(), List.of());
        }
        try {
            return parse(YamlCodec.parseMap(BoundedFileReader.readUtf8(path)));
        } catch (IOException | YamlFormatException e) {
            return fallback("could not read " + path.getFileName() + ": " + e.getMessage());
        }
    }

    public void save(MaintenanceStatus status) throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(KEY_ACTIVE, status.isActive());
        status.endsAt().ifPresent(endsAt -> values.put(KEY_ENDS_AT, endsAt.toEpochMilli()));
        AtomicFileWriter.write(path, YamlCodec.dump(values));
    }

    private StateLoad parse(Map<String, Object> values) {
        Object active = values.get(KEY_ACTIVE);
        if (!(active instanceof Boolean isActive)) {
            return fallback("'" + KEY_ACTIVE + "' is missing or not a boolean in " + path.getFileName());
        }
        if (!isActive) {
            return new StateLoad(MaintenanceStatus.inactive(), List.of());
        }
        Object endsAt = values.get(KEY_ENDS_AT);
        if (endsAt == null) {
            return new StateLoad(MaintenanceStatus.permanent(), List.of());
        }
        if (endsAt instanceof Integer || endsAt instanceof Long) {
            Instant instant = Instant.ofEpochMilli(((Number) endsAt).longValue());
            return new StateLoad(MaintenanceStatus.until(instant), List.of());
        }
        return fallback("'" + KEY_ENDS_AT + "' is not an epoch-millisecond number in " + path.getFileName());
    }

    private static StateLoad fallback(String warning) {
        return new StateLoad(MaintenanceStatus.inactive(), List.of(warning + "; maintenance mode starts disabled"));
    }
}
