package minedcounter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Parses a small, chat-friendly formatting language for the sidebar title.
 *
 * <p>Supported codes are the classic ampersand codes: {@code &0}-{@code &f}
 * colours, {@code &l} bold, {@code &n} underline, {@code &o} italic,
 * {@code &m} strikethrough, {@code &k} obfuscated and {@code &r} reset.
 * {@code &&} produces a literal ampersand. The section sign ({@code §}) is
 * accepted as an alias for {@code &} so titles copied from existing MOTD or
 * scoreboard strings keep working.</p>
 */
final class TitleParser {
    private TitleParser() {
    }

    static Component parse(String raw) {
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

    /** Visible text without any formatting codes, used to reject empty titles. */
    static String plainText(String raw) {
        return parse(raw).getString();
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
