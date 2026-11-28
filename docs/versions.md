# 26.x 版本清单

构建日期：2026-11-28。当前模组版本 **1.2.1**，文件名使用 `mined-counter-1.2.1+<Minecraft版本>.jar`。

| Minecraft | Fabric Loader | Fabric API | 文件 | SHA-256 |
| --- | --- | --- | --- | --- |
| 26.1 | 0.19.2 | 0.155.3+26.1.2 | `mined-counter-1.2.1+26.1.jar` | `7db7b7dd18be7a78b826fa6a361c5e92733216742d687c63ef080bb93d96593c` |
| 26.1.1 | 0.19.2 | 0.155.3+26.1.2 | `mined-counter-1.2.1+26.1.1.jar` | `f8796c1b87b0de23b4253c6996a33d3f4711fd264549c7201999b99709b3687d` |
| 26.1.2 | 0.19.2 | 0.155.3+26.1.2 | `mined-counter-1.2.1+26.1.2.jar` | `afa9a04336e2ae0b61d10a979d439be2556539758a2c8e894cc90fca0655a8e3` |
| 26.2 | 0.20.1 | 0.161.0+26.2 | `mined-counter-1.2.1+26.2.jar` | `d276c01dcedfd49b8f892e5e756e92d0edde2d1456561e9ae828b45028cd7db3` |
| 26.3 | 0.20.1 | 0.167.0+26.3 | `mined-counter-1.2.1+26.3.jar` | `971457d2f362433cb0085925adf084568560904bae3e75e851f14b1e8fa25fa8` |

## 说明

- 26.1 / 26.1.1 / 26.1.2 使用同一份源码和 Fabric API 0.155.3+26.1.2 构建；26.1 的 jar 覆盖整个 26.1.x，26.1.1 / 26.1.2 的 jar 从对应补丁版本起兼容。按精确版本分发只是为了下载时更直观。
- 26.2 和 26.3 使用各自版本的 Fabric API 构建；26.2/26.3 需要 Fabric Loader 0.20.1+。
- 26.x 的 Minecraft / Fabric API 要求 Java 25；jar 的 `fabric.mod.json` 已声明 `java >=25`。
- 26.2/26.3 已完成编译验证；26.1.2 已在本地 Fabric 服务端做过运行冒烟测试。正式发布前建议在 26.2/26.3 服务端各跑一次 `/minedcounter status`。

## 文件命名

```text
mined-counter-<modversion>+<minecraft>.jar
mined-counter-1.2.1+26.1.2.jar
```

`+` 后面是 SemVer build metadata，不是模组版本的一部分；Fabric API、Sodium、Lithium 等使用相同习惯。只有同时发布多个加载器时才需要 `-fabric` / `-neoforge` 段，本模组目前仅 Fabric。

## 重新构建

```powershell
.\build-versions.ps1
```

依赖目录结构（默认 `D:\Deepseek Harness\mc-versions`）：

```text
<minecraft>/
├── <minecraft>.jar
├── fabric-loader.jar
└── fabric-api.jar
```

## 发布状态

- `v1.2.1`：26.1 – 26.3 多版本发布，包含上表 5 个 jar。
- `v1.2.0`：保留的旧单版本发布（仅 26.1.2 的 `mined-counter-1.2.0.jar`）。
- 两个 Release 都保留，方便旧用户继续下载。
