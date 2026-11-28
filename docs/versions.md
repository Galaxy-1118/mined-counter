# 26.x 版本清单

构建日期：2026-11-28。模组版本保持 **1.2.0**，文件名使用 `mined-counter-1.2.0+<Minecraft版本>.jar`。

| Minecraft | Fabric Loader | Fabric API | 文件 | SHA-256 |
| --- | --- | --- | --- | --- |
| 26.1 | 0.19.2 | 0.155.3+26.1.2 | `mined-counter-1.2.0+26.1.jar` | `dbe0517b80300b258642f34f5f063ecc7bc0fa2111d2a30905f6ae6533cb73f6` |
| 26.1.1 | 0.19.2 | 0.155.3+26.1.2 | `mined-counter-1.2.0+26.1.1.jar` | `d8a77760be5d6b65f9e3e044a32a746457c8159de923495d979b220fd54acb4c` |
| 26.1.2 | 0.19.2 | 0.155.3+26.1.2 | `mined-counter-1.2.0+26.1.2.jar` | `c038fb3a4953e24f8b58d92b62603495b6f1c50666a7d1adc20ddfe0c4c316ff` |
| 26.2 | 0.20.1 | 0.161.0+26.2 | `mined-counter-1.2.0+26.2.jar` | `42e35825ee7c984cf22f01c025de0a8c880bb32726eba60474afb92f96d3c2f5` |
| 26.3 | 0.20.1 | 0.167.0+26.3 | `mined-counter-1.2.0+26.3.jar` | `28d9820d4b91e8640f452a8dd796bde40fbd466cc5597b15d2d8a2d5b4fe21f9` |

## 说明

- 26.1 / 26.1.1 / 26.1.2 使用同一份源码和 Fabric API 0.155.3+26.1.2 构建；26.1 的 jar 覆盖整个 26.1.x，26.1.1 / 26.1.2 的 jar 从对应补丁版本起兼容。按精确版本分发只是为了下载时更直观。
- 26.2 和 26.3 使用各自版本的 Fabric API 构建；26.2/26.3 需要 Fabric Loader 0.20.1+。
- 26.x 的 Minecraft / Fabric API 要求 Java 25；jar 的 `fabric.mod.json` 已声明 `java >=25`。
- 26.2/26.3 已完成编译验证；26.1.2 已在本地 Fabric 服务端做过运行冒烟测试。正式发布前建议在 26.2/26.3 服务端各跑一次 `/minedcounter status`。

## 文件命名

```text
mined-counter-<modversion>+<minecraft>.jar
mined-counter-1.2.0+26.1.2.jar
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

当前 GitHub 的 `v1.2.0` Release 是最初的单版本 26.1.2 包；上述 5 个 jar 是本地多版本构建产物，尚未上传。发布时可以选择追加到现有 `v1.2.0` Release，或按新的模组版本创建 `v1.2.1`/`v1.3.0` Release。
