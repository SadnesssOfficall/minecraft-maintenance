package net.sadness.maintenance.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigParserTest {

    private static ConfigLoad parse(String yaml) throws ConfigException {
        return ConfigParser.parse(yaml);
    }

    private static void assertDefaults(MaintenanceConfig config) {
        MaintenanceConfig defaults = MaintenanceConfig.defaults();
        assertEquals(defaults.persistState(), config.persistState());
        assertEquals(defaults.bypassOperators(), config.bypassOperators());
        assertEquals(defaults.motdEnabled(), config.motdEnabled());
        assertEquals(defaults.motdTemplate(), config.motdTemplate());
        assertEquals(defaults.countdownAnnouncements(), config.countdownAnnouncements());
        for (MessageKey key : MessageKey.values()) {
            assertEquals(defaults.messages().template(key), config.messages().template(key));
        }
    }

    @Test
    void emptyDocumentYieldsDefaults() throws ConfigException {
        ConfigLoad load = parse("");
        assertDefaults(load.config());
        assertTrue(load.warnings().isEmpty());
    }

    @Test
    void defaultsPersistState() {
        assertTrue(MaintenanceConfig.defaults().persistState());
    }

    @Test
    void readsScalarOptions() throws ConfigException {
        ConfigLoad load = parse("persist-state: false\nbypass-operators: false\nmotd:\n  enabled: false\n  text: hi\n");
        assertFalse(load.config().persistState());
        assertFalse(load.config().bypassOperators());
        assertFalse(load.config().motdEnabled());
        assertEquals("hi", load.config().motdTemplate());
        assertTrue(load.warnings().isEmpty());
    }

    @Test
    void invalidBooleanFallsBackWithWarning() throws ConfigException {
        ConfigLoad load = parse("persist-state: maybe\n");
        assertTrue(load.config().persistState());
        assertEquals(1, load.warnings().size());
    }

    @Test
    void announcementsAreParsedSortedAndDeduplicated() throws ConfigException {
        ConfigLoad load = parse("countdown-announcements: [30s, 5m, 30s, 1m]\n");
        assertEquals(
                List.of(Duration.ofMinutes(5), Duration.ofMinutes(1), Duration.ofSeconds(30)),
                load.config().countdownAnnouncements());
        assertTrue(load.warnings().isEmpty());
    }

    @Test
    void invalidAnnouncementEntriesAreSkippedWithWarnings() throws ConfigException {
        ConfigLoad load = parse("countdown-announcements: [1m, nope, 0s, -5m, 99d]\n");
        assertEquals(List.of(Duration.ofMinutes(1)), load.config().countdownAnnouncements());
        assertEquals(4, load.warnings().size());
    }

    @Test
    void announcementsOfWrongTypeFallBackToDefaults() throws ConfigException {
        ConfigLoad load = parse("countdown-announcements: 5m\n");
        assertEquals(MaintenanceConfig.DEFAULT_COUNTDOWN_ANNOUNCEMENTS, load.config().countdownAnnouncements());
        assertEquals(1, load.warnings().size());
    }

    @Test
    void emptyAnnouncementListDisablesAnnouncements() throws ConfigException {
        assertTrue(parse("countdown-announcements: []\n").config().countdownAnnouncements().isEmpty());
    }

    @Test
    void messageOverridesApply() throws ConfigException {
        ConfigLoad load = parse("messages:\n  kick: bye\n");
        assertEquals("bye", load.config().messages().template(MessageKey.KICK));
        assertEquals(MessageKey.ENABLED.defaultTemplate(), load.config().messages().template(MessageKey.ENABLED));
    }

    @Test
    void blankMessageIsRejectedExceptPrefix() throws ConfigException {
        ConfigLoad load = parse("messages:\n  kick: \"  \"\n  prefix: \"\"\n");
        assertEquals(MessageKey.KICK.defaultTemplate(), load.config().messages().template(MessageKey.KICK));
        assertEquals("", load.config().messages().template(MessageKey.PREFIX));
        assertEquals(1, load.warnings().size());
    }

    @Test
    void unknownKeysProduceWarnings() throws ConfigException {
        ConfigLoad load = parse("bogus: 1\nmessages:\n  nothing: x\n");
        assertEquals(2, load.warnings().size());
    }

    @Test
    void sectionOfWrongTypeFallsBackWithWarning() throws ConfigException {
        ConfigLoad load = parse("motd: yes\nmessages: [a]\n");
        assertDefaults(load.config());
        assertEquals(2, load.warnings().size());
    }

    @Test
    void malformedYamlThrows() {
        assertThrows(ConfigException.class, () -> parse("{{{ nope"));
    }

    @Test
    void nonMappingRootThrows() {
        assertThrows(ConfigException.class, () -> parse("- a\n- b\n"));
    }

    @Test
    void bundledConfigMatchesBuiltInDefaults() throws Exception {
        try (var stream = ConfigParserTest.class.getResourceAsStream("/config.yml")) {
            ConfigLoad load = parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            assertTrue(load.warnings().isEmpty());
            assertEquals(MaintenanceConfig.defaults().countdownAnnouncements(), load.config().countdownAnnouncements());
            assertEquals(MaintenanceConfig.DEFAULT_MOTD_TEMPLATE, load.config().motdTemplate());
            for (MessageKey key : MessageKey.values()) {
                assertEquals(key.defaultTemplate(), load.config().messages().template(key), key.path());
            }
        }
    }
}
