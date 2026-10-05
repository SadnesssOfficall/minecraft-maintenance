package net.sadness.maintenance.io;

import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class YamlCodec {

    private static final int MAX_ALIASES = 10;

    private YamlCodec() {
    }

    public static Map<String, Object> parseMap(String text) throws YamlFormatException {
        Object root;
        try {
            root = newYaml().load(text);
        } catch (YAMLException | ClassCastException e) {
            throw new YamlFormatException("invalid YAML: " + firstLine(e.getMessage()), e);
        }
        if (root == null) {
            return Map.of();
        }
        if (!(root instanceof Map<?, ?> raw)) {
            throw new YamlFormatException("the document root must be a mapping");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    public static String dump(Map<String, Object> values) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options).dump(values);
    }

    private static Yaml newYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(MAX_ALIASES);
        return new Yaml(new SafeConstructor(options));
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "unknown error";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
