package minedcounter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Server side counter.
 *
 * Single objective: minecount_total is the live number, written directly when a
 * block is broken, and shown on the right side as the "挖掘榜" sidebar.
 *
 * Cost: one scoreboard increment per broken block (same order of magnitude as the
 * vanilla statistic update), plus one map lookup per second to keep the sidebar
 * attached. No per-tick player iteration.
 */
final class CounterHook {
    private static final String TOTAL_OBJECTIVE = "minecount_total";
    private static final String LEGACY_POOL_OBJECTIVE = "minecount_pool";
    private static final int DISPLAY_CHECK_TICKS = 20;

    private static Objective cachedObjective;
    private static int tickCounter;

    private CounterHook() {
    }

    static void register() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            Scoreboard scoreboard = level.getScoreboard();
            Objective objective = cachedObjective;
            if (objective == null || scoreboard.getObjective(TOTAL_OBJECTIVE) != objective) {
                objective = ensure(scoreboard);
            }
            if (objective == null) {
                return;
            }
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(serverPlayer, objective);
            score.set(score.get() + 1);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            cachedObjective = null;
            tickCounter = 0;
            setSidebar(server);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            cachedObjective = null;
            tickCounter = 0;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < DISPLAY_CHECK_TICKS) {
                return;
            }
            tickCounter = 0;
            ensure(server.getScoreboard());
        });

        MinedCounter.LOGGER.info("[mined_counter] active: instant counting in objective '{}', sidebar title = 挖掘榜",
                TOTAL_OBJECTIVE);
    }

    private static void setSidebar(MinecraftServer server) {
        Objective objective = ensure(server.getScoreboard());
        if (objective != null) {
            // Applied once per launch, so in-game edits survive until the next start.
            objective.setDisplayName(title());
        }
    }

    private static Objective ensure(Scoreboard scoreboard) {
        Objective objective = scoreboard.getObjective(TOTAL_OBJECTIVE);
        if (objective == null) {
            objective = scoreboard.addObjective(TOTAL_OBJECTIVE, ObjectiveCriteria.DUMMY, title(),
                    ObjectiveCriteria.RenderType.INTEGER, false, null);
        }
        if (objective == null) {
            return null;
        }
        // one-time cleanup of the two-objective layout used by 1.0.0
        Objective legacy = scoreboard.getObjective(LEGACY_POOL_OBJECTIVE);
        if (legacy != null) {
            scoreboard.removeObjective(legacy);
        }
        if (scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) != objective) {
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        }
        cachedObjective = objective;
        return objective;
    }

    private static Component title() {
        return Component.literal("挖掘榜")
                .withStyle(ChatFormatting.YELLOW)
                .withStyle(ChatFormatting.UNDERLINE);
    }
}
