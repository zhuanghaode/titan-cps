package dev.titancps.access;

/**
 * Access to {@code LivingEntity.attackStrengthTicker}, the per-tick counter that decides how much
 * damage a hit deals.
 *
 * <p>The vanilla damage formula is {@code damage *= 0.2 + scale^2 * 0.8}, where
 * {@code scale = attackStrengthTicker / (20 / attackSpeed)} capped at 1. To guarantee a full
 * power hit every time we push the ticker to the current item's full charge value right before
 * attacking.</p>
 */
public interface IChargeAccessor {
    /** Forces the attack strength ticker to a specific value. */
    void titancps$setAttackStrengthTicker(int value);

    /** Reads the current attack strength ticker value. */
    int titancps$getAttackStrengthTicker();
}
