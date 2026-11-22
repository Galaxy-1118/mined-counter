package minedcounter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;

/**
 * Parses the title argument. Two formats are accepted:
 *
 * <ul>
 *   <li>classic ampersand codes: {@code &6&l&n挖掘榜};</li>
 *   <li>a JSON text component, e.g.
 *       {@code {"text":"挖掘榜","color":"yellow","underlined":true}}.
 *       A style-only object such as {@code {color:"yellow";underlined:true}}
 *       is applied to the current title text.</li>
 * </ul>
 *
 * <p>The JSON parser is deliberately lenient: unquoted keys, single quotes and
 * semicolon separators are normalised so the short form shown in the README
 * works in chat.</p>
 */
final class TitleParser {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();
    private static final String DEFAULT_TEXT = "挖掘榜";

    private TitleParser() {
    }

    static boolean looksLikeJson(String raw) {
        if (raw == null) {
            return false;
        }
        String trimmed = raw.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    /** Auto-detects JSON first, then falls back to the classic &-code parser. */
    static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        if (looksLikeJson(raw)) {
            Component json = parseJson(raw, DEFAULT_TEXT);
            if (json != null) {
                return json;
            }
        }
        return parseLegacy(raw);
    }

    /**
     * Parses a JSON component. When the object has no content field (style-only
     * JSON), {@code fallbackText} is inserted as its text.
     *
     * @return the parsed component, or {@code null} if the JSON is invalid
     */
    static Component parseJson(String raw, String fallbackText) {
        JsonElement element = parseLenient(raw);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (!hasContent(object)) {
                object.addProperty("text", textOrDefault(fallbackText));
            }
            element = object;
        }
        try {
            DataResult<Component> result = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element);
            return result.result().orElse(null);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * Returns a canonical JSON string with a text field added to style-only
     * objects, or {@code null} if the input is not a valid component.
     */
    static String canonicalJson(String raw, String fallbackText) {
        JsonElement element = parseLenient(raw);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (!hasContent(object)) {
                object.addProperty("text", textOrDefault(fallbackText));
            }
            element = object;
        }
        Component parsed = parseJson(GSON.toJson(element), fallbackText);
        if (parsed == null) {
            return null;
        }
        return GSON.toJson(element);
    }

    static String plainText(String raw) {
        return parse(raw).getString();
    }

    private static String textOrDefault(String fallbackText) {
        return fallbackText == null || fallbackText.isBlank() ? DEFAULT_TEXT : fallbackText;
    }

    private static boolean hasContent(JsonObject object) {
        return object.has("text")
                || object.has("translate")
                || object.has("keybind")
                || object.has("score")
                || object.has("selector")
                || object.has("nbt")
                || object.has("extra")
                || object.has("type");
    }

    private static JsonElement parseLenient(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return JsonParser.parseString(normalizeSeparators(raw));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /** Gson tolerates unquoted keys, but not semicolons used as separators. */
    private static String normalizeSeparators(String raw) {
        StringBuilder result = new StringBuilder(raw.length());
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (escaped) {
                result.append(current);
                escaped = false;
                continue;
            }
            if (quoted && current == '\\') {
                result.append(current);
                escaped = true;
                continue;
            }
            if (current == '"') {
                quoted = !quoted;
                result.append(current);
                continue;
            }
            result.append(!quoted && current == ';' ? ',' : current);
        }
        return result.toString();
    }

    private static Component parseLegacy(String raw) {
        MutableComponent result = Component.empty();
        if (raw == null || raw.isEmpty()) {
            return result;
        }

        StyleState style = new StyleState();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if ((current == '&' || current == '\u00a7') && i + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(i + 1));
                if (code == '&') {
                    text.append('&');
                    i++;
                    continue;
                }
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    flush(result, text, style);
                    style.apply(formatting);
                    i++;
                    continue;
                }
            }
            text.append(current);
        }
        flush(result, text, style);
        return result;
    }

    private static void flush(MutableComponent result, StringBuilder text, StyleState style) {
        if (text.length() == 0) {
            return;
        }
        MutableComponent part = Component.literal(text.toString());
        if (style.color != null) {
            part.withStyle(style.color);
        }
        if (style.obfuscated) {
            part.withStyle(ChatFormatting.OBFUSCATED);
        }
        if (style.bold) {
            part.withStyle(ChatFormatting.BOLD);
        }
        if (style.strikethrough) {
            part.withStyle(ChatFormatting.STRIKETHROUGH);
        }
        if (style.underline) {
            part.withStyle(ChatFormatting.UNDERLINE);
        }
        if (style.italic) {
            part.withStyle(ChatFormatting.ITALIC);
        }
        result.append(part);
        text.setLength(0);
    }

    private static final class StyleState {
        private ChatFormatting color;
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private boolean strikethrough;
        private boolean obfuscated;

        private void apply(ChatFormatting formatting) {
            if (formatting == ChatFormatting.RESET) {
                reset();
                return;
            }
            if (formatting.isColor()) {
                color = formatting;
                return;
            }
            if (formatting == ChatFormatting.BOLD) {
                bold = true;
            } else if (formatting == ChatFormatting.ITALIC) {
                italic = true;
            } else if (formatting == ChatFormatting.UNDERLINE) {
                underline = true;
            } else if (formatting == ChatFormatting.STRIKETHROUGH) {
                strikethrough = true;
            } else if (formatting == ChatFormatting.OBFUSCATED) {
                obfuscated = true;
            }
        }

        private void reset() {
            color = null;
            bold = false;
            italic = false;
            underline = false;
            strikethrough = false;
            obfuscated = false;
        }
    }
}
