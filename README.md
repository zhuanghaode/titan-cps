# Titan CPS

为泰坦整合包调校的 Minecraft **1.21.4 / NeoForge** 客户端左键连点模组。

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-brightgreen)
![NeoForge](https://img.shields.io/badge/NeoForge-21.4.161-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Side](https://img.shields.io/badge/Side-Client-orange)

> **纯客户端模组。** 放进整合包的 `mods` 文件夹即可，装到服务端上没有任何作用。

[English](#english) · [中文](#简体中文)

---

## 简体中文

### 安装

1. 把 `titancps-1.0.0.jar` 放进整合包的 `mods` 文件夹
2. 启动游戏，首次运行会自动生成 `config/titancps.json`

因新泰坦只有 1.21.4 这一个版本，本模组也只做了这个版本。代码已开源，需要其他版本可以自行编译。

### 按键

| 按键 | 作用 |
|---|---|
| `V` | 启用 / 关闭 |
| `右 Shift` | 打开设置界面 |

**启用不等于连点。** 按 `V` 之后屏幕会提示「已启用 - 按住左键」，此时模组处于待命状态，
**只有按住鼠标左键时才会连点**，松手立即停止。

想要「按 V 就自动连点」的效果，在设置里打开 `始终连点（忽略左键）`。

两个键都可以在 **选项 → 操作 → 按键绑定 → Titan CPS** 里修改。

### 功能

| 选项 | 默认 | 说明 |
|---|---|---|
| 每 tick 点击次数 | 20 | 无实际限制（代码上限一百万），填多少都行 |
| 始终连点（忽略左键） | 关 | 开启后不需要按住左键 |
| 绕过冷却 | 开 | 清掉 `missTime`，否则点击会被吞掉 |
| 锁定满蓄力 | 开 | **影响 DPS 最大的选项** |
| 跳过无敌帧目标 | 开 | 目标处于受击无敌窗口时跳过 |
| 只攻击实体 | 关 | 开启后不会打到方块 |
| 打开界面时继续运行 | 开 | 开着背包也继续 |
| 空手时停止 | 关 | 手上没东西自动停 |

### 原理

原版有两个机制会拖慢输出：

1. **蓄力衰减** —— 伤害按 `0.2 + scale² × 0.8` 结算，没蓄满力只打 20%
2. **受击无敌帧** —— `invulnerableTime > 10` 时服务端直接丢弃攻击

本模组分别用「满蓄力锁」和「无敌帧过滤」绕过。

所以**真正的提升来自每一下都有效，而不只是点击频率**。

另外，`startAttack()` 内部会调用 `player.swing()`，而它每次都会发一个挥手包。
如果循环一千次就会每 tick 发一千个包。本模组改成：
**每 tick 第一个点击走完整流程，后续直接发包不挥手** —— 伤害一样，带宽省掉 99%。

### 性能说明

`clicksPerTick` 是唯一需要关心的性能参数。循环跑在客户端主线程上：

| 数值 | 表现 |
|---|---|
| 10~50 | 流畅 |
| 200~1000 | 流畅，推荐 |
| 2000 | 有明显卡顿 |
| 10000+ | 每 tick 都可能超出预算 |
| 100000 | 直接卡死崩溃 |

连点频率过高时客户端会短暂卡顿，这是主线程循环的正常表现。
建议常态用 **200~1000**，打血厚的 boss 时再临时调高。

### 环境要求

- Minecraft **1.21.4**
- NeoForge **21.4.161** 或更高
- 仅客户端生效

### 构建

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew build        # 产物在 build/libs/titancps-1.0.0.jar
./gradlew runClient    # 开发客户端
```

> `settings.gradle` 里把 ModDevGradle 硬编码的 `maven.neoforged.net` 改写成了
> ForgeCDN 镜像，因为这个域名在国内被 DNS 污染。在国外构建的话这段可以删掉。

### 已验证

- Mixin 配置正常加载
- `MixinMinecraft` 和 `MixinLivingEntity` 运行时注入成功
- `missTime` 读写正常
- 按键判定正确（未启用 / 未按住时不触发）
- 构建产物可正常进入游戏

### 许可证

[MIT](LICENSE) © 2026 zhuanghd

---

<a id="english"></a>

## English

Client-side left-click autoclicker for **Minecraft 1.21.4 / NeoForge**, tuned for the
Titans modpack.

> **Client-side only.** Install it in your modpack's `mods/` folder — it does nothing on a
> dedicated server.

### Install

1. Put `titancps-1.0.0.jar` into your modpack's `mods/` folder.
2. Launch the game. A `config/titancps.json` is generated on first run.

The Titans is a 1.21.4-only modpack, so this mod targets that version. The code is open
source — compile it yourself if you need another version.

### Default keys

| Key | Action |
|---|---|
| `V` | Arm / disarm |
| `Right Shift` | Open the settings screen |

**Arming is not clicking.** After pressing `V` the mod reports `ARMED - hold attack` and stays
idle until you hold down the attack button. Clicks only go out while the left mouse button is
physically held, and they stop the instant you release it.

Set `alwaysOn` in the settings screen if you want arming alone to click continuously.

Both keys are rebindable in **Options → Controls → Key Binds → Titan CPS**.

### How it works

Three independent layers, all configurable in the settings screen.

**L1 — Frequency.** Each client tick, `Minecraft.startAttack()` is invoked `clicksPerTick`
times. Before every call the internal `missTime` gate is cleared, otherwise the vanilla "miss"
timer would swallow the click. There is no meaningful cap; the machine is the limit.

**L2 — Charge lock.** Vanilla damage scales with attack charge:
`damage *= 0.2 + scale² * 0.8`, where `scale = attackStrengthTicker / (20 / attackSpeed)`.
L2 forces `attackStrengthTicker` to the weapon's full-charge value immediately before each
swing, so every hit lands at 100% power regardless of timing.

**L3 — Invulnerability gate.** `LivingEntity.hurtServer()` drops any hit while
`invulnerableTime > 10`. L3 checks the crosshair target and skips the tick when it is inside
that window, so no packets are wasted on hits the server would discard.

### The swing-packet optimisation

`startAttack()` unconditionally calls `player.swing()`, and `LocalPlayer.swing()` sends a
`ServerboundSwingPacket`. Calling `startAttack()` 2000 times per tick would therefore emit
2000 swing packets every tick for zero gameplay benefit.

Titan CPS sends **one full `startAttack()` per tick** (animation included) and routes the
remaining clicks straight to `MultiPlayerGameMode.attack()`, which sends the interact packet
without swinging. Same damage, one animation, a fraction of the bandwidth.

### Performance

The loop runs on the client thread, so this is the only real tuning knob.

| Value | Behaviour |
|---|---|
| 10–50 | Smooth |
| 200–1000 | Smooth, recommended |
| 2000 | Noticeable stutter |
| 10000+ | May exceed the tick budget |
| 100000 | Freezes the client |

### Requirements

- Minecraft 1.21.4
- NeoForge 21.4.161 or newer
- Client-side only

### Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew build        # -> build/libs/titancps-1.0.0.jar
./gradlew runClient    # dev client
```

`settings.gradle` rewrites ModDevGradle's hardcoded `maven.neoforged.net` URLs to the
ForgeCDN mirror, because that host is DNS-poisoned in mainland China. Feel free to drop that
block if you build elsewhere.

### License

[MIT](LICENSE) © 2026 zhuanghd
