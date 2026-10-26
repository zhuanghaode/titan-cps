package dev.titancps.mixin;

import dev.titancps.access.IMinecraftInvoker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements IMinecraftInvoker {
    @Shadow
    protected int missTime;

    @Shadow
    private boolean startAttack() {
        throw new AssertionError();
    }

    @Override
    public boolean titancps$startAttack() {
        return this.startAttack();
    }

    @Override
    public int titancps$getMissTime() {
        return this.missTime;
    }

    @Override
    public void titancps$setMissTime(int value) {
        this.missTime = value;
    }
}
