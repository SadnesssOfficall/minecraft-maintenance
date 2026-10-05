package net.sadness.maintenance.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.sadness.maintenance.core.UuidInput;
import net.sadness.maintenance.core.WhitelistEntry;

public final class WhitelistFile {

    static final String KEY_PLAYERS = "players";

    private final Path path;

    public WhitelistFile(Path path) {
        this.path = path;
    }

    public WhitelistLoad load() {
        if (Files.notExists(path)) {
            return new WhitelistLoad(List.of(), List.of());
        }
        try {
            return parse(YamlCodec.parseMap(BoundedFileReader.readUtf8(path)));
        } catch (IOException | YamlFormatException e) {
            return new WhitelistLoad(List.of(),
                    List.of("could not read " + path.getFileName() + ": " + e.getMessage()
                            + "; the maintenance whitelist starts empty"));
        }
    }

    public void save(Collection<WhitelistEntry> entries) throws IOException {
        Map<String, Object> players = new LinkedHashMap<>();
        entries.forEach(entry -> players.put(entry.id().toString(), entry.name()));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put(KEY_PLAYERS, players);
        AtomicFileWriter.write(path, YamlCodec.dump(root));
    }

    private WhitelistLoad parse(Map<String, Object> values) {
        Object players = values.get(KEY_PLAYERS);
        if (players == null) {
            return new WhitelistLoad(List.of(), List.of());
        }
        if (!(players instanceof Map<?, ?> map)) {
            return new WhitelistLoad(List.of(),
                    List.of("'" + KEY_PLAYERS + "' is not a mapping in " + path.getFileName()
                            + "; the maintenance whitelist starts empty"));
        }
        List<WhitelistEntry> entries = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        map.forEach((key, value) -> {
            Optional<UUID> id = UuidInput.parse(String.valueOf(key));
            if (id.isEmpty() || value == null || String.valueOf(value).isBlank()) {
                warnings.add("skipped invalid whitelist entry '" + key + "' in " + path.getFileName());
                return;
            }
            entries.add(new WhitelistEntry(id.get(), String.valueOf(value)));
        });
        return new WhitelistLoad(entries, warnings);
    }
}
