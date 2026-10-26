package dev.titancps.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.titancps.TitanCps;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tiny hand-rolled JSON config so the mod has zero third-party dependencies.
 *
 * <p>Every field is public and mutable; the GUI and the hotkey handler write to it directly and
 * call {@link #save()} afterwards.</p>
 */
public final class CpsConfig {
    private static final Logger LOGGER = TitanCps.LOGGER;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final CpsConfig INSTANCE = new CpsConfig();

    private Path file;

    // ---- runtime state -------------------------------------------------
    /** Whether the autoclicker is currently running. Toggled by the hotkey. */
    public transient boolean active = false;

    // ---- persisted settings -------------------------------------------
    /** How many {@code startAttack()} calls to make per client tick. */
    public int clicksPerTick = 20;
    /** L1: skip attack cooldown / miss-time throttling so every call really fires. */
    public boolean bypassCooldown = true;
    /** L2: force the attack strength ticker to full before attacking (full damage every hit). */
    public boolean lockCharge = true;
    /** L3: do not send attacks at entities that are inside their hurt-immunity window. */
    public boolean respectInvulnerability = true;
    /** Stop spamming once the main hand is empty. */
    public boolean stopWhenEmptyHand = false;
    /** Only attack entities, never blocks. */
    public boolean entityOnly = false;
    /** Keep clicking while a container / chat screen is open. */
    public boolean runWithGuiOpen = true;
    /** Stop the autoclicker as soon as any GUI opens. */
    public boolean stopOnGuiOpen = false;
    /** Warn (visually only, never blocks) once clicksPerTick exceeds this value. */
    public int warnThreshold = 2000;
    /** Continue clicking without holding the attack button (true) or only while it is held (false). */
    public boolean alwaysOn = false;

    private CpsConfig() {
    }

    public static CpsConfig getInstance() {
        return INSTANCE;
    }

    public void load(Path configDir) {
        this.file = configDir.resolve("titancps.json");
        if (!Files.exists(this.file)) {
            this.save();
            return;
        }
        try {
            String text = Files.readString(this.file);
            JsonObject obj = JsonParser.parseString(text).getAsJsonObject();

            this.clicksPerTick = getInt(obj, "clicksPerTick", this.clicksPerTick);
            this.bypassCooldown = getBool(obj, "bypassCooldown", this.bypassCooldown);
            this.lockCharge = getBool(obj, "lockCharge", this.lockCharge);
            this.respectInvulnerability = getBool(obj, "respectInvulnerability", this.respectInvulnerability);
            this.stopWhenEmptyHand = getBool(obj, "stopWhenEmptyHand", this.stopWhenEmptyHand);
            this.entityOnly = getBool(obj, "entityOnly", this.entityOnly);
            this.runWithGuiOpen = getBool(obj, "runWithGuiOpen", this.runWithGuiOpen);
            this.stopOnGuiOpen = getBool(obj, "stopOnGuiOpen", this.stopOnGuiOpen);
            this.warnThreshold = getInt(obj, "warnThreshold", this.warnThreshold);
            this.alwaysOn = getBool(obj, "alwaysOn", this.alwaysOn);

            this.sanitize();
        } catch (Exception e) {
            LOGGER.warn("Failed to read titancps.json, using defaults", e);
        }
    }

    public void save() {
        if (this.file == null) {
            return;
        }
        try {
            this.sanitize();
            JsonObject obj = new JsonObject();
            obj.addProperty("clicksPerTick", this.clicksPerTick);
            obj.addProperty("bypassCooldown", this.bypassCooldown);
            obj.addProperty("lockCharge", this.lockCharge);
            obj.addProperty("respectInvulnerability", this.respectInvulnerability);
            obj.addProperty("stopWhenEmptyHand", this.stopWhenEmptyHand);
            obj.addProperty("entityOnly", this.entityOnly);
            obj.addProperty("runWithGuiOpen", this.runWithGuiOpen);
            obj.addProperty("stopOnGuiOpen", this.stopOnGuiOpen);
            obj.addProperty("warnThreshold", this.warnThreshold);
            obj.addProperty("alwaysOn", this.alwaysOn);

            Files.createDirectories(this.file.getParent());
            Files.writeString(this.file, GSON.toJson(obj));
        } catch (IOException e) {
            LOGGER.error("Failed to write titancps.json", e);
        }
    }

    public void sanitize() {
        // No hard cap on purpose: the user asked for the machine's limit. We only guard against
        // nonsensical values that would break arithmetic or loop forever.
        this.clicksPerTick = Math.max(1, Math.min(this.clicksPerTick, 1_000_000));
        this.warnThreshold = Math.max(1, this.warnThreshold);
    }

    /** True when the current click rate is above the (advisory) warning threshold. */
    public boolean isAboveWarnThreshold() {
        return this.clicksPerTick > this.warnThreshold;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsInt() : def;
    }

    private static boolean getBool(JsonObject obj, String key, boolean def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsBoolean() : def;
    }
}
