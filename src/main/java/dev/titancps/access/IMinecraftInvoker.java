package dev.titancps.access;

/**
 * Duck-typed access to the private bits of {@code net.minecraft.client.Minecraft} that we need.
 *
 * <p>{@code startAttack()} is private, {@code missTime} is protected and gates every click. Both
 * are exposed here by {@link MixinMinecraft}.</p>
 */
public interface IMinecraftInvoker {
    /** Invokes {@code Minecraft.startAttack()} directly, bypassing the mouse-button path. */
    boolean titancps$startAttack();

    /** Gets the internal miss timer that blocks clicks after a miss. */
    int titancps$getMissTime();

    /** Clears the miss timer so the next click is not swallowed. */
    void titancps$setMissTime(int value);
}
