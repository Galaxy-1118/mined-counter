package minedcounter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point. Fabric API is only a soft dependency: when it is missing the mod
 * stays inactive instead of keeping the server from starting.
 */
public class MinedCounter implements ModInitializer {
    public static final String MOD_ID = "mined_counter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        boolean eventsPresent = loader.isModLoaded("fabric-api")
                || (loader.isModLoaded("fabric-events-interaction-v0")
                && loader.isModLoaded("fabric-lifecycle-events-v1"));
        if (!eventsPresent) {
            LOGGER.warn("[mined_counter] Fabric API not found on this side - mod stays inactive. "
                    + "Add fabric-api to the mods folder (server side) to enable the mined counter.");
            return;
        }

        // Load the config first: commands and the event hooks both read it.
        CounterHook.initialize();

        if (loader.isModLoaded("fabric-api") || loader.isModLoaded("fabric-command-api-v2")) {
            MinedCounterCommand.register();
        } else {
            LOGGER.warn("[mined_counter] fabric-command-api-v2 is missing - /minedcounter commands are disabled.");
        }

        LOGGER.info("[mined_counter] loaded; config file: config/mined_counter.json");
    }
}
