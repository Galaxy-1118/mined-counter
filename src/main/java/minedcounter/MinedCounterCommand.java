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
public final class MinedCounterCommand {
    private MinedCounterCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("minedcounter")
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
                            .executes(MinedCounterCommand::setTitle)));
            root.then(Commands.literal("position")
                    .then(Commands.argument("position", StringArgumentType.word())
                            .executes(MinedCounterCommand::setPosition)));
            root.then(Commands.literal("init")
                    .executes(context -> init(context, true))
                    .then(Commands.literal("online")
                            .executes(context -> init(context, false))));
            root.then(Commands.literal("creative")
                    .then(Commands.literal("on")
                            .executes(context -> setCreative(context, true)))
                    .then(Commands.literal("off")
                            .executes(context -> setCreative(context, false))));
            root.then(Commands.literal("reload")
                    .executes(MinedCounterCommand::reload));
            root.then(Commands.literal("status")
                    .executes(MinedCounterCommand::status));

            dispatcher.register(root);
        });
    }

    private static int setEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        CommandSourceStack source = context.getSource();
        CounterHook.setEnabled(source.getServer(), enabled);
        return success(source, Messages.text(source, enabled ? Messages.Key.ENABLED : Messages.Key.DISABLED));
    }

    private static int setVisible(CommandContext<CommandSourceStack> context, boolean visible) {
        CommandSourceStack source = context.getSource();
        CounterConfig config = CounterHook.config();

        if (visible) {
            if (!config.enabled) {
                return failure(source, Messages.text(source, Messages.Key.CANNOT_SHOW_DISABLED));
            }
            if (config.usesSidebar() && !config.sidebar) {
                return failure(source, Messages.text(source, Messages.Key.SIDEBAR_BLOCKED));
            }
        }

        CounterHook.setVisible(source.getServer(), visible);
        return visible
                ? success(source, Messages.text(source, Messages.Key.SHOWN, config.position))
                : success(source, Messages.text(source, Messages.Key.HIDDEN));
    }

    private static int setTitle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String title = StringArgumentType.getString(context, "title").trim();

        if (title.isEmpty()) {
            return failure(source, Messages.text(source, Messages.Key.TITLE_EMPTY));
        }

        if (TitleParser.looksLikeJson(title)) {
            String fallback = CounterHook.config().plainTitle();
            String canonical = TitleParser.canonicalJson(title, fallback);
            if (canonical == null) {
                return failure(source, Messages.text(source, Messages.Key.TITLE_JSON_INVALID));
            }
            if (canonical.length() > CounterConfig.MAX_JSON_TITLE_LENGTH) {
                return failure(source, Messages.text(source, Messages.Key.TITLE_TOO_LONG,
                        CounterConfig.MAX_JSON_TITLE_LENGTH));
            }
            if (TitleParser.plainText(canonical).trim().isEmpty()) {
                return failure(source, Messages.text(source, Messages.Key.TITLE_ONLY_FORMAT));
            }
            CounterHook.setTitle(source.getServer(), canonical);
        } else {
            if (title.length() > CounterConfig.MAX_TITLE_LENGTH) {
                return failure(source, Messages.text(source, Messages.Key.TITLE_TOO_LONG,
                        CounterConfig.MAX_TITLE_LENGTH));
            }
            if (TitleParser.plainText(title).trim().isEmpty()) {
                return failure(source, Messages.text(source, Messages.Key.TITLE_ONLY_FORMAT));
            }
            CounterHook.setTitle(source.getServer(), title);
        }

        source.sendSuccess(() -> Component.literal(Messages.text(source, Messages.Key.TITLE_UPDATED))
                .withStyle(ChatFormatting.GREEN)
                .append(CounterHook.config().titleComponent()), true);
        return 1;
    }

    private static int setPosition(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String requested = StringArgumentType.getString(context, "position");
        String position = CounterConfig.normalizePosition(requested);

        if (position == null) {
            return failure(source, Messages.text(source, Messages.Key.POSITION_UNKNOWN, requested));
        }
        if ("sidebar".equals(position) && !CounterHook.config().sidebar) {
            return failure(source, Messages.text(source, Messages.Key.SIDEBAR_BLOCKED));
        }

        CounterHook.setPosition(source.getServer(), position);
        return success(source, Messages.text(source, Messages.Key.POSITION_SET, position));
    }

    private static int init(CommandContext<CommandSourceStack> context, boolean includeOffline) {
        CommandSourceStack source = context.getSource();
        CounterHook.InitResult result = CounterHook.initializeFromStats(source.getServer(), includeOffline);
        return success(source, Messages.text(source, Messages.Key.INIT_RESULT,
                result.onlinePlayers(), result.offlinePlayers(), result.entries(), result.totalMined()));
    }

    private static int setCreative(CommandContext<CommandSourceStack> context, boolean countCreative) {
        CommandSourceStack source = context.getSource();
        CounterHook.setCountCreative(source.getServer(), countCreative);
        return success(source, Messages.text(source,
                countCreative ? Messages.Key.CREATIVE_ON : Messages.Key.CREATIVE_OFF));
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CounterConfig config = CounterHook.reload(source.getServer());
        return success(source, Messages.text(source, Messages.Key.RELOADED,
                config.title, config.position, config.sidebar, config.countCreative));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CounterConfig config = CounterHook.config();
        boolean sidebarBlocked = config.usesSidebar() && !config.sidebar;
        boolean effectiveVisible = config.enabled && config.visible && !sidebarBlocked;

        String counting = Messages.text(source, config.enabled ? Messages.Key.ON : Messages.Key.OFF);
        String visibility = Messages.text(source,
                effectiveVisible ? Messages.Key.SHOWN_LABEL : Messages.Key.HIDDEN_LABEL);
        String sidebarAllowed = Messages.text(source, config.sidebar ? Messages.Key.YES : Messages.Key.NO);
        String blockedSuffix = sidebarBlocked ? Messages.text(source, Messages.Key.BLOCKED_SUFFIX) : "";
        String creative = Messages.text(source, config.countCreative ? Messages.Key.ON : Messages.Key.OFF);

        source.sendSuccess(() -> Component.literal(Messages.text(source, Messages.Key.STATUS_HEADER))
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal(Messages.text(source, Messages.Key.STATUS_COUNTING, counting)), false);
        source.sendSuccess(() -> Component.literal(Messages.text(source, Messages.Key.STATUS_VISIBLE,
                visibility, config.position)), false);
        source.sendSuccess(() -> Component.literal(Messages.text(source, Messages.Key.STATUS_SIDEBAR,
                sidebarAllowed, blockedSuffix)), false);
        source.sendSuccess(() -> Component.literal(Messages.text(source, Messages.Key.STATUS_CREATIVE, creative)), false);
        source.sendSuccess(() -> Component.literal(Messages.text(source, Messages.Key.STATUS_TITLE))
                .append(config.titleComponent()), false);
        source.sendSuccess(() -> Component.literal(Messages.text(source, Messages.Key.STATUS_CONFIG)), false);
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
