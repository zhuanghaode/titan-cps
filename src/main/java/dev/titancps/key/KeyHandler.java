package dev.titancps.key;

import com.mojang.blaze3d.platform.InputConstants;
import dev.titancps.config.CpsConfig;
import dev.titancps.core.CpsController;
import dev.titancps.gui.CpsConfigScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Key handling plus the on-screen status line.
 *
 * <p>Keys are polled with {@code consumeClick()} because the 1.21.4 key-mapping API no longer
 * delivers a "pressed this tick" edge to listeners.</p>
 */
public final class KeyHandler {
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.titancps.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.titancps");

    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.titancps.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.titancps");

    private static long lastWarnTick = -1000L;

    private KeyHandler() {
    }

    public static KeyMapping getToggleKey() {
        return TOGGLE_KEY;
    }

    public static KeyMapping getConfigKey() {
        return CONFIG_KEY;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        CpsConfig cfg = CpsConfig.getInstance();

        while (TOGGLE_KEY.consumeClick()) {
            if (mc.screen == null) {
                CpsController.toggle();
                flashStatus(mc, cfg);
            }
        }

        while (CONFIG_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new CpsConfigScreen(mc.screen));
            }
        }

        // A screen opening cancels the autoclicker when the user asked for that.
        if (cfg.stopOnGuiOpen && mc.screen != null && cfg.active) {
            CpsController.setActive(false);
        }

        // Advisory warning only; never blocks the user. Rate limited so it does not spam.
        if (cfg.active && cfg.isAboveWarnThreshold()) {
            long now = mc.level != null ? mc.level.getGameTime() : 0L;
            if (now - lastWarnTick > 100L) {
                lastWarnTick = now;
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            Component.literal("[TitanCPS] ")
                                    .append(Component.literal(cfg.clicksPerTick + " clicks/tick")
                                            .withStyle(ChatFormatting.YELLOW))
                                    .append(Component.literal(" is above the safe threshold ("
                                                    + cfg.warnThreshold + ")")
                                            .withStyle(ChatFormatting.RED)),
                            true);
                }
            }
        }
    }

    private static void flashStatus(Minecraft mc, CpsConfig cfg) {
        if (mc.player == null) {
            return;
        }
        Component text;
        if (cfg.active) {
            text = Component.literal("[TitanCPS] ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable(cfg.alwaysOn
                            ? "titancps.status.always"
                            : "titancps.status.armed"))
                    .append(Component.literal("  " + cfg.clicksPerTick + "/tick")
                            .withStyle(cfg.isAboveWarnThreshold() ? ChatFormatting.RED : ChatFormatting.GRAY));
        } else {
            text = Component.literal("[TitanCPS] ").withStyle(ChatFormatting.RED)
                    .append(Component.translatable("titancps.status.disarmed"));
        }
        mc.player.displayClientMessage(text, true);
    }
}
