package net.sadness.maintenance.core;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MaintenanceWhitelist {

    private static final Comparator<WhitelistEntry> ORDER = Comparator
            .comparing((WhitelistEntry entry) -> entry.name().toLowerCase(Locale.ROOT))
            .thenComparing(WhitelistEntry::id);

    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public boolean add(WhitelistEntry entry) {
        return names.put(entry.id(), entry.name()) == null;
    }

    public Optional<WhitelistEntry> remove(UUID id) {
        String name = names.remove(id);
        return name == null ? Optional.empty() : Optional.of(new WhitelistEntry(id, name));
    }

    public boolean contains(UUID id) {
        return names.containsKey(id);
    }

    public Optional<WhitelistEntry> findByName(String name) {
        return names.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(name))
                .map(e -> new WhitelistEntry(e.getKey(), e.getValue()))
                .min(ORDER);
    }

    public List<WhitelistEntry> entries() {
        return names.entrySet().stream()
                .map(e -> new WhitelistEntry(e.getKey(), e.getValue()))
                .sorted(ORDER)
                .toList();
    }

    public int size() {
        return names.size();
    }

    public void replaceAll(Collection<WhitelistEntry> entries) {
        names.clear();
        entries.forEach(this::add);
    }
}
