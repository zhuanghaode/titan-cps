package dev.titancps;

import dev.titancps.config.CpsConfig;
import dev.titancps.core.CpsController;
import dev.titancps.key.KeyHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Titan CPS - a client-side autoclicker aimed at the Titans modpack.
 *
 * <p>Loads only on the physical client; nothing here runs on a dedicated server.</p>
 */
@Mod(value = TitanCps.MOD_ID, dist = Dist.CLIENT)
public final class TitanCps {
    public static final String MOD_ID = "titancps";
    public static final Logger LOGGER = LoggerFactory.getLogger("TitanCPS");

    public TitanCps(IEventBus modBus, ModContainer container) {
        CpsConfig.getInstance().load(FMLPaths.CONFIGDIR.get());

        modBus.addListener(this::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(KeyHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(CpsController::onClientTick);
        LOGGER.info("Titan CPS loaded.");
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyHandler.TOGGLE_KEY);
        event.register(KeyHandler.CONFIG_KEY);
    }
}
