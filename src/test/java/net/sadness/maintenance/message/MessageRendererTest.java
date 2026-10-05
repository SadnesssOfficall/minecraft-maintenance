package net.sadness.maintenance.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.sadness.maintenance.config.MessageKey;
import net.sadness.maintenance.config.Messages;
import org.junit.jupiter.api.Test;

class MessageRendererTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static MessageRenderer renderer(Map<MessageKey, String> overrides) {
        return new MessageRenderer(new Messages(overrides));
    }

    @Test
    void rendersPlaceholders() {
        MessageRenderer renderer = renderer(Map.of(MessageKey.COUNTDOWN, "ends in <remaining>"));
        assertEquals("ends in 5m", plain(renderer.render(MessageKey.COUNTDOWN, Map.of("remaining", "5m"))));
    }

    @Test
    void expandsPrefixTag() {
        MessageRenderer renderer = renderer(Map.of(MessageKey.PREFIX, "[M]", MessageKey.EXPIRED, "<prefix> over"));
        assertEquals("[M] over", plain(renderer.render(MessageKey.EXPIRED)));
    }

    @Test
    void prefixMayContainMiniMessageFormatting() {
        MessageRenderer renderer = renderer(Map.of(MessageKey.PREFIX, "<red>[M]</red>", MessageKey.EXPIRED, "<prefix> over"));
        assertEquals("[M] over", plain(renderer.render(MessageKey.EXPIRED)));
    }

    @Test
    void playerInputIsNeverParsedAsMiniMessage() {
        MessageRenderer renderer = renderer(Map.of(MessageKey.PLAYER_UNKNOWN, "unknown <player>"));
        Component rendered = renderer.render(MessageKey.PLAYER_UNKNOWN, Map.of("player", "<red>evil</red><click:run_command:/op x>"));
        assertEquals("unknown <red>evil</red><click:run_command:/op x>", plain(rendered));
        assertFalse(rendered.children().stream().anyMatch(child -> child.clickEvent() != null));
    }

    @Test
    void blankPrefixRendersEmpty() {
        MessageRenderer renderer = renderer(Map.of(MessageKey.PREFIX, "", MessageKey.EXPIRED, "<prefix>over"));
        assertEquals("over", plain(renderer.render(MessageKey.EXPIRED)));
    }

    @Test
    void missingPlaceholderValueLeavesTagUnresolved() {
        MessageRenderer renderer = renderer(Map.of(MessageKey.COUNTDOWN, "x <remaining> y"));
        assertEquals("x <remaining> y", plain(renderer.render(MessageKey.COUNTDOWN)));
    }

    @Test
    void newlinesAreKept() {
        assertEquals("a\nb", plain(renderer(Map.of(MessageKey.KICK, "a\nb")).render(MessageKey.KICK)));
    }

    @Test
    void everyDefaultTemplateRendersWithoutError() {
        MessageRenderer renderer = new MessageRenderer(Messages.defaults());
        Map<String, String> values = Map.of(
                "duration", "1h", "remaining", "5m", "input", "x", "reason", "r", "count", "2", "error", "e", "player", "p");
        for (MessageKey key : MessageKey.values()) {
            assertFalse(plain(renderer.render(key, values)).contains("<"), key.path());
        }
    }

    @Test
    void renderTemplateUsesProvidedText() {
        assertEquals("hi", plain(renderer(Map.of()).renderTemplate("<green>hi</green>", Map.of())));
    }
}
