# Mined Counter / 挖掘榜

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62B47A?style=flat-square)](https://minecraft.net)
[![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.19.2%2B-1976D2?style=flat-square)](https://fabricmc.net)
[![Environment](https://img.shields.io/badge/Environment-Server-6E6E6E?style=flat-square)](#安装)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

一个轻量的 **Fabric 服务端模组**：统计每位玩家挖掘方块的总次数，并在屏幕右侧的计分板实时显示“挖掘榜”。

目标版本：**Minecraft 26.1.2** + **Fabric Loader 0.19.2**，Java 21+。

> 当前版本 **1.1.0**。下载：[Releases](https://github.com/Galaxy-1118/mined-counter/releases/latest)

---

## 为什么做这个模组

原版计分板只能按**方块种类**统计，例如：

```mcfunction
scoreboard objectives add stone_mined minecraft.mined:minecraft.stone
```

想统计“所有方块一共挖了多少”，原版没有现成准则。常见做法是为每种方块建一个计分项再用函数求和，或者用数据包定时轮询——配置麻烦，种类多了还会明显增加开销。

这个模组换了个做法：监听方块破坏事件，**在方块被挖掉的瞬间直接给玩家 +1**。只有一个计分项，不需要数据包、不需要指令、不需要配置文件。

## 特性

- **即时统计**：方块破坏后立刻 +1，不是每分钟轮询。
- **零配置**：放进 `mods` 重启服务器，右侧自动出现“挖掘榜”。
- **纯服务端**：专用服务器只需装在服务端；原版客户端不用装也能看到边栏。
- **单人可用**：单人游戏把模组装在客户端即可（ integrated server 会加载它）。
- **轻量**：约 4 KB；每 20 tick 只做一次计分板指向检查，不遍历玩家。
- **软依赖 Fabric API**：Fabric API 缺失时自动停用并打印警告，不会导致服务器启动失败。
- **自动迁移**：启动时清理 1.0.0 遗留的 `minecount_pool` 计分项。
- **MIT 开源**：约 150 行 Java，方便阅读和二次修改。

## 安装

1. 安装 **Minecraft 26.1.2** 和 **Fabric Loader 0.19.2+**（[Fabric 安装器](https://fabricmc.net/use/)）。
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api)（选 `26.1.2` 版本）。
3. 把 `mined-counter-1.1.0.jar` 放进服务器的 `mods` 文件夹。
4. 重启服务器。

目录结构示例：

```text
server/
└── mods/
    ├── fabric-api-0.152.1+26.1.2.jar
    └── mined-counter-1.1.0.jar
```

启动成功后，日志里会出现：

```text
[mined_counter] active: instant counting in objective 'minecount_total', sidebar title = 挖掘榜
```

## 使用

启动后，右侧边栏会自动显示黄色下划线的“挖掘榜”，按分数从高到低排列。

计分项名称是 `minecount_total`，数据保存在原版计分板里，重启不会丢失。

常用指令：

```mcfunction
# 查看计分项
/scoreboard objectives list

# 重置某位玩家
/scoreboard players reset <玩家> minecount_total

# 重置所有玩家
/scoreboard players reset * minecount_total

# 隐藏边栏（注意：模组每秒会重新挂载，见“已知限制”）
/scoreboard objectives setdisplay sidebar
```

## 兼容性

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.2+ |
| Fabric API | 26.1.2 对应版本（开发环境：0.149.0+26.1.2） |
| Java | 21+ |
| 环境 | 服务端 / 单人游戏 |

- 只统计**玩家破坏方块**；爆炸、活塞、自然消失等不计入。
- 会占用 `sidebar` 显示位。与其他会修改 sidebar 的计分板模组同时使用时，以最后加载/刷新者为准。
- 未测试 26.2 及更高版本；`fabric.mod.json` 已限制为 `~26.1.2`。

## 工作原理

1. 通过 Fabric API 的 `PlayerBlockBreakEvents.AFTER` 监听方块破坏。
2. 用 `Scoreboard#getOrCreatePlayerScore` 给 `minecount_total` 加 1。
3. 服务端启动时创建计分项并挂到 sidebar；之后每 20 tick 检查一次指向。
4. 数据完全存放在原版计分板中，没有额外的存档文件。

## 从源码构建

本项目**没有使用 Gradle/Loom**。26.1.2 的正式版 jar 直接包含 Mojang 官方类名（例如 `net/minecraft/server/level/ServerPlayer`），因此可以用 `javac` 直接编译。

### 前置条件

- JDK 21 或更高（脚本默认读取 `JAVA_HOME`，否则使用 `D:\JDK25`）
- 一个已安装 Fabric Loader 和 Fabric API 的 `26.1.2-Fabric 0.19.2` 版本目录
- Fabric API 的 bundle jar 放在该版本的 `mods` 文件夹里

### 构建

```powershell
.\build.ps1 -GameDir "E:\MCPCL\PCL 2.12.7.3\.minecraft\versions\26.1.2-Fabric 0.19.2" -JdkHome "D:\JDK25"
```

输出：`dist/mined-counter-1.1.0.jar`

脚本会从 Fabric API bundle 中解出嵌套模块，并自动在 `.minecraft\libraries` 里查找 `slf4j`、`guava` 等编译期依赖。

### 目录结构

```text
mined-counter/
├── src/main/java/minedcounter/
│   ├── MinedCounter.java     # 入口，检查 Fabric API 是否可用
│   └── CounterHook.java      # 事件监听、计分板创建、边栏维护
├── src/main/resources/
│   └── fabric.mod.json
├── build.ps1
├── dist/
│   └── mined-counter-1.1.0.jar
├── CHANGELOG.md
└── LICENSE
```

## 发布新版本

1. 修改 `src/main/resources/fabric.mod.json` 和 `build.ps1` 里的版本号。
2. 重新运行 `build.ps1`。
3. 提交并推送 `main`。
4. 打 tag 并推送：`git tag v1.1.1`、`git push origin v1.1.1`。
5. GitHub Actions 会自动创建 Release 并上传 `dist/mined-counter-1.1.1.jar`。

发布说明放在 `.github/release-notes/<tag>.md`，文件名必须和 tag 完全一致。

## 已知限制

- 边栏标题固定为“挖掘榜”（黄色 + 下划线），暂不可配置。
- 只统计总量，不按方块种类细分。
- 会强制占用 sidebar；管理员手动隐藏后，模组会在 1 秒内重新挂载。
- 没有配置文件、没有指令、没有权限系统。
- 仅面向 26.1.2，跨版本需要重新编译并调整 `fabric.mod.json`。

如果你遇到其他问题，欢迎开 Issue 或提交 PR。

## 同类项目

这个想法并不新，GitHub / Modrinth 上已有不少实现。列出它们，方便你按需选择：

| 项目 | 特点 | 26.1.2 |
| --- | --- | --- |
| [BlocksMined](https://modrinth.com/mod/blocksmined) | 挖掘 + 放置统计，可改分数颜色和显示位置；MIT；未开源 | ✅ |
| [Minecraft Digging Leaderboard](https://github.com/iL6hua/Minecraft-Digging-Leaderboard) | 中文挖掘榜，带配置文件、TPS 显示；AGPL-3.0 | ❌（到 1.21.11） |
| [Simple Statistic List](https://github.com/MShower/Simple-Statistic-List) | 挖掘/放置/击杀等多统计轮播；MIT | ❌（到 1.21.10） |
| [digging_counter](https://github.com/Missdrop/digging_counter) | 早期同类模组，基本停止维护 | ❌（1.21） |

**Mined Counter 的定位**：不追求功能最多，而是最小可用——一个计分项、零配置、约 150 行、MIT。如果你需要颜色/位置配置或更多统计维度，上面几个项目可能更合适。

## 参与贡献

- 发现 Bug 或有建议：开 [Issue](https://github.com/Galaxy-1118/mined-counter/issues)。
- 提交代码：Fork → 新建分支 → 修改 → 发 Pull Request。
- 这个项目的目标是保持小而清晰，新增功能前请先开 Issue 讨论。

## 许可证

[MIT](LICENSE) © 2026 Galaxy-1118

---

<details>
<summary><b>English</b></summary>

# Mined Counter

A lightweight **server-side Fabric mod** that counts how many blocks each player has mined and shows a live leaderboard on the scoreboard sidebar.

Target: **Minecraft 26.1.2** + **Fabric Loader 0.19.2**, Java 21+.

## Why

Vanilla scoreboard criteria track one block type at a time (`minecraft.mined:minecraft.stone`). There is no built-in "total blocks mined" criterion, so server owners usually end up with a datapack that sums dozens of objectives. This mod increments a single objective the moment a block is broken — no datapack, no commands, no config.

## Features

- Instant counting on block break (no polling loop).
- Creates `minecount_total` and attaches it to the sidebar automatically.
- Server-side only: vanilla clients see the sidebar without installing anything.
- Works in singleplayer when installed on the client.
- ~4 KB; checks the sidebar pointer once every 20 ticks, never iterates players.
- Fabric API is a soft dependency: without it the mod stays inactive instead of crashing the server.
- Cleans up the legacy `minecount_pool` objective from 1.0.0.
- MIT licensed, ~150 lines of Java.

## Install

1. Minecraft 26.1.2 + Fabric Loader 0.19.2+.
2. [Fabric API](https://modrinth.com/mod/fabric-api) for 26.1.2.
3. Put `mined-counter-1.1.0.jar` into `mods/`.
4. Restart the server.

## Usage

The sidebar titled “挖掘榜” appears automatically. The objective is `minecount_total` and persists with the world.

```mcfunction
/scoreboard objectives list
/scoreboard players reset <player> minecount_total
/scoreboard players reset * minecount_total
/scoreboard objectives setdisplay sidebar
```

Note: the mod re-attaches the sidebar every second, so hiding it manually does not stick.

## Compatibility

| Item | Version |
| --- | --- |
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.2+ |
| Fabric API | any 26.1.2 build (developed with 0.149.0+26.1.2) |
| Java | 21+ |
| Side | server / singleplayer |

Only player block breaks are counted. Explosions and pistons are ignored. The mod owns the `sidebar` slot and will conflict with other sidebar mods.

## Build

No Gradle/Loom: the 26.1.2 production jar ships with Mojang class names, so plain `javac` works.

```powershell
.\build.ps1 -GameDir "C:\...\versions\26.1.2-Fabric 0.19.2" -JdkHome "C:\...\jdk-21"
```

Output: `dist/mined-counter-1.1.0.jar`.

## Alternatives

- [BlocksMined](https://modrinth.com/mod/blocksmined) — mined/placed stats, color and display options, 26.1.2, closed source.
- [Minecraft Digging Leaderboard](https://github.com/iL6hua/Minecraft-Digging-Leaderboard) — config file, TPS display, up to 1.21.11.
- [Simple Statistic List](https://github.com/MShower/Simple-Statistic-List) — multiple rotating stats, up to 1.21.10.

Mined Counter intentionally stays minimal: one objective, zero config, MIT.

## License

[MIT](LICENSE) © 2026 Galaxy-1118

</details>
