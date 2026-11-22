# 更新日志 / Changelog

## 1.2.0 — 2026-11-22

### 新增 / Added
- `/minedcounter` 管理指令：`enable`、`disable`、`show`、`hide`、`title`、`position`、`creative`、`init`、`init online`、`reload`、`status`。
- 标题支持标准 JSON 文本组件和“只写样式”的简写，例如 `{"text":"挖掘榜","color":"yellow","underlined":true}`、`{color:"yellow";underlined:true}`。
- 显示位置切换：`sidebar`、`list`、`below_name`。
- `config/mined_counter.json` 配置文件：标题、sidebar 开关、位置、统计/显示状态、创造模式计数。
- `/minedcounter init`：重置计分板并从原版统计重建；默认读取在线玩家和 `world/players/stats` 中的离线玩家，`init online` 只处理在线玩家。
- 初始化时通过 `usercache.json` 把离线玩家 UUID 映射回名字。
- 指令回显中英双语：玩家跟随客户端语言，控制台跟随服务器系统语言。
- `/minedcounter creative on|off`：控制是否统计创造模式破坏方块，默认关闭。

### 变更 / Changed
- 指令名从 `/minedcount` 统一为 `/minedcounter`。
- 计分项名从 `minecount_total` 统一为 `minedcounter_total`；启动时自动复制旧分数并删除旧计分项，不会清空榜单。
- 默认忽略创造模式破坏方块，使实时计数与原版 `Stats.BLOCK_MINED`、`/minedcounter init` 保持一致。
- 生存模式按 `ServerPlayer.hasCorrectToolForDrops(state)` 过滤：徒手挖石头、错误工具挖矿等无效采集不再计分，和原版 `minecraft.mined` 统计、`/minedcounter init` 使用同一条件。
- 计分板显示逻辑改为根据配置管理显示位，并区分“统计开关”和“显示开关”。
- 每 20 tick 的检查只维护目标显示位，不遍历玩家。

### 文档 / Docs
- 增加 JSON 标题、创造模式方案、双语回显、旧数据迁移、假人与大小写问题的说明。
- 增加 `examples/mined_counter.json` 配置示例。

## 1.1.0 — 2026-10-11

### 新增 / Added
- 右侧边栏“挖掘榜”，玩家破坏方块后即时 +1。
- 自动创建计分项 `minecount_total` 并挂到 sidebar。
- 启动时自动清理 1.0.0 遗留的 `minecount_pool` 计分项。

### 变更 / Changed
- 从“两个计分项 + 每分钟汇总”改为“单个计分项 + 破坏瞬间累加”。
- Fabric API 改为软依赖：缺失时模组自动停用并输出警告，不再阻止服务器启动。
- 每 20 tick 只检查一次边栏指向，不遍历玩家列表。

### 修复 / Fixed
- 1.0.0 中依赖 Fabric API 才能启动的问题。

## 1.0.0 — 2026-10-11

- 首个版本：每玩家两个计分项，每 60 秒汇总一次并刷新边栏。
