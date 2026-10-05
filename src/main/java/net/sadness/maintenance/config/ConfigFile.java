package net.sadness.maintenance.config;

import java.io.IOException;
import java.nio.file.Path;
import net.sadness.maintenance.io.BoundedFileReader;

public final class ConfigFile {

    private final Path path;

    public ConfigFile(Path path) {
        this.path = path;
    }

    public ConfigLoad load() throws IOException, ConfigException {
        return ConfigParser.parse(BoundedFileReader.readUtf8(path));
    }
}
