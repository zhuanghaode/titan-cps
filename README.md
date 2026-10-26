# Titan CPS

Client-side left-click autoclicker for **Minecraft 1.21.4 / NeoForge**, tuned for the
Titans modpack.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-brightgreen)
![NeoForge](https://img.shields.io/badge/NeoForge-21.4.161-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

> **Client-side only.** Install it in your modpack's `mods/` folder — it does nothing on a
> dedicated server.

## Install

1. Put `titancps-1.0.0.jar` into your modpack's `mods/` folder.
2. Launch the game. A `config/titancps.json` is generated on first run.

Client-side only: installing it on a dedicated server does nothing.

## Default keys

| Key | Action |
|---|---|
| `V` | Arm / disarm the autoclicker |
| `Right Shift` | Open the settings screen |

**Arming is not clicking.** After pressing `V` the mod reports `ARMED - hold attack` and stays
idle until you hold down the attack button. Clicks only go out while the left mouse button is
physically held, and they stop the instant you release it. Set `alwaysOn` in the settings screen
if you want the old behaviour where arming alone clicks continuously.

Both are rebindable in **Options → Controls → Key Binds** under the *Titan CPS* category.

## How it works

Three independent layers. All are configurable in the settings screen.

### L1 — Frequency
Each client tick, `Minecraft.startAttack()` is invoked `clicksPerTick` times. Before every call
the internal `missTime` gate is cleared, otherwise the vanilla "miss" timer would swallow the
click. There is **no hard cap**; the machine is the limit. Values above `warnThreshold`
(default 2000) trigger an on-screen warning but are never blocked.

### L2 — Charge lock
Vanilla damage scales with attack charge:
`damage *= 0.2 + scale² * 0.8`, where `scale = attackStrengthTicker / (20 / attackSpeed)`.
L2 forces `attackStrengthTicker` to the weapon's full-charge value immediately before each
swing, so every hit lands at 100 % power regardless of timing.

### L3 — Invulnerability gate
`LivingEntity.hurtServer()` drops any hit while `invulnerableTime > 10` (and only applies the
overflow afterwards). L3 checks the crosshair target and skips the tick when it is inside that
window, so no packets are wasted on hits the server would discard.

## The swing-packet optimisation

`startAttack()` unconditionally calls `player.swing()`, and `LocalPlayer.swing()` sends a
`ServerboundSwingPacket`. Calling `startAttack()` 2000 times per tick would therefore emit
2000 swing packets every tick for zero gameplay benefit.

Titan CPS sends **one full `startAttack()` per tick** (animation included) and routes the
remaining clicks straight to `MultiPlayerGameMode.attack()`, which sends the interact packet
without swinging. Same damage, one animation, a fraction of the bandwidth.

## Performance notes

`clicksPerTick` is the only real performance dial.

| Value | Behaviour |
|---|---|
| 10–50 | Snappy, still stable |
| 200–500 | Clearly above humanly possible |
| 2000 | One warning shows; client holds up |
| 10000+ | The loop itself may cost more than a tick; the game will stutter |

The loop runs on the client thread, so an extreme value slows **your** client, not the server's
tick rate. The server is limited separately by its packet rate limits; on multiplayer servers a
very high value may get you kicked.

## Files

```
src/main/java/dev/titancps/
├── TitanCps.java              mod entry point
├── access/                    interfaces cast onto vanilla classes
│   ├── IMinecraftInvoker.java
│   └── IChargeAccessor.java
├── config/CpsConfig.java      hand-rolled JSON config
├── core/CpsController.java    L1 / L2 / L3 engine
├── gui/CpsConfigScreen.java   settings screen
├── key/KeyHandler.java        hotkeys + status line
└── mixin/                     @Shadow plumbing
    ├── MixinMinecraft.java
    └── MixinLivingEntity.java
```

## Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
gradle build          # -> build/libs/titancps-1.0.0.jar
gradle runClient      # dev client
```

`settings.gradle` rewrites ModDevGradle's hardcoded `maven.neoforged.net` URLs to the
ForgeCDN mirror, because that host is DNS-poisoned in mainland China.

## Verified

- Mixin config loads (`Compatibility level set to JAVA_21`).
- `MixinMinecraft` and `MixinLivingEntity` both apply at runtime (verified via an in-client
  self-test: `Minecraft mixin applied: true`, `LivingEntity charge mixin applied: true`).
- `missTime` accessor round-trips (`10000 -> 0`).
- Builds and reaches the main menu with no errors.
- Input gate: `keyAttack.isDown()` is queryable at runtime, and the gate returns
  `false` when disarmed or when the button is not held, `true` only when armed and held
  (verified in-client).

## License

[MIT](LICENSE) © 2026 zhuanghd
