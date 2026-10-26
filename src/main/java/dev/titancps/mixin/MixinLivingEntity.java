package dev.titancps.mixin;

import dev.titancps.access.IChargeAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity implements IChargeAccessor {
    @Shadow
    protected int attackStrengthTicker;

    @Override
    public void titancps$setAttackStrengthTicker(int value) {
        this.attackStrengthTicker = value;
    }

    @Override
    public int titancps$getAttackStrengthTicker() {
        return this.attackStrengthTicker;
    }
}
