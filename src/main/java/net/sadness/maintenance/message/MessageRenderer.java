package net.sadness.maintenance.message;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.sadness.maintenance.config.MessageKey;
import net.sadness.maintenance.config.Messages;

public final class MessageRenderer {

    private static final String PREFIX_TAG = "prefix";

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Messages messages;

    public MessageRenderer(Messages messages) {
        this.messages = messages;
    }

    public Component render(MessageKey key) {
        return render(key, Map.of());
    }

    public Component render(MessageKey key, Map<String, String> values) {
        return renderTemplate(messages.template(key), values);
    }

    public Component renderTemplate(String template, Map<String, String> values) {
        TagResolver.Builder resolvers = TagResolver.builder()
                .resolver(Placeholder.parsed(PREFIX_TAG, messages.template(MessageKey.PREFIX)));
        values.forEach((name, value) -> resolvers.resolver(Placeholder.unparsed(name, value)));
        return miniMessage.deserialize(template, resolvers.build());
    }
}
