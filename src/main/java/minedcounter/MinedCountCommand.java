package minedcounter;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Admin commands. Permission level 2 (gamemasters) is enough; no mod-specific
 * permission node is needed.
 */
public final class MinedCountCommand {
    private MinedCountCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("minedcount")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

            root.then(Commands.literal("enable")
                    .executes(context -> setEnabled(context, true)));
            root.then(Commands.literal("disable")
                    .executes(context -> setEnabled(context, false)));
            root.then(Commands.literal("show")
                    .executes(context -> setVisible(context, true)));
            root.then(Commands.literal("hide")
                    .executes(context -> setVisible(context, false)));
            root.then(Commands.literal("title")
                    .then(Commands.argument("title", StringArgumentType.greedyString())
                            .executes(MinedCountCommand::setTitle)));
            root.then(Commands.literal("position")
                    .then(Commands.argument("position", StringArgumentType.word())
                            .executes(MinedCountCommand::setPosition)));
            root.then(Commands.literal("init")
                    .executes(context -> init(context, true))
                    .then(Commands.literal("online")
                            .executes(context -> init(context, false))));
            root.then(Commands.literal("reload")
                    .executes(MinedCountCommand::reload));
            root.then(Commands.literal("status")
                    .executes(MinedCountCommand::status));

            dispatcher.register(root);
        });
    }

    private static int setEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        CommandSourceStack source = context.getSource();
        CounterHook.setEnabled(source.getServer(), enabled);
        if (enabled) {
            return success(source, "挖掘统计已开启。");
        }
        return success(source, "挖掘统计已关闭，已有分数保留；计分板已隐藏。");
    }

    private static int setVisible(CommandContext<CommandSourceStack> context, boolean visible) {
        CommandSourceStack source = context.getSource();
        CounterConfig config = CounterHook.config();

        if (visible) {
            if (!config.enabled) {
                return failure(source, "统计当前已关闭，请先执行 /minedcount enable。");
            }
            if (config.usesSidebar() && !config.sidebar) {
                return failure(source, "配置中 sidebar=false，无法使用 sidebar；"
                        + "请修改配置文件，或执行 /minedcount position list。");
            }
        }

        CounterHook.setVisible(source.getServer(), visible);
        return success(source, visible
                ? "计分板已显示，位置：" + config.position + "。"
                : "计分板已隐藏；统计仍在继续。");
    }

    private static int setTitle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String title = StringArgumentType.getString(context, "title").trim();

        if (title.isEmpty()) {
            return failure(source, "标题不能为空。");
        }
        if (title.length() > CounterConfig.MAX_TITLE_LENGTH) {
            return failure(source, "标题最长 " + CounterConfig.MAX_TITLE_LENGTH + " 个字符。");
        }
        if (TitleParser.plainText(title).trim().isEmpty()) {
            return failure(source, "标题不能只包含颜色/格式代码。");
        }

        CounterHook.setTitle(source.getServer(), title);
        source.sendSuccess(() -> Component.literal("标题已更新：")
                .append(CounterHook.config().titleComponent()), true);
        return 1;
    }

    private static int setPosition(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String requested = StringArgumentType.getString(context, "position");
        String position = CounterConfig.normalizePosition(requested);

        if (position == null) {
            return failure(source, "未知的显示位置：" + requested
                    + "。可用：sidebar、list、below_name。");
        }
        if ("sidebar".equals(position) && !CounterHook.config().sidebar) {
            return failure(source, "配置中 sidebar=false，无法使用 sidebar；"
                    + "请修改配置文件，或选择 list / below_name。");
        }

        CounterHook.setPosition(source.getServer(), position);
        return success(source, "显示位置已设为 " + position + "。");
    }

    private static int init(CommandContext<CommandSourceStack> context, boolean includeOffline) {
        CommandSourceStack source = context.getSource();
        CounterHook.InitResult result = CounterHook.initializeFromStats(source.getServer(), includeOffline);
        return success(source, "已重置并初始化挖掘榜：在线玩家 " + result.onlinePlayers()
                + " 人，离线玩家 " + result.offlinePlayers()
                + " 人，写入 " + result.entries() + " 条分数，合计 " + result.totalMined() + " 次挖掘。");
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CounterConfig config = CounterHook.reload(source.getServer());
        return success(source, "配置已重新加载：title=" + config.title
                + "，position=" + config.position
                + "，sidebar=" + config.sidebar + "。");
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CounterConfig config = CounterHook.config();
        boolean sidebarBlocked = config.usesSidebar() && !config.sidebar;
        boolean effectiveVisible = config.enabled && config.visible && !sidebarBlocked;

        source.sendSuccess(() -> Component.literal("Mined Counter 状态")
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("统计：")
                .append(Component.literal(config.enabled ? "开启" : "关闭")
                        .withStyle(config.enabled ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        source.sendSuccess(() -> Component.literal("显示：")
                .append(Component.literal(effectiveVisible ? "显示" : "隐藏")
                        .withStyle(effectiveVisible ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal("（位置：" + config.position + "）")), false);
        source.sendSuccess(() -> Component.literal("sidebar 允许：")
                .append(Component.literal(config.sidebar ? "是" : "否")
                        .withStyle(config.sidebar ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal(sidebarBlocked ? "（当前配置阻止 sidebar 显示）" : "")), false);
        source.sendSuccess(() -> Component.literal("标题：")
                .append(config.titleComponent()), false);
        source.sendSuccess(() -> Component.literal("配置：config/mined_counter.json"), false);
        return 1;
    }

    private static int success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
        return 0;
    }
}
