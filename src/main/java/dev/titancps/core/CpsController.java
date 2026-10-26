package dev.titancps.core;

import dev.titancps.TitanCps;
import dev.titancps.config.CpsConfig;
import dev.titancps.access.IChargeAccessor;
import dev.titancps.access.IMinecraftInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The autoclicker engine.
 *
 * <p>Three layers, each independently toggleable:</p>
 * <ul>
 *   <li><b>L1 frequency</b> - call {@code Minecraft.startAttack()} {@code clicksPerTick} times per
 *       tick, clearing {@code missTime} first so the internal click gate never swallows a call.</li>
 *   <li><b>L2 charge lock</b> - force {@code attackStrengthTicker} to the current weapon's full
 *       charge value before every swing, so the {@code 0.2 + scale^2 * 0.8} damage penalty never
 *       applies.</li>
 *   <li><b>L3 invulnerability gate</b> - skip entities that are inside their hurt-immunity window
 *       ({@code Entity.invulnerableTime > 10}). Sending those packets is pure waste: the server
 *       drops them in {@code LivingEntity.hurtServer()}.</li>
 * </ul>
 *
 * <h2>Why the loop is not a naive repeat</h2>
 * <p>{@code startAttack()} unconditionally calls {@code player.swing()}, and
 * {@code LocalPlayer.swing()} sends a {@code ServerboundSwingPacket}. Calling it 2000 times per
 * tick would emit 2000 swing packets every tick (~40k/s) for no gameplay benefit. The first click
 * of a tick is therefore a full {@code startAttack()} (swing included), while every following click
 * goes straight to {@code MultiPlayerGameMode.attack()}, which sends the interact packet but does
 * not swing. That keeps one animation per tick and one full attack per loop iteration.</p>
 */
public final class CpsController {
    /** Vanilla immunity window; {@code hurtServer()} drops hits while above this. */
    private static final int INVULNERABLE_WINDOW = 10;

    private CpsController() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        CpsConfig cfg = CpsConfig.getInstance();
        Minecraft mc = Minecraft.getInstance();

        if (!cfg.active || mc.level == null || mc.player == null || mc.gameMode == null) {
            return;
        }
        if (mc.screen != null && !cfg.runWithGuiOpen) {
            return;
        }
        // The toggle key only arms the feature (or forces continuous clicking when alwaysOn is set).
        // By default the autoclicker fires only while the physical attack button is held down.
        if (!cfg.alwaysOn && !mc.options.keyAttack.isDown()) {
            return;
        }
        if (mc.player.isHandsBusy()) {
            return;
        }
        if (cfg.stopWhenEmptyHand && mc.player.getMainHandItem().isEmpty()) {
            setActive(false);
            return;
        }

        HitResult hit = mc.hitResult;

        // Bail out before doing any work when the crosshair is pointed at something we must not hit.
        if (cfg.entityOnly && (hit == null || hit.getType() != HitResult.Type.ENTITY)) {
            return;
        }
        boolean entityTarget = hit instanceof EntityHitResult entityHit
                && isDamagableNow(entityHit.getEntity(), cfg.respectInvulnerability);
        // With respectInvulnerability on and a blocked entity we skip the whole tick.
        if (cfg.respectInvulnerability && hit != null && hit.getType() == HitResult.Type.ENTITY && !entityTarget) {
            return;
        }

        int count = Math.max(1, cfg.clicksPerTick);
        IMinecraftInvoker invoker = (IMinecraftInvoker) mc;
        IChargeAccessor charge = (IChargeAccessor) mc.player;

        // L2: computed once per tick, not per click.
        int fullCharge = cfg.lockCharge ? computeFullCharge(mc.player) : -1;

        for (int i = 0; i < count; i++) {
            if (cfg.bypassCooldown && invoker.titancps$getMissTime() > 0) {
                invoker.titancps$setMissTime(0);
            }

            if (fullCharge >= 0) {
                charge.titancps$setAttackStrengthTicker(fullCharge);
            }

            if (i == 0) {
                // First click of the tick: full path, includes hand swing animation + packet.
                if (invoker.titancps$startAttack()) {
                    break;
                }
            } else {
                // Subsequent clicks: send the attack packet without spamming swing packets.
                if (!attackSilently(mc, hit)) {
                    break;
                }
            }
        }
    }

    /**
     * Attacks the current crosshair target the way {@code startAttack()} does, minus the hand
     * swing. Returns false when the target can no longer be attacked.
     */
    private static boolean attackSilently(Minecraft mc, HitResult hit) {
        if (hit == null || mc.player == null || mc.gameMode == null) {
            return false;
        }
        if (mc.player.isHandsBusy()) {
            return false;
        }
        switch (hit.getType()) {
            case ENTITY -> {
                Entity target = ((EntityHitResult) hit).getEntity();
                if (target == null || !target.isAlive()) {
                    return false;
                }
                mc.gameMode.attack(mc.player, target);
                return true;
            }
            case BLOCK -> {
                // Re-issuing block destroy every click would flood START_DESTROY_BLOCK packets.
                // Vanilla only re-sends every 5 ticks (destroyDelay), which is plenty.
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Returns true when the entity can actually take damage right now.
     *
     * <p>Mirrors the vanilla gate in {@code LivingEntity.hurtServer}: with
     * {@code invulnerableTime > 10} the hit is dropped entirely (or only the overflow is applied),
     * so there is no point in sending it.</p>
     */
    private static boolean isDamagableNow(Entity entity, boolean respectInvulnerability) {
        if (!(entity instanceof LivingEntity living)) {
            // Armour stands and other non-living entities have no immunity window.
            return true;
        }
        if (living.isDeadOrDying()) {
            return false;
        }
        return !respectInvulnerability || living.invulnerableTime <= INVULNERABLE_WINDOW;
    }

    /**
     * Full charge tick count for the item currently in the main hand:
     * {@code 20 / attackSpeed}. A result of {@code -1} means "do not touch the ticker".
     */
    private static int computeFullCharge(LocalPlayer player) {
        double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        if (attackSpeed <= 0.0D) {
            return 0;
        }
        int ticks = (int) Math.ceil(20.0D / attackSpeed);
        return Math.max(0, Math.min(ticks, 20_000));
    }

    public static void setActive(boolean active) {
        CpsConfig cfg = CpsConfig.getInstance();
        if (cfg.active == active) {
            return;
        }
        cfg.active = active;
        if (active) {
            TitanCps.LOGGER.info("Titan CPS enabled ({} clicks/tick)", cfg.clicksPerTick);
        } else {
            TitanCps.LOGGER.info("Titan CPS disabled");
        }
    }

    public static void toggle() {
        setActive(!CpsConfig.getInstance().active);
    }
}
