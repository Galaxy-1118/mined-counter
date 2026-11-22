# Mined Counter / 挖掘榜

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62B47A?style=flat-square)](https://minecraft.net)
[![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.19.2%2B-1976D2?style=flat-square)](https://fabricmc.net)
[![Environment](https://img.shields.io/badge/Environment-Server-6E6E6E?style=flat-square)](#安装)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

一个轻量的 **Fabric 服务端模组**：统计每位玩家挖掘方块的总次数，并在计分板上实时显示“挖掘榜”。管理员可以用游戏内指令修改标题样式、切换显示位置、开关统计与显示，也可以从原版统计重建榜单。

目标版本：**Minecraft 26.1.2** + **Fabric Loader 0.19.2**，Java 21+。

> 当前开发版本 **1.2.0**。1.1.0 的下载与历史仍在 [Releases](https://github.com/Galaxy-1118/mined-counter/releases)。

---

## 为什么做这个模组

原版计分板只能按**方块种类**统计，例如：

```mcfunction
scoreboard objectives add stone_mined minecraft.mined:minecraft.stone
```

想统计“所有方块一共挖了多少”，原版没有现成准则。常见做法是为每种方块建一个计分项再用函数求和，或者用数据包定时轮询——配置麻烦，种类多了还会明显增加开销。

这个模组换了个做法：监听方块破坏事件，**在方块被挖掉的瞬间直接给玩家 +1**。只有一个计分项，不需要数据包；标题、位置、开关和初始化都可以用 `/minedcount` 管理。

## 功能

- **即时统计**：方块破坏后立刻 +1，不是每分钟轮询。
- **零配置可用**：放进 `mods` 重启服务器，默认自动出现“挖掘榜”。
- **标题可定制**：管理员可用指令修改文字、颜色、粗体、下划线、斜体、删除线等。
- **位置可切换**：`sidebar`（右侧）、`list`（Tab 列表）、`below_name`（玩家名字下方）。
- **统计开关**：`/minedcount enable|disable` 控制是否继续计数。
- **显示开关**：`/minedcount show|hide` 只控制计分板显示，计数继续。
- **简单配置文件**：标题、是否允许占用 sidebar、位置、开关状态都会持久化。
- **从原版统计初始化**：重置榜单并从玩家统计中汇总各类方块挖掘数；默认包含离线玩家。
- **纯服务端**：专用服务器只需装在服务端；原版客户端不用装也能看到计分板。
- **轻量**：约 20 KB；每 20 tick 只做一次计分板检查，不遍历玩家。
- **软依赖 Fabric API**：Fabric API 缺失时自动停用并打印警告，不会导致服务器启动失败。
- **MIT 开源**：约 700 行 Java，方便阅读和二次修改。

## 安装

1. 安装 **Minecraft 26.1.2** 和 **Fabric Loader 0.19.2+**（[Fabric 安装器](https://fabricmc.net/use/)）。
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api)（选 `26.1.2` 版本）。
3. 把 `mined-counter-1.2.0.jar` 放进服务器的 `mods` 文件夹。
4. 重启服务器。

目录结构示例：

```text
server/
├── config/
│   └── mined_counter.json      # 首次启动自动生成
└── mods/
    ├── fabric-api-0.152.1+26.1.2.jar
    └── mined-counter-1.2.0.jar
```

启动成功后，日志里会出现：

```text
[mined_counter] active: objective=minecount_total, title='&e&n挖掘榜', position=sidebar, enabled=true, visible=true, sidebar=true
```

### 从 1.1.0 升级

直接用 1.2.0 的 jar 替换旧 jar 即可。计分项 `minecount_total` 和已有分数会保留；首次启动会生成 `config/mined_counter.json`，默认标题是 `&e&n挖掘榜`。如果你之前用指令改过计分板显示名，升级后会被配置文件中的标题覆盖，改回即可。

## 指令

所有 `/minedcount` 指令都需要 **权限等级 2**（游戏内 OP / 管理员）。单人游戏需要开启作弊。

| 指令 | 作用 |
| --- | --- |
| `/minedcount enable` | 恢复统计（计数继续，显示按 `visible` 状态恢复） |
| `/minedcount disable` | 停止统计并隐藏计分板；已有分数保留 |
| `/minedcount show` | 显示计分板；统计继续进行 |
| `/minedcount hide` | 隐藏计分板；统计继续进行 |
| `/minedcount title <标题>` | 修改标题文字与样式，例如 `&6&l&n挖掘榜` |
| `/minedcount position <位置>` | 位置：`sidebar`、`list`、`below_name` |
| `/minedcount init` | 重置榜单，并从在线 + 离线玩家统计重建 |
| `/minedcount init online` | 只从在线玩家统计重建（不扫描离线文件，适合大服） |
| `/minedcount reload` | 重新读取 `config/mined_counter.json` |
| `/minedcount status` | 查看当前统计/显示/位置/标题状态 |

### 标题格式

标题使用经典的 `&` 格式代码，也接受 `§`：

| 代码 | 效果 |
| --- | --- |
| `&0` ~ `&9`、`&a` ~ `&f` | 颜色 |
| `&l` | 粗体 |
| `&n` | 下划线 |
| `&o` | 斜体 |
| `&m` | 删除线 |
| `&k` | 混淆文字 |
| `&r` | 重置颜色与格式 |
| `&&` | 输出一个普通 `&` |

示例：

```mcfunction
/minedcount title &6&l&n挖掘榜
/minedcount title &c&l红色&r&n下划线
```

标题最长 256 个字符，不能只包含格式代码。

### 显示位置

```mcfunction
/minedcount position sidebar     # 右侧边栏
/minedcount position list        # Tab 列表
/minedcount position below_name  # 玩家名字下方
```

## 配置文件

路径：**`config/mined_counter.json`**（服务端根目录；单人游戏在 `.minecraft/config/`）。首次启动自动生成：

```json
{
  "title": "&e&n挖掘榜",
  "sidebar": true,
  "position": "sidebar",
  "enabled": true,
  "visible": true
}
```

| 字段 | 说明 |
| --- | --- |
| `title` | 标题原文，支持 `&` 格式代码 |
| `sidebar` | 是否允许模组占用 sidebar。设为 `false` 时，`sidebar` 位置不会被挂载；`list` / `below_name` 不受影响 |
| `position` | `sidebar`、`list` 或 `below_name` |
| `enabled` | 是否继续统计方块 |
| `visible` | 是否显示计分板 |

`/minedcount enable/disable/show/hide/title/position` 会同步写回配置文件。手动编辑文件后执行 `/minedcount reload` 生效。

> `sidebar=false` 时，`/minedcount show` 和 `/minedcount position sidebar` 会拒绝执行并提示原因，避免配置与指令互相打架。

## 初始化计分板

`/minedcount init` 会：

1. 删除并重建 `minecount_total`（等于重置所有分数）；
2. 对**在线玩家**读取实时的原版统计；
3. 默认还会扫描 `world/players/stats/*.json`，读取**离线玩家**的统计；
4. 按 `usercache.json` 把 UUID 映射回玩家名；
5. 汇总每种方块的 `minecraft.mined` 数量，写入榜单。

输出示例：

```text
已重置并初始化挖掘榜：在线玩家 1 人，离线玩家 12 人，写入 13 条分数，合计 45678 次挖掘。
```

注意事项：

- **只统计玩家破坏方块**；爆炸、活塞、自然消失不计入。
- 默认会读取所有离线统计文件，可能产生一次性的磁盘 I/O 和短暂卡顿；大服建议在低峰期执行，或使用 `/minedcount init online`。
- 离线玩家必须能在 `usercache.json` 里找到名字，否则会被跳过。
- 该指令不会自动运行，只在你手动执行时发生。

## 兼容性

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.2+ |
| Fabric API | 26.1.2 对应版本（开发环境：0.149.0+26.1.2） |
| Java | 21+ |
| 环境 | 服务端 / 单人游戏 |

- 会占用配置的显示位。与其他会修改同一显示位的计分板模组同时使用时，以最后加载/刷新者为准。
- 未测试 26.2 及更高版本；`fabric.mod.json` 已限制为 `~26.1.2`。
- 计分项 ID 固定为 `minecount_total`，不随标题变化。

## 工作原理

1. 通过 Fabric API 的 `PlayerBlockBreakEvents.AFTER` 监听方块破坏；统计开启时直接给 `minecount_total` +1。
2. 服务端启动时创建计分项，读取配置并挂到目标显示位；之后每 20 tick 检查一次显示位，不遍历玩家。
3. `/minedcount` 指令修改配置并立即调用同一套应用逻辑。
4. `/minedcount init` 重建计分项，从在线统计和 `world/players/stats` 汇总数据。
5. 数据完全存放在原版计分板中，没有额外的存档文件；配置文件只保存显示设置。

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

输出：`dist/mined-counter-1.2.0.jar`

脚本会从 Fabric API bundle 中解出嵌套模块，并自动在 `.minecraft\libraries` 里查找 `slf4j`、`guava`、`brigadier`、`jspecify` 等编译期依赖。

### 目录结构

```text
mined-counter/
├── src/main/java/minedcounter/
│   ├── MinedCounter.java       # 入口，检查 Fabric API 是否可用
│   ├── CounterHook.java        # 事件、计分板、配置应用、统计导入
│   ├── CounterConfig.java      # 配置文件读写与校验
│   ├── MinedCountCommand.java  # /minedcount 指令树
│   └── TitleParser.java        # & 格式代码解析
├── src/main/resources/
│   └── fabric.mod.json
├── build.ps1
├── dist/
│   ├── mined-counter-1.1.0.jar
│   └── mined-counter-1.2.0.jar
├── CHANGELOG.md
└── LICENSE
```

## 发布新版本

1. 修改 `src/main/resources/fabric.mod.json` 和 `build.ps1` 里的版本号。
2. 重新运行 `build.ps1`。
3. 在 `.github/release-notes/` 新建与 tag 同名的说明文件。
4. 提交并推送 `main`。
5. 打 tag 并推送：`git tag v1.2.0`、`git push origin v1.2.0`。
6. GitHub Actions 会自动创建 Release，并只上传与 tag 匹配的 `dist/mined-counter-<version>.jar`。

## 已知限制

- 只统计总量，不按方块种类细分。
- 会强制占用所选显示位；与其他同位置计分板模组冲突。
- 标题只支持 `&` 格式代码，不支持 JSON 文本组件、点击事件或悬浮事件。
- 指令权限固定为等级 2，没有更细的权限节点。
- `init` 读取离线统计需要 `usercache.json` 中存在对应玩家名。
- 仅面向 26.1.2，跨版本需要重新编译并调整 `fabric.mod.json`。

## 关于假人与大小写（不是模组 bug）

有服务器反馈：真实玩家 `Galaxy_1118` 和假人 `galaxy_1118` 同时出现在榜单，但执行 `/scoreboard players reset galaxy_1118 minecount_total` 时，被重置的是真实玩家 `Galaxy_1118`。

这是原版计分板与指令解析的行为，不是本模组造成的：

- Minecraft 的计分板内部用 `Map<String, PlayerScores>` 按 **`getScoreboardName()` 原样** 存储，**大小写敏感**，所以两个名字会显示成两条。
- `/scoreboard players ...` 的目标参数在解析名字时会调用 `PlayerList.getPlayerByName()`，而它使用 **`equalsIgnoreCase`**；真实玩家在线时，`galaxy_1118` 会先匹配到 `Galaxy_1118`。
- 本模组只在 `PlayerBlockBreakEvents.AFTER` 里给 `ServerPlayer` 加分，不会创建假人条目；假人条目来自假人模组或手动指令。

可行的规避方式：

- 给假人起一个不会与真实 ID 仅大小写不同的名字，例如 `bot_galaxy`、`galaxy_bot`。
- 如果假人模组支持，用实体选择器而不是纯名字来重置。
- 需要精确重置大小写不同的条目时，只能直接改计分板数据或使用能按原始名字匹配的工具。

所以这一项**没有修改模组代码**。

## 同类项目

这个想法并不新，GitHub / Modrinth 上已有不少实现。列出它们，方便你按需选择：

| 项目 | 特点 | 26.1.2 |
| --- | --- | --- |
| [BlocksMined](https://modrinth.com/mod/blocksmined) | 挖掘 + 放置统计，可改分数颜色和显示位置；MIT；未开源 | ✅ |
| [Minecraft Digging Leaderboard](https://github.com/iL6hua/Minecraft-Digging-Leaderboard) | 中文挖掘榜，带配置文件、TPS 显示；AGPL-3.0 | ❌（到 1.21.11） |
| [Simple Statistic List](https://github.com/MShower/Simple-Statistic-List) | 挖掘/放置/击杀等多统计轮播；MIT | ❌（到 1.21.10） |
| [digging_counter](https://github.com/Missdrop/digging_counter) | 早期同类模组，基本停止维护 | ❌（1.21） |

**Mined Counter 的定位**：不追求功能最多，而是保持小而清晰——一个计分项、零配置、可指令调整、MIT。如果你需要颜色/位置配置或更多统计维度，上面几个项目可能更合适。

## 参与贡献

- 发现 Bug 或有建议：开 [Issue](https://github.com/Galaxy-1118/mined-counter/issues)。
- 提交代码：Fork → 新建分支 → 修改 → 发 Pull Request。
- 新增功能前请先开 Issue 讨论，避免和“保持小而清晰”的目标冲突。

## 许可证

[MIT](LICENSE) © 2026 Galaxy-1118

---

<details>
<summary><b>English</b></summary>

# Mined Counter

A lightweight **server-side Fabric mod** that counts how many blocks each player has mined and shows a live leaderboard on the scoreboard. Admins can restyle the title, move the display, toggle counting/visibility, and rebuild the board from vanilla statistics with `/minedcount`.

Target: **Minecraft 26.1.2** + **Fabric Loader 0.19.2**, Java 21+.

## Features

- Instant counting on block break (no polling loop).
- Creates `minecount_total` and attaches it to the configured display slot automatically.
- Title supports classic `&` formatting: colours, bold, underline, italic, strikethrough, obfuscated, reset.
- Display positions: `sidebar`, `list`, `below_name`.
- `/minedcount enable|disable` controls counting; `/minedcount show|hide` controls only visibility.
- Simple `config/mined_counter.json` persisted settings.
- `/minedcount init` rebuilds the board from vanilla stats, including offline players by default.
- Server-side only: vanilla clients see the scoreboard without installing anything.
- ~20 KB; checks the display slot once every 20 ticks, never iterates players.
- Fabric API is a soft dependency; without it the mod stays inactive instead of crashing the server.
- MIT licensed.

## Install

1. Minecraft 26.1.2 + Fabric Loader 0.19.2+.
2. [Fabric API](https://modrinth.com/mod/fabric-api) for 26.1.2.
3. Put `mined-counter-1.2.0.jar` into `mods/`.
4. Restart the server.

Upgrading from 1.1.0: replace the jar. The objective and scores are kept; a config file is generated on first start.

## Commands

Requires permission level 2 (OP / gamemaster). In singleplayer, cheats must be enabled.

| Command | Effect |
| --- | --- |
| `/minedcount enable` | Resume counting |
| `/minedcount disable` | Stop counting and hide the board; scores are kept |
| `/minedcount show` | Show the board; counting continues |
| `/minedcount hide` | Hide the board; counting continues |
| `/minedcount title <title>` | Set title text/styles, e.g. `&6&l&nMined` |
| `/minedcount position <sidebar\|list\|below_name>` | Move the display |
| `/minedcount init` | Reset and rebuild from online + offline stats |
| `/minedcount init online` | Rebuild from online players only |
| `/minedcount reload` | Reload `config/mined_counter.json` |
| `/minedcount status` | Show current state |

## Config

`config/mined_counter.json`:

```json
{
  "title": "&e&n挖掘榜",
  "sidebar": true,
  "position": "sidebar",
  "enabled": true,
  "visible": true
}
```

- `sidebar`: allow the mod to use the sidebar slot. When false, `sidebar` is blocked; `list` and `below_name` still work.
- `position`: `sidebar`, `list`, or `below_name`.
- `enabled`: counting on/off.
- `visible`: display on/off.

Commands write these values back. Manual edits take effect after `/minedcount reload`.

## Init semantics

`/minedcount init` removes and recreates `minecount_total`, reads live online stats, then scans `world/players/stats/*.json` for offline players and maps UUIDs through `usercache.json`. Block mining stats are summed per player.

- Only player block breaks are counted.
- Reading all offline stats is a one-time I/O cost; on very large servers use `/minedcount init online` and run at low traffic.
- Offline players missing from `usercache.json` are skipped.

## Compatibility

| Item | Version |
| --- | --- |
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.2+ |
| Fabric API | any 26.1.2 build (developed with 0.149.0+26.1.2) |
| Java | 21+ |
| Side | server / singleplayer |

The mod owns the configured display slot and will conflict with other scoreboard mods using the same slot. Only block breaks are counted; explosions and pistons are ignored.

## Fake players and case sensitivity

This is vanilla behaviour, not a mod bug:

- Scoreboard storage is keyed by the exact `getScoreboardName()` string and is **case-sensitive**, so `Galaxy_1118` and `galaxy_1118` are two entries.
- `/scoreboard players ...` resolves names through `PlayerList.getPlayerByName()`, which uses `equalsIgnoreCase`; while the real player is online, `galaxy_1118` matches `Galaxy_1118`.
- The mod only increments the `ServerPlayer` that broke the block; fake-player entries come from the fake-player mod or manual commands.

Use a fake-player name that differs by more than case (for example `bot_galaxy`).

## Build

No Gradle/Loom: the 26.1.2 production jar ships with Mojang class names, so plain `javac` works.

```powershell
.\build.ps1 -GameDir "C:\...\versions\26.1.2-Fabric 0.19.2" -JdkHome "C:\...\jdk-21"
```

Output: `dist/mined-counter-1.2.0.jar`.

## Alternatives

- [BlocksMined](https://modrinth.com/mod/blocksmined) — mined/placed stats, colour and display options, 26.1.2, closed source.
- [Minecraft Digging Leaderboard](https://github.com/iL6hua/Minecraft-Digging-Leaderboard) — config file, TPS display, up to 1.21.11.
- [Simple Statistic List](https://github.com/MShower/Simple-Statistic-List) — multiple rotating stats, up to 1.21.10.

Mined Counter intentionally stays small: one objective, zero-config defaults, admin commands, MIT.

## License

[MIT](LICENSE) © 2026 Galaxy-1118

</details>
