# Mined Counter / 挖掘榜

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1--26.3-62B47A?style=flat-square)](https://minecraft.net)
[![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.19.2%2B-1976D2?style=flat-square)](https://fabricmc.net)
[![Java](https://img.shields.io/badge/Java-25%2B-orange?style=flat-square)](https://adoptium.net)
[![Environment](https://img.shields.io/badge/Environment-Server-6E6E6E?style=flat-square)](#安装)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

一个轻量的 **Fabric 服务端模组**：统计每位玩家挖掘方块的总次数，并在计分板上实时显示“挖掘榜”。管理员可以用游戏内指令修改标题样式、切换显示位置、开关统计与显示，也可以从原版统计重建榜单。

目标版本：**Minecraft 26.1 – 26.3**，Fabric Loader **0.19.2+ / 0.20.1+**，Java **25+**。每个 Minecraft 版本有独立 jar，详见下方版本矩阵。

> 当前开发版本 **1.2.0**。1.1.0 的下载与历史仍在 [Releases](https://github.com/Galaxy-1118/mined-counter/releases)。

---

## 为什么做这个模组

原版计分板只能按**方块种类**统计，例如：

```mcfunction
scoreboard objectives add stone_mined minecraft.mined:minecraft.stone
```

想统计“所有方块一共挖了多少”，原版没有现成准则。常见做法是为每种方块建一个计分项再用函数求和，或者用数据包定时轮询——配置麻烦，种类多了还会明显增加开销。

这个模组换了个做法：监听方块破坏事件，**在方块被挖掉的瞬间直接给玩家 +1**。只有一个计分项 `minedcounter_total`，不需要数据包；标题、位置、开关和初始化都可以用 `/minedcounter` 管理。

## 功能

- **即时统计**：有效采集后立刻 +1，不是每分钟轮询。
- **与原版统计对齐**：生存模式只统计使用正确工具、能正常掉落的方块，和 `minecraft.mined` 的判定一致。
- **零配置可用**：放进 `mods` 重启服务器，默认自动出现“挖掘榜”。
- **标题可定制**：支持经典 `&` 格式代码，也支持 JSON 文本组件；颜色、粗体、下划线、斜体、删除线等都可以设置。
- **位置可切换**：`sidebar`（右侧）、`list`（Tab 列表）、`below_name`（玩家名字下方）。
- **统计开关**：`/minedcounter enable|disable` 控制是否继续计数。
- **显示开关**：`/minedcounter show|hide` 只控制计分板显示，计数继续。
- **创造模式开关**：默认忽略创造模式破坏方块，使实时计数与原版统计、`/minedcounter init` 保持一致；可在配置或指令中打开。
- **简单配置文件**：标题、sidebar 开关、位置、统计/显示/创造模式状态都会持久化。
- **从原版统计初始化**：重置榜单并从玩家统计中汇总各类方块挖掘数；默认包含离线玩家。
- **中英双语回显**：玩家收到的指令反馈跟随客户端语言（`zh_*` / 其他），控制台跟随服务器系统语言。
- **旧数据迁移**：首次启动自动把旧版的 `minecount_total` 分数迁移到 `minedcounter_total`，不会清空榜单。
- **纯服务端**：专用服务器只需装在服务端；原版客户端不用装也能看到计分板。
- **轻量**：约 26 KB；每 20 tick 只做一次计分板检查，不遍历玩家。
- **软依赖 Fabric API**：Fabric API 缺失时自动停用并打印警告，不会导致服务器启动失败。
- **MIT 开源**：约 1000 行 Java，方便阅读和二次修改。

## 安装

1. 确认你的 Minecraft 是 **26.1 – 26.3** 之间的正式版，并安装对应的 Fabric Loader。
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api)（版本见下表）。
3. 把**对应 Minecraft 版本**的 jar 放进服务器的 `mods` 文件夹。
4. 重启服务器，并确认 Java 是 25 或更高。

### 26.x 版本矩阵

| Minecraft | Fabric Loader | Fabric API | jar |
| --- | --- | --- | --- |
| 26.1 | 0.19.2+ | 0.155.3+26.1.2 | `mined-counter-1.2.0+26.1.jar` |
| 26.1.1 | 0.19.2+ | 0.155.3+26.1.2 | `mined-counter-1.2.0+26.1.1.jar` |
| 26.1.2 | 0.19.2+ | 0.155.3+26.1.2 | `mined-counter-1.2.0+26.1.2.jar` |
| 26.2 | 0.20.1+ | 0.161.0+26.2 | `mined-counter-1.2.0+26.2.jar` |
| 26.3 | 0.20.1+ | 0.167.0+26.3 | `mined-counter-1.2.0+26.3.jar` |

26.1 / 26.1.1 / 26.1.2 使用同一份代码和 Fabric API 构建；26.1 的 jar 覆盖整个 26.1.x，26.1.1 / 26.1.2 的 jar 从对应补丁版本起兼容。建议按你的精确版本选择对应文件。完整文件清单和 SHA-256 见 [docs/versions.md](docs/versions.md)。

### 文件命名

文件名采用主流 Fabric 模组的约定：

```text
mined-counter-<模组版本>+<Minecraft版本>.jar
mined-counter-1.2.0+26.1.2.jar
```

- `mined-counter`：模组名，使用小写和连字符；
- `1.2.0`：模组版本；
- `+26.1.2`：SemVer build metadata，表示目标 Minecraft 版本；
- 本项目只有 Fabric 一个加载器，所以文件名不额外加 `fabric`。如果以后同时支持 NeoForge，会改为 `mined-counter-fabric-...` / `mined-counter-neoforge-...`。

Fabric API、Sodium、Lithium 等主流模组也使用类似格式，例如 `fabric-api-0.167.0+26.3.jar`、`lithium-fabric-0.26.4+mc26.3.jar`。不建议使用 `mined_counter-fabric-'26.1.2'-v-1.2.0.jar` 这类带引号、下划线和 `v-` 前缀的写法。

目录结构示例：

```text
server/
├── config/
│   └── mined_counter.json      # 首次启动自动生成
└── mods/
    ├── fabric-api-0.155.3+26.1.2.jar
    └── mined-counter-1.2.0+26.1.2.jar
```

启动成功后，日志里会出现：

```text
[mined_counter] active: objective=minedcounter_total, title='&e&n挖掘榜', position=sidebar, enabled=true, visible=true, sidebar=true, countCreative=false
```

### 从 1.1.0 或早期 1.2.0 升级

直接用新 jar 替换旧 jar 即可：

- 旧计分项 `minecount_total` 会在启动时自动复制到 `minedcounter_total`，然后删除旧计分项；
- 旧指令 `/minedcount` 已改名为 `/minedcounter`，旧的命令名不再注册；
- 计分项和已有分数保留；首次启动会生成 `config/mined_counter.json`，默认标题是 `&e&n挖掘榜`；
- 如果你之前用指令改过计分板显示名，升级后会被配置文件中的标题覆盖，改回即可。
- 旧版本可能已经记录了徒手挖石头、错误工具挖矿或创造模式破坏的分数；升级后建议执行一次 `/minedcounter init`，按原版统计重建并清理这些不符合新规则的分数。

## 指令

所有 `/minedcounter` 指令都需要 **权限等级 2**（游戏内 OP / 管理员）。单人游戏需要开启作弊。

| 指令 | 作用 |
| --- | --- |
| `/minedcounter enable` | 恢复统计（计数继续，显示按 `visible` 状态恢复） |
| `/minedcounter disable` | 停止统计并隐藏计分板；已有分数保留 |
| `/minedcounter show` | 显示计分板；统计继续进行 |
| `/minedcounter hide` | 隐藏计分板；统计继续进行 |
| `/minedcounter title <标题>` | 修改标题文字与样式，支持 `&` 代码或 JSON |
| `/minedcounter position <位置>` | 位置：`sidebar`、`list`、`below_name` |
| `/minedcounter creative on\|off` | 是否统计创造模式破坏方块（默认 `off`） |
| `/minedcounter init` | 重置榜单，并从在线 + 离线玩家统计重建 |
| `/minedcounter init online` | 只从在线玩家统计重建（不扫描离线文件，适合大服） |
| `/minedcounter reload` | 重新读取 `config/mined_counter.json` |
| `/minedcounter status` | 查看当前统计/显示/位置/创造模式/标题状态 |

### 标题格式：`&` 代码

标题使用经典 `&` 格式代码，也接受 `§`：

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
/minedcounter title &6&l&n挖掘榜
/minedcounter title &c&l红色&r&n下划线
```

### 标题格式：JSON

从 1.2.0 起，`title` 也接受标准 Minecraft JSON 文本组件：

```mcfunction
/minedcounter title {"text":"挖掘榜","color":"yellow","underlined":true,"bold":true}
```

也支持**只写样式**的简写，样式会应用到当前标题文字上：

```mcfunction
/minedcounter title {color:"yellow";underlined:true}
```

- JSON 解析是宽松的：键名可以不加引号，分隔符可以用 `;`；最终会保存为规范 JSON。
- 只写样式时，当前标题的纯文本会作为 `text` 写入配置，所以重启后仍然有效。
- 标题 JSON 最长 1024 个字符；`&` 代码标题最长 256 个字符。
- 标题不能为空，也不能只有样式而没有可见文字。

## 配置文件

路径：**`config/mined_counter.json`**（服务端根目录；单人游戏在 `.minecraft/config/`）。首次启动自动生成：

```json
{
  "title": "&e&n挖掘榜",
  "sidebar": true,
  "position": "sidebar",
  "enabled": true,
  "visible": true,
  "countCreative": false
}
```

| 字段 | 说明 |
| --- | --- |
| `title` | 标题原文，支持 `&` 格式代码或 JSON 文本组件 |
| `sidebar` | 是否允许模组占用 sidebar。设为 `false` 时，`sidebar` 位置不会被挂载；`list` / `below_name` 不受影响 |
| `position` | `sidebar`、`list` 或 `below_name` |
| `enabled` | 是否继续统计方块 |
| `visible` | 是否显示计分板 |
| `countCreative` | 是否统计创造模式破坏方块；默认 `false` |

`/minedcounter enable/disable/show/hide/title/position/creative` 会同步写回配置文件。手动编辑文件后执行 `/minedcounter reload` 生效。

> `sidebar=false` 时，`/minedcounter show` 和 `/minedcounter position sidebar` 会拒绝执行并提示原因，避免配置与指令互相打架。

## 创造模式计数

默认 `countCreative=false`，**创造模式破坏方块不会计入**。这是推荐设置，因为：

- 原版统计 `Stats.BLOCK_MINED` 不记录创造模式破坏的方块；
- 如果实时计数包含创造模式，`/minedcounter init` 从原版统计重建时就会“丢失”这部分，导致实时分数和重建结果不一致；
- 对生存服/生电服来说，挖掘榜通常只应该反映生存获取。

如果你确实希望创造模式也计数，可以执行：

```mcfunction
/minedcounter creative on
```

或在配置里设置 `"countCreative": true` 后 `/minedcounter reload`。

**注意**：打开后，`/minedcounter init` 无法从原版统计恢复创造模式产生的分数，重建时这部分会被清掉。这是原版统计不记录创造模式导致的，不是模组可以无损补回的；如果以后需要“创造/生存分开统计”，需要额外维护第二个计分项或数据文件，会明显增加复杂度，本版本没有这样做。

## 有效采集条件（为什么徒手挖石头不计分）

为了和原版 `minecraft.mined` 统计、`/minedcounter init` 对齐，生存模式只有在类似以下条件成立时才 +1：

```text
ServerPlayer.hasCorrectToolForDrops(state) == true
```

原版 `ServerPlayerGameMode.destroyBlock` 也只在同一条件成立时才调用 `Block.playerDestroy`，而 `Stats.BLOCK_MINED` 正是在 `playerDestroy` 里累加的。因此：

- 徒手挖石头、用木镐挖钻石矿等**不会**计分，也不会进入原版统计；
- 徒手挖泥土、沙子等不需要工具的方块**会**计分（原版也会记录）；
- 爆炸、活塞、自然消失不计入。

创造模式默认不计数；`/minedcounter creative on` 后，创造模式的所有破坏都会计数（不再检查工具），但这部分无法被 `init` 从原版统计恢复，原因见上一节。

这样修改后，生存模式的实时榜单和 `/minedcounter init` 重建结果来自同一套判定，不会再出现“实时有分、init 后消失”的情况。

## 为什么不撤销 init

修复后，`init` 和实时计数都只认“原版统计会记录的有效采集”，所以 `init` 可以安全保留：

- 新服/新世界：不执行 `init`，模组就从部署时开始记录；
- 旧版本升级：执行一次 `init`，可以按原版统计清理旧版本误计的无效采集/创造模式分数；
- 需要离线玩家数据：`init` 是唯一能从 `world/players/stats` 重建历史数据的入口。

如果你希望“只从部署模组时开始记录”，不执行 `init` 即可；模组不会自动运行它。

## 初始化计分板

`/minedcounter init` 会：

1. 删除并重建 `minedcounter_total`（等于重置所有分数）；
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
- 生存模式只统计使用正确工具、能正常掉落的采集；徒手挖石头等无效采集不会计分，详见“有效采集条件”。
- 默认会读取所有离线统计文件，可能产生一次性的磁盘 I/O 和短暂卡顿；大服建议在低峰期执行，或使用 `/minedcounter init online`。
- 离线玩家必须能在 `usercache.json` 里找到名字，否则会被跳过。
- 该指令不会自动运行，只在你手动执行时发生。
- 如果 `countCreative=true`，创造模式产生的实时分数不会出现在重建结果中，原因见上一节。

## 本地化 / Localisation

指令回显支持中文和英文，不需要安装资源包：

- 玩家执行指令时，根据玩家的客户端语言决定：`zh_*` 使用中文，其他语言使用英文；
- 控制台、命令方块、RCON 没有客户端语言，使用服务器操作系统的默认语言；
- 指令名、子指令名和错误信息中的参数值保持英文（例如 `sidebar`、`list`），便于复制和排错。

如果你希望增加更多语言，可以在 `Messages.java` 中增加语言分支；当前只内置中文和英文，避免引入客户端资源包依赖。

## 兼容性

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.1 / 26.1.1 / 26.1.2 / 26.2 / 26.3，每个版本使用对应 jar |
| Fabric Loader | 26.1.x：0.19.2+；26.2 / 26.3：0.20.1+ |
| Fabric API | 26.1.x：0.155.3+26.1.2；26.2：0.161.0+26.2；26.3：0.167.0+26.3 |
| Java | 25+ |
| 环境 | 服务端 / 单人游戏 |

- 会占用配置的显示位。与其他会修改同一显示位的计分板模组同时使用时，以最后加载/刷新者为准。
- 26.1–26.3 源码均已编译验证；发布前建议在对应版本服务端各做一次冒烟测试，尤其是 26.2/26.3。
- 计分项 ID 固定为 `minedcounter_total`，不随标题变化。

## 工作原理

1. 通过 Fabric API 的 `PlayerBlockBreakEvents.AFTER` 监听方块破坏；统计开启且（默认）非创造模式时，直接给 `minedcounter_total` +1。
2. 服务端启动时创建计分项，读取配置并挂到目标显示位；之后每 20 tick 检查一次显示位，不遍历玩家。
3. 启动时把旧 `minecount_total` 的分数复制到新计分项，并清理 1.0.0 的 `minecount_pool`。
4. `/minedcounter` 指令修改配置并立即调用同一套应用逻辑；标题支持 `&` 代码或 JSON 组件。
5. `/minedcounter init` 重建计分项，从在线统计和 `world/players/stats` 汇总数据。
6. 数据完全存放在原版计分板中，没有额外的存档文件；配置文件只保存显示设置。

## 从源码构建

本项目**没有使用 Gradle/Loom**。26.x 的正式版 jar 直接包含 Mojang 官方类名（例如 `net/minecraft/server/level/ServerPlayer`），因此可以用 `javac` 直接编译。

### 前置条件

- JDK 25（脚本默认读取 `JAVA_HOME`，否则使用 `D:\JDK25`）
- Java 25 运行时（Minecraft 26.x / Fabric API 要求）
- 每个目标版本的游戏 jar、Fabric Loader、Fabric API（见下方目录布局）

### 多版本构建

```powershell
.\build-versions.ps1
```

脚本读取 `versions.json`，默认从 `D:\Deepseek Harness\mc-versions` 取各版本依赖，输出：

```text
dist/mined-counter-1.2.0+26.1.jar
dist/mined-counter-1.2.0+26.1.1.jar
dist/mined-counter-1.2.0+26.1.2.jar
dist/mined-counter-1.2.0+26.2.jar
dist/mined-counter-1.2.0+26.3.jar
```

每个版本目录需要：

```text
mc-versions/<minecraft>/
├── <minecraft>.jar        # Mojang 映射的客户端 jar
├── fabric-loader.jar
└── fabric-api.jar         # Fabric API bundle
```

`build.ps1` 仍然保留，用于只构建本地 26.1.2 单版本开发包：

```powershell
.\build.ps1 -GameDir "E:\MCPCL\PCL 2.12.7.3\.minecraft\versions\26.1.2-Fabric 0.19.2" -JdkHome "D:\JDK25"
```

输出：`dist/mined-counter-1.2.0.jar`

### 目录结构

```text
mined-counter/
├── src/main/java/minedcounter/
│   ├── MinedCounter.java        # 入口，检查 Fabric API 是否可用
│   ├── CounterHook.java         # 事件、计分板、配置应用、统计导入、旧数据迁移
│   ├── CounterConfig.java       # 配置文件读写与校验
│   ├── MinedCounterCommand.java # /minedcounter 指令树
│   ├── TitleParser.java         # & 代码与 JSON 标题解析
│   └── Messages.java            # 中英双语指令回显
├── src/main/resources/
│   └── fabric.mod.json
├── examples/
│   └── mined_counter.json       # 配置示例
├── build.ps1                    # 单版本 26.1.2 开发构建
├── build-versions.ps1           # 26.x 多版本构建
├── versions.json                # 版本 / Loader / API 矩阵
├── docs/
│   └── versions.md              # 26.x 文件清单与 SHA-256
├── dist/
│   ├── mined-counter-1.1.0.jar
│   ├── mined-counter-1.2.0.jar          # 旧版单版本包
│   └── mined-counter-1.2.0+26.*.jar     # 多版本包
├── CHANGELOG.md
└── LICENSE
```

## 发布新版本

1. 修改 `src/main/resources/fabric.mod.json`、`build.ps1` 和 `versions.json` 里的版本/矩阵信息。
2. 运行 `.\build-versions.ps1` 生成全部 26.x jar。
3. 在 `.github/release-notes/` 新建与 tag 同名的说明文件。
4. 提交并推送 `main`。
5. 打 tag 并推送：`git tag v1.2.0`、`git push origin v1.2.0`。
6. GitHub Actions 会自动创建 Release，并上传与 tag 匹配的 `dist/mined-counter-<version>+*.jar`。

## 已知限制

- 只统计总量，不按方块种类细分。
- 生存模式不统计徒手挖石头、错误工具挖矿等无效采集，以保持与原版统计、`init` 一致。
- 会强制占用所选显示位；与其他同位置计分板模组冲突。
- 标题 JSON 使用标准文本组件解析，但指令回显只有中文/英文两种语言。
- 指令权限固定为等级 2，没有更细的权限节点。
- `init` 读取离线统计需要 `usercache.json` 中存在对应玩家名。
- 面向 Minecraft 26.1–26.3；每个版本使用对应 jar，跨大版本需要重新编译。

## 关于假人与大小写（不是模组 bug）

有服务器反馈：真实玩家 `Galaxy_1118` 和假人 `galaxy_1118` 同时出现在榜单，但执行 `/scoreboard players reset galaxy_1118 minedcounter_total` 时，被重置的是真实玩家 `Galaxy_1118`。

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

A lightweight **server-side Fabric mod** that counts how many blocks each player has mined and shows a live leaderboard on the scoreboard. Admins can restyle the title (including JSON components), move the display, toggle counting/visibility, and rebuild the board from vanilla statistics with `/minedcounter`.

Target: **Minecraft 26.1 – 26.3**, Fabric Loader **0.19.2+ / 0.20.1+**, Java **25+**. Each Minecraft version has its own jar; see the matrix below.

## Features

- Instant counting on valid harvests (no polling loop).
- Survival counting uses the same `hasCorrectToolForDrops` condition as vanilla `Stats.BLOCK_MINED`, so wrong-tool breaks such as punching stone do not score.
- Creates `minedcounter_total` and attaches it to the configured display slot automatically.
- Titles support classic `&` codes and standard JSON text components.
- Display positions: `sidebar`, `list`, `below_name`.
- `/minedcounter enable|disable` controls counting; `/minedcounter show|hide` controls only visibility.
- Creative-mode block breaks are ignored by default so live scores match vanilla statistics; `/minedcounter creative on|off` changes this.
- Simple `config/mined_counter.json` persisted settings.
- `/minedcounter init` rebuilds the board from vanilla stats, including offline players by default.
- Command feedback is available in Chinese and English; player feedback follows the client language.
- Automatically migrates the old `minecount_total` objective to `minedcounter_total`.
- Server-side only: vanilla clients see the scoreboard without installing anything.
- ~26 KB; checks the display slot once every 20 ticks, never iterates players.
- Fabric API is a soft dependency; without it the mod stays inactive instead of crashing the server.
- MIT licensed.

## Install

1. Use a release version of Minecraft **26.1 – 26.3** with the matching Fabric Loader.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for that Minecraft version.
3. Put the matching jar into `mods/`.
4. Restart the server with Java 25+.

| Minecraft | Fabric Loader | Fabric API | jar |
| --- | --- | --- | --- |
| 26.1 | 0.19.2+ | 0.155.3+26.1.2 | `mined-counter-1.2.0+26.1.jar` |
| 26.1.1 | 0.19.2+ | 0.155.3+26.1.2 | `mined-counter-1.2.0+26.1.1.jar` |
| 26.1.2 | 0.19.2+ | 0.155.3+26.1.2 | `mined-counter-1.2.0+26.1.2.jar` |
| 26.2 | 0.20.1+ | 0.161.0+26.2 | `mined-counter-1.2.0+26.2.jar` |
| 26.3 | 0.20.1+ | 0.167.0+26.3 | `mined-counter-1.2.0+26.3.jar` |

File naming follows the common Fabric convention `mined-counter-<modversion>+<minecraft>.jar`, the same pattern used by Fabric API, Sodium and Lithium. The `+26.1.2` suffix is SemVer build metadata, not part of the mod version. A loader segment such as `-fabric` is only needed when the project ships multiple loaders; this project is Fabric-only.

Upgrading from 1.1.0: replace the jar. The old objective is migrated automatically; a config file is generated on first start. The command was renamed from `/minedcount` to `/minedcounter`. If an older build already counted invalid or creative breaks, run `/minedcounter init` once after upgrading to rebuild from vanilla stats.

## Commands

Requires permission level 2 (OP / gamemaster). In singleplayer, cheats must be enabled.

| Command | Effect |
| --- | --- |
| `/minedcounter enable` | Resume counting |
| `/minedcounter disable` | Stop counting and hide the board; scores are kept |
| `/minedcounter show` | Show the board; counting continues |
| `/minedcounter hide` | Hide the board; counting continues |
| `/minedcounter title <title>` | Set title text/styles, `&` codes or JSON |
| `/minedcounter position <sidebar\|list\|below_name>` | Move the display |
| `/minedcounter creative on\|off` | Count creative block breaks (default off) |
| `/minedcounter init` | Reset and rebuild from online + offline stats |
| `/minedcounter init online` | Rebuild from online players only |
| `/minedcounter reload` | Reload `config/mined_counter.json` |
| `/minedcounter status` | Show current state |

## Titles

Legacy codes:

```mcfunction
/minedcounter title &6&l&nMined
```

Standard JSON component:

```mcfunction
/minedcounter title {"text":"Mined","color":"yellow","underlined":true,"bold":true}
```

Style-only JSON is applied to the current title text:

```mcfunction
/minedcounter title {color:"yellow";underlined:true}
```

The JSON parser accepts unquoted keys and semicolon separators, then stores canonical JSON. JSON titles are limited to 1024 characters; legacy titles to 256.

## Config

`config/mined_counter.json`:

```json
{
  "title": "&e&n挖掘榜",
  "sidebar": true,
  "position": "sidebar",
  "enabled": true,
  "visible": true,
  "countCreative": false
}
```

- `sidebar`: allow the mod to use the sidebar slot. When false, `sidebar` is blocked; `list` and `below_name` still work.
- `position`: `sidebar`, `list`, or `below_name`.
- `enabled`: counting on/off.
- `visible`: display on/off.
- `countCreative`: count creative block breaks (default false).

Commands write these values back. Manual edits take effect after `/minedcounter reload`.

## Creative mode

The default `countCreative=false` ignores creative block breaks. This keeps live scores consistent with vanilla `Stats.BLOCK_MINED` (which does not record creative breaks), so `/minedcounter init` does not silently drop creative-only points.

If enabled, creative breaks are counted live (all of them, without the tool check), but `/minedcounter init` cannot restore them from vanilla stats; they will be cleared on rebuild.

## Valid harvests

In survival, a break only scores when `ServerPlayer.hasCorrectToolForDrops(state)` is true. Vanilla `ServerPlayerGameMode.destroyBlock` uses the same condition before calling `Block.playerDestroy`, and that is where `Stats.BLOCK_MINED` is incremented. Consequences:

- Punching stone or mining diamond ore with a wooden pickaxe does not score (vanilla does not record it either).
- Punching dirt or sand does score (no tool is required, and vanilla records it).
- Explosions, pistons and natural decay do not score.

This keeps the live survival board and `/minedcounter init` based on the same set of blocks.

## Why init stays

After this fix, both live counting and `init` use the same "valid harvest" definition, so `init` remains safe:

- New server/world: don't run `init`; counting starts when the mod is deployed.
- Upgrading from an older build: run `init` once to rebuild from vanilla stats and clean up scores that were counted under the old rules.
- Offline players: `init` is the only path that rebuilds their history from `world/players/stats`.

If you want "count from deployment only", simply don't run `init`; it never runs automatically.

## Init semantics

`/minedcounter init` removes and recreates `minedcounter_total`, reads live online stats, then scans `world/players/stats/*.json` for offline players and maps UUIDs through `usercache.json`. Block mining stats are summed per player.

- Only player block breaks are counted.
- Survival counting only includes valid harvests (correct tool / normal drops); invalid breaks such as punching stone are ignored.
- Reading all offline stats is a one-time I/O cost; on very large servers use `/minedcounter init online` and run at low traffic.
- Offline players missing from `usercache.json` are skipped.

## Localisation

Command feedback is Chinese for `zh_*` clients and English otherwise. Console/RCON uses the server operating-system language. Command and argument names stay in English.

## Compatibility

| Item | Version |
| --- | --- |
| Minecraft | 26.1 / 26.1.1 / 26.1.2 / 26.2 / 26.3, one jar each |
| Fabric Loader | 26.1.x: 0.19.2+; 26.2 / 26.3: 0.20.1+ |
| Fabric API | 26.1.x: 0.155.3+26.1.2; 26.2: 0.161.0+26.2; 26.3: 0.167.0+26.3 |
| Java | 25+ |
| Side | server / singleplayer |

The mod owns the configured display slot and will conflict with other scoreboard mods using the same slot. Only block breaks are counted; explosions and pistons are ignored. Survival only counts valid harvests, matching vanilla statistics. All five targets compile from the same sources; run a smoke test on 26.2/26.3 before publishing.

## Fake players and case sensitivity

This is vanilla behaviour, not a mod bug:

- Scoreboard storage is keyed by the exact `getScoreboardName()` string and is **case-sensitive**, so `Galaxy_1118` and `galaxy_1118` are two entries.
- `/scoreboard players ...` resolves names through `PlayerList.getPlayerByName()`, which uses `equalsIgnoreCase`; while the real player is online, `galaxy_1118` matches `Galaxy_1118`.
- The mod only increments the `ServerPlayer` that broke the block; fake-player entries come from the fake-player mod or manual commands.

Use a fake-player name that differs by more than case (for example `bot_galaxy`).

## Build

No Gradle/Loom: all 26.x production jars ship with Mojang class names, so plain `javac` works.

```powershell
.\build-versions.ps1
```

Output: `dist/mined-counter-1.2.0+<minecraft>.jar` for every target in `versions.json`. The legacy `build.ps1` builds only the local 26.1.2 package.

## Alternatives

- [BlocksMined](https://modrinth.com/mod/blocksmined) — mined/placed stats, colour and display options, 26.1.2, closed source.
- [Minecraft Digging Leaderboard](https://github.com/iL6hua/Minecraft-Digging-Leaderboard) — config file, TPS display, up to 1.21.11.
- [Simple Statistic List](https://github.com/MShower/Simple-Statistic-List) — multiple rotating stats, up to 1.21.10.

Mined Counter intentionally stays small: one objective, zero-config defaults, admin commands, MIT.

## License

[MIT](LICENSE) © 2026 Galaxy-1118

</details>
