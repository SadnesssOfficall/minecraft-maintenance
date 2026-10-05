package net.sadness.maintenance.io;

import java.util.List;
import net.sadness.maintenance.core.WhitelistEntry;

public record WhitelistLoad(List<WhitelistEntry> entries, List<String> warnings) {

    public WhitelistLoad {
        entries = List.copyOf(entries);
        warnings = List.copyOf(warnings);
    }
}
