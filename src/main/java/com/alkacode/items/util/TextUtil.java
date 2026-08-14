package com.alkacode.items.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

public final class TextUtil {

    private TextUtil() { }

    public static Component parse(String raw) {
        return MiniMessage.miniMessage().deserialize(raw == null ? "" : raw);
    }

    public static Component parse(String raw, Map<String, String> placeholders) {
        return parse(replace(raw, placeholders));
    }

    public static String replace(String raw, Map<String, String> placeholders) {
        String result = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }

    public static String plain(String miniMessage) {
        return PlainTextComponentSerializer.plainText().serialize(parse(miniMessage));
    }
}
