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
        if (!fabricApiPresent()) {
            LOGGER.warn("[mined_counter] Fabric API not found on this side - mod stays inactive. "
                    + "Add fabric-api to the mods folder (server side) to enable the mined counter.");
            return;
        }
        // Deliberately in a separate class: CounterHook touches Fabric API types,
        // so it must not be loaded when the API is absent.
        CounterHook.register();
    }

    private static boolean fabricApiPresent() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("fabric-api")
                || loader.isModLoaded("fabric-events-interaction-v0")
                || loader.isModLoaded("fabric-lifecycle-events-v1");
    }
}
