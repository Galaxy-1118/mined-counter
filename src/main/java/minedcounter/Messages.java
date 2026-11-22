package minedcounter;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Server-side localisation for command feedback. The player's client language
 * is used when the command came from a player; console and command blocks fall
 * back to the server operating-system language. English is the fallback for all
 * non-Chinese locales.
 */
final class Messages {
    enum Key {
        ENABLED("挖掘统计已开启。", "Counting enabled."),
        DISABLED("挖掘统计已关闭，已有分数保留；计分板已隐藏。",
                "Counting disabled; existing scores are kept and the board is hidden."),
        SHOWN("计分板已显示，位置：%s。", "Board shown at %s."),
        HIDDEN("计分板已隐藏；统计仍在继续。", "Board hidden; counting continues."),
        CANNOT_SHOW_DISABLED("统计当前已关闭，请先执行 /minedcounter enable。",
                "Counting is disabled; run /minedcounter enable first."),
        SIDEBAR_BLOCKED("配置中 sidebar=false，无法使用 sidebar；请修改配置文件，或执行 /minedcounter position list。",
                "sidebar=false in the config; edit the config or use /minedcounter position list."),
        TITLE_EMPTY("标题不能为空。", "Title cannot be empty."),
        TITLE_TOO_LONG("标题最长 %d 个字符。", "Title is limited to %d characters."),
        TITLE_ONLY_FORMAT("标题不能只包含颜色/格式代码。", "Title cannot contain only formatting codes."),
        TITLE_JSON_INVALID("JSON 标题格式无效，请检查括号、引号和字段名。",
                "Invalid JSON title; check brackets, quotes and field names."),
        TITLE_UPDATED("标题已更新：", "Title updated: "),
        POSITION_SET("显示位置已设为 %s。", "Display position set to %s."),
        POSITION_UNKNOWN("未知的显示位置：%s。可用：sidebar、list、below_name。",
                "Unknown position: %s. Use sidebar, list or below_name."),
        INIT_RESULT("已重置并初始化挖掘榜：在线玩家 %d 人，离线玩家 %d 人，写入 %d 条分数，合计 %d 次挖掘。",
                "Rebuilt the board: %d online, %d offline, %d scores, %d blocks mined."),
        RELOADED("配置已重新加载：title=%s，position=%s，sidebar=%s，countCreative=%s。",
                "Config reloaded: title=%s, position=%s, sidebar=%s, countCreative=%s."),
        CREATIVE_ON("创造模式破坏方块会计入统计；注意：/minedcounter init 无法从原版统计恢复这部分计数。",
                "Creative block breaks are now counted; note that /minedcounter init cannot restore them from vanilla stats."),
        CREATIVE_OFF("创造模式破坏方块不再计入统计。", "Creative block breaks are no longer counted."),
        STATUS_HEADER("Mined Counter 状态", "Mined Counter status"),
        STATUS_COUNTING("统计：%s", "Counting: %s"),
        STATUS_VISIBLE("显示：%s（位置：%s）", "Display: %s (position: %s)"),
        STATUS_SIDEBAR("sidebar 允许：%s%s", "Sidebar allowed: %s%s"),
        STATUS_CREATIVE("创造模式计数：%s", "Creative counting: %s"),
        STATUS_TITLE("标题：", "Title: "),
        STATUS_CONFIG("配置：config/mined_counter.json", "Config: config/mined_counter.json"),
        ON("开启", "on"),
        OFF("关闭", "off"),
        YES("是", "yes"),
        NO("否", "no"),
        SHOWN_LABEL("显示", "shown"),
        HIDDEN_LABEL("隐藏", "hidden"),
        BLOCKED_SUFFIX("（当前配置阻止 sidebar 显示）", " (blocked by config)");

        private final String zh;
        private final String en;

        Key(String zh, String en) {
            this.zh = zh;
            this.en = en;
        }
    }

    private Messages() {
    }

    static String text(CommandSourceStack source, Key key, Object... args) {
        String pattern = isChinese(source) ? key.zh : key.en;
        return args.length == 0 ? pattern : String.format(Locale.ROOT, pattern, args);
    }

    private static boolean isChinese(CommandSourceStack source) {
        String language = null;
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            language = player.clientInformation().language();
        }
        if (language == null || language.isBlank()) {
            language = Locale.getDefault().getLanguage();
        }
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("zh");
    }
}
