# 更新日志 / Changelog

## 1.2.0 — 2026-11-22

### 新增 / Added
- `/minedcount` 管理指令：`enable`、`disable`、`show`、`hide`、`title`、`position`、`init`、`init online`、`reload`、`status`。
- 标题样式指令，支持 `&` 颜色、粗体、下划线、斜体、删除线、混淆和重置代码。
- 显示位置切换：`sidebar`、`list`、`below_name`。
- `config/mined_counter.json` 配置文件：标题、sidebar 开关、位置、统计/显示状态。
- `/minedcount init`：重置计分板并从原版统计重建；默认读取在线玩家和 `world/players/stats` 中的离线玩家，`init online` 只处理在线玩家。
- 初始化时通过 `usercache.json` 把离线玩家 UUID 映射回名字。

### 变更 / Changed
- 计分板显示逻辑改为根据配置管理显示位，并区分“统计开关”和“显示开关”。
- 每 20 tick 的检查只维护目标显示位，不遍历玩家。
- 升级到 1.2.0 后首次启动会生成配置文件；已有的 `minecount_total` 分数保留。

### 文档 / Docs
- 增加指令、配置、初始化、假人与大小写问题的说明。

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
