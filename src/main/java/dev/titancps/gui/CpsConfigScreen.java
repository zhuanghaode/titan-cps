package dev.titancps.gui;

import dev.titancps.config.CpsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Standalone settings screen. Deliberately dependency-free (no Cloth Config, no MaLiLib) so it
 * keeps working no matter what else is in the pack.
 */
public final class CpsConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int WIDTH = 300;

    private final Screen parent;
    private final CpsConfig cfg = CpsConfig.getInstance();

    private EditBox clicksBox;
    private int top;

    public CpsConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.titancps.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int x = this.width / 2 - WIDTH / 2;
        int y = this.height / 2 - 120;
        this.top = y;

        // --- clicks per tick ---
        this.clicksBox = new EditBox(this.font, x, y, WIDTH, 20, Component.literal("clicks/tick"));
        this.clicksBox.setMaxLength(9);
        this.clicksBox.setValue(Integer.toString(this.cfg.clicksPerTick));
        this.clicksBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,9}"));
        this.addRenderableWidget(this.clicksBox);
        y += ROW_HEIGHT;

        this.addRenderableWidget(Button.builder(
                        label("alwaysOn", this.cfg.alwaysOn),
                        b -> {
                            this.cfg.alwaysOn = !this.cfg.alwaysOn;
                            b.setMessage(label("alwaysOn", this.cfg.alwaysOn));
                        })
                .bounds(x, y, WIDTH, 20).build());
        y += ROW_HEIGHT;

        this.addRenderableWidget(Button.builder(
                        label("bypassCooldown", this.cfg.bypassCooldown),
                        b -> {
                            this.cfg.bypassCooldown = !this.cfg.bypassCooldown;
                            b.setMessage(label("bypassCooldown", this.cfg.bypassCooldown));
                        })
                .bounds(x, y, WIDTH, 20).build());
        y += ROW_HEIGHT;

        this.addRenderableWidget(Button.builder(
                        label("lockCharge", this.cfg.lockCharge),
                        b -> {
                            this.cfg.lockCharge = !this.cfg.lockCharge;
                            b.setMessage(label("lockCharge", this.cfg.lockCharge));
                        })
                .bounds(x, y, WIDTH, 20).build());
        y += ROW_HEIGHT;

        this.addRenderableWidget(Button.builder(
                        label("respectInvulnerability", this.cfg.respectInvulnerability),
                        b -> {
                            this.cfg.respectInvulnerability = !this.cfg.respectInvulnerability;
                            b.setMessage(label("respectInvulnerability", this.cfg.respectInvulnerability));
                        })
                .bounds(x, y, WIDTH, 20).build());
        y += ROW_HEIGHT;

        this.addRenderableWidget(Button.builder(
                        label("entityOnly", this.cfg.entityOnly),
                        b -> {
                            this.cfg.entityOnly = !this.cfg.entityOnly;
                            b.setMessage(label("entityOnly", this.cfg.entityOnly));
                        })
                .bounds(x, y, WIDTH, 20).build());
        y += ROW_HEIGHT;

        this.addRenderableWidget(Button.builder(
                        label("runWithGuiOpen", this.cfg.runWithGuiOpen),
                        b -> {
                            this.cfg.runWithGuiOpen = !this.cfg.runWithGuiOpen;
                            b.setMessage(label("runWithGuiOpen", this.cfg.runWithGuiOpen));
                        })
                .bounds(x, y, WIDTH, 20).build());
        y += ROW_HEIGHT;

        this.addRenderableWidget(Button.builder(
                        label("stopWhenEmptyHand", this.cfg.stopWhenEmptyHand),
                        b -> {
                            this.cfg.stopWhenEmptyHand = !this.cfg.stopWhenEmptyHand;
                            b.setMessage(label("stopWhenEmptyHand", this.cfg.stopWhenEmptyHand));
                        })
                .bounds(x, y, WIDTH, 20).build());
        y += ROW_HEIGHT + 8;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(x, y, WIDTH, 20).build());
    }

    private static Component label(String key, boolean enabled) {
        return Component.translatable("gui.titancps." + key)
                .append(Component.literal(": "))
                .append(Component.translatable(enabled ? "gui.titancps.on" : "gui.titancps.off"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.top - 24, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.titancps.clicksPerTick"),
                this.width / 2 - WIDTH / 2, this.top - 12, 0xA0A0A0, false);

        if (this.cfg.isAboveWarnThreshold()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.titancps.warn", this.cfg.warnThreshold),
                    this.width / 2, this.top + 150, 0xFF5555);
        }
    }

    @Override
    public void onClose() {
        int parsed;
        try {
            parsed = Integer.parseInt(this.clicksBox.getValue().trim());
        } catch (NumberFormatException e) {
            parsed = this.cfg.clicksPerTick;
        }
        this.cfg.clicksPerTick = Math.max(1, parsed);
        this.cfg.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
