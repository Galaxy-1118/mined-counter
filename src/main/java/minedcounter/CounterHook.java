package minedcounter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Server-side counter and scoreboard lifecycle.
 *
 * <p>The mod owns one dummy objective, {@code minedcounter_total}. A block
 * break increments the breaking player's score directly, so there is no
 * per-tick player iteration. A light task runs once per second to keep the
 * configured display slot attached after another mod or an admin changes it.</p>
 */
public final class CounterHook {
    private static final String TOTAL_OBJECTIVE = "minedcounter_total";
    private static final String LEGACY_TOTAL_OBJECTIVE = "minecount_total";
    private static final String LEGACY_POOL_OBJECTIVE = "minecount_pool";
    private static final int DISPLAY_CHECK_TICKS = 20;
    private static final DisplaySlot[] MANAGED_SLOTS = {
            DisplaySlot.SIDEBAR,
            DisplaySlot.LIST,
            DisplaySlot.BELOW_NAME
    };

    private static CounterConfig config = new CounterConfig();
    private static boolean initialized;
    private static int tickCounter;
    private static String appliedTitle;

    private CounterHook() {
    }

    static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        config = CounterConfig.load();

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!config.enabled || level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            boolean creative = serverPlayer.isCreative();
            if (creative && !config.countCreative) {
                return;
            }
            // Vanilla awards Stats.BLOCK_MINED from Block.playerDestroy, which
            // ServerPlayerGameMode only calls when the held item is the correct
            // tool for drops. Match that condition so /minedcounter init and the
            // live counter describe the same set of blocks.
            if (!creative && !serverPlayer.hasCorrectToolForDrops(state)) {
                return;
            }
            Scoreboard scoreboard = level.getScoreboard();
            migrateLegacyObjectives(scoreboard);
            Objective objective = scoreboard.getObjective(TOTAL_OBJECTIVE);
            if (objective == null) {
                objective = createObjective(scoreboard);
                if (objective != null) {
                    updateDisplay(scoreboard, objective);
                }
            }
            if (objective == null) {
                return;
            }
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(serverPlayer, objective);
            int current = score.get();
            score.set(current == Integer.MAX_VALUE ? Integer.MAX_VALUE : current + 1);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            tickCounter = 0;
            appliedTitle = null;
            apply(server);
            MinedCounter.LOGGER.info(
                    "[mined_counter] active: objective={}, title='{}', position={}, enabled={}, visible={}, sidebar={}, countCreative={}",
                    TOTAL_OBJECTIVE, config.title, config.position, config.enabled, config.visible,
                    config.sidebar, config.countCreative);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            tickCounter = 0;
            appliedTitle = null;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < DISPLAY_CHECK_TICKS) {
                return;
            }
            tickCounter = 0;
            apply(server);
        });
    }

    static CounterConfig config() {
        return config;
    }

    static void setEnabled(MinecraftServer server, boolean enabled) {
        config.enabled = enabled;
        config.save();
        apply(server);
    }

    static void setVisible(MinecraftServer server, boolean visible) {
        config.visible = visible;
        config.save();
        apply(server);
    }

    static void setTitle(MinecraftServer server, String title) {
        config.title = title;
        config.save();
        appliedTitle = null;
        apply(server);
    }

    static void setPosition(MinecraftServer server, String position) {
        config.position = position;
        config.save();
        apply(server);
    }

    static void setCountCreative(MinecraftServer server, boolean countCreative) {
        config.countCreative = countCreative;
        config.save();
    }

    static CounterConfig reload(MinecraftServer server) {
        config = CounterConfig.load();
        appliedTitle = null;
        apply(server);
        return config;
    }

    /**
     * Rebuilds the objective from vanilla statistics. Online players are read
     * from their live counters; when {@code includeOffline} is true, cached
     * player statistics under {@code world/stats} are read as well. Reading many
     * offline files is a one-time I/O cost, hence the {@code init online} escape
     * hatch for very large servers.
     */
    static InitResult initializeFromStats(MinecraftServer server, boolean includeOffline) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective previous = scoreboard.getObjective(TOTAL_OBJECTIVE);
        if (previous != null) {
            scoreboard.removeObjective(previous);
        }
        Objective legacyTotal = scoreboard.getObjective(LEGACY_TOTAL_OBJECTIVE);
        if (legacyTotal != null) {
            scoreboard.removeObjective(legacyTotal);
        }

        appliedTitle = null;
        Objective objective = createObjective(scoreboard);
        if (objective == null) {
            return new InitResult(0, 0, 0, 0L);
        }

        Map<UUID, String> knownNames = includeOffline ? readUserCache(server) : Map.of();
        Set<UUID> onlineIds = new HashSet<>();
        int online = 0;
        int offline = 0;
        int entries = 0;
        long totalMined = 0L;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online++;
            onlineIds.add(player.getUUID());
            long mined = sumBlockMined(player.getStats());
            totalMined += mined;
            if (mined > 0L) {
                scoreboard.getOrCreatePlayerScore(player, objective)
                        .set((int) Math.min(mined, Integer.MAX_VALUE));
                entries++;
            }
        }

        if (includeOffline) {
            Path statsDirectory = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
            if (Files.isDirectory(statsDirectory)) {
                try (Stream<Path> files = Files.list(statsDirectory)) {
                    for (Path file : files.filter(CounterHook::isStatsFile).toList()) {
                        UUID id = statsFileUuid(file);
                        if (id == null || onlineIds.contains(id)) {
                            continue;
                        }
                        String name = knownNames.get(id);
                        if (name == null) {
                            continue;
                        }
                        offline++;
                        try {
                            long mined = sumBlockMined(new ServerStatsCounter(server, file));
                            totalMined += mined;
                            if (mined > 0L) {
                                scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), objective)
                                        .set((int) Math.min(mined, Integer.MAX_VALUE));
                                entries++;
                            }
                        } catch (RuntimeException exception) {
                            MinedCounter.LOGGER.warn("[mined_counter] could not read stats file {}", file, exception);
                        }
                    }
                } catch (IOException exception) {
                    MinedCounter.LOGGER.warn("[mined_counter] could not scan {}", statsDirectory, exception);
                }
            }
        }

        updateDisplay(scoreboard, objective);
        return new InitResult(online, offline, entries, totalMined);
    }

    private static long sumBlockMined(StatsCounter counter) {
        long mined = 0L;
        for (Block block : BuiltInRegistries.BLOCK) {
            mined += counter.getValue(Stats.BLOCK_MINED, block);
        }
        return mined;
    }

    private static boolean isStatsFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".json") && name.length() > ".json".length();
    }

    private static UUID statsFileUuid(Path path) {
        String name = path.getFileName().toString();
        try {
            return UUID.fromString(name.substring(0, name.length() - ".json".length()));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Map<UUID, String> readUserCache(MinecraftServer server) {
        Map<UUID, String> names = new HashMap<>();
        Path file = server.getServerDirectory().resolve("usercache.json");
        if (!Files.isRegularFile(file)) {
            return names;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) {
                return names;
            }
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                try {
                    NameAndId entry = NameAndId.fromJson(element.getAsJsonObject());
                    if (entry.name() != null && !entry.name().isBlank()) {
                        names.put(entry.id(), entry.name());
                    }
                } catch (RuntimeException ignored) {
                    // One malformed cache entry should not abort the whole import.
                }
            }
        } catch (Exception exception) {
            MinedCounter.LOGGER.warn("[mined_counter] could not read {}", file, exception);
        }
        return names;
    }

    private static void apply(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        migrateLegacyObjectives(scoreboard);
        Objective objective = scoreboard.getObjective(TOTAL_OBJECTIVE);

        // Do not create a fresh objective for a disabled, hidden counter; the
        // admin may simply have removed it on purpose.
        if (objective == null && (config.enabled || config.visible)) {
            objective = createObjective(scoreboard);
        }
        if (objective == null) {
            return;
        }

        if (!config.title.equals(appliedTitle)) {
            objective.setDisplayName(config.titleComponent());
            appliedTitle = config.title;
        }

        updateDisplay(scoreboard, objective);
    }

    /**
     * Copies scores from the 1.1.0/early-1.2.0 objective name and removes the
     * old 1.0.0 pool objective. The new objective keeps all existing entries so
     * the rename does not reset a server's leaderboard.
     */
    private static void migrateLegacyObjectives(Scoreboard scoreboard) {
        Objective legacyTotal = scoreboard.getObjective(LEGACY_TOTAL_OBJECTIVE);
        if (legacyTotal != null) {
            Objective current = scoreboard.getObjective(TOTAL_OBJECTIVE);
            if (current == null) {
                current = scoreboard.addObjective(
                        TOTAL_OBJECTIVE,
                        ObjectiveCriteria.DUMMY,
                        config.titleComponent(),
                        ObjectiveCriteria.RenderType.INTEGER,
                        false,
                        null);
                if (current != null) {
                    appliedTitle = config.title;
                }
            }
            if (current != null) {
                for (PlayerScoreEntry entry : scoreboard.listPlayerScores(legacyTotal)) {
                    ScoreHolder holder = ScoreHolder.forNameOnly(entry.owner());
                    ReadOnlyScoreInfo existing = scoreboard.getPlayerScoreInfo(holder, current);
                    if (existing == null) {
                        scoreboard.getOrCreatePlayerScore(holder, current).set(entry.value());
                    }
                }
            }
            scoreboard.removeObjective(legacyTotal);
        }

        Objective legacyPool = scoreboard.getObjective(LEGACY_POOL_OBJECTIVE);
        if (legacyPool != null) {
            scoreboard.removeObjective(legacyPool);
        }
    }

    private static Objective createObjective(Scoreboard scoreboard) {
        Objective objective = scoreboard.addObjective(
                TOTAL_OBJECTIVE,
                ObjectiveCriteria.DUMMY,
                config.titleComponent(),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null);
        if (objective != null) {
            appliedTitle = config.title;
        }
        return objective;
    }

    private static void updateDisplay(Scoreboard scoreboard, Objective objective) {
        DisplaySlot slot = config.displaySlot();
        boolean allowed = config.enabled
                && config.visible
                && (slot != DisplaySlot.SIDEBAR || config.sidebar);

        if (!allowed) {
            detach(scoreboard, objective);
            return;
        }

        for (DisplaySlot managed : MANAGED_SLOTS) {
            if (managed != slot && scoreboard.getDisplayObjective(managed) == objective) {
                scoreboard.setDisplayObjective(managed, null);
            }
        }
        if (scoreboard.getDisplayObjective(slot) != objective) {
            scoreboard.setDisplayObjective(slot, objective);
        }
    }

    private static void detach(Scoreboard scoreboard, Objective objective) {
        for (DisplaySlot managed : MANAGED_SLOTS) {
            if (scoreboard.getDisplayObjective(managed) == objective) {
                scoreboard.setDisplayObjective(managed, null);
            }
        }
    }

    record InitResult(int onlinePlayers, int offlinePlayers, int entries, long totalMined) {
    }
}
