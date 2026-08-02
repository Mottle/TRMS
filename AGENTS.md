# Repository Guidelines

## 项目结构与模块组织

TRMS 是一个面向 Java 25 和 Minecraft 26.1.2 的 Gradle 多项目构建。

- `common/` 存放与运行端无关的 Java 协议、持久化和模具图案逻辑。它不得依赖 Minecraft、NeoForge、Horizon、渲染或世界状态。
- `extension/` 是 Horizon 服务端 Extension。Java 源码位于 `src/main/java`，描述文件和数据资源位于 `src/main/resources`。
- `mod/` 是配套的 NeoForge **仅客户端** Mod；客户端代码位于 `src/main/java/moe/liar/trms/client`，资源位于 `src/main/resources/assets/trms`。
- 每个模块将 JUnit 测试放在 `src/test/java`。共享契约测试夹具位于 `docs/contracts/`；设计与 API 文档位于 `docs/`。

不要将 `.gradle/`、`extension/run/`、`extension/run-assets/` 或 `mod/runs/` 视为源码输入；它们是生成文件或本地运行时状态。

## 构建、测试与开发命令

在仓库根目录通过 Gradle Wrapper 执行：

```bash
./gradlew verifyTrms                  # 构建并测试全部产物
./gradlew :common:test                # 测试纯共享逻辑
./gradlew :extension:test             # 测试 Horizon Extension 逻辑
./gradlew :mod:test                   # 运行具有 NeoForge 环境的 Mod 测试
./gradlew :extension:runHorizonServer # 启动受管理的本地服务端
./gradlew :mod:runClient              # 启动本地 NeoForge 客户端
```

Extension 从 `mavenLocal()` 解析 Horizon 依赖。连接客户端至 `127.0.0.1:25565` 前，先启动服务端。

## 编码风格与命名约定

使用 Java 25、UTF-8、四个空格缩进，并沿用现有明确且防御性的风格。包名保持在 `moe.liar.trms` 下；类型使用 `PascalCase`，方法和字段使用 `camelCase`，常量使用 `UPPER_SNAKE_CASE`。保持端侧边界：共享契约属于 `common`，客户端渲染和输入属于 `mod`，权威校验和世界状态修改属于 `extension`。为公开或不直观的行为添加 JavaDoc，特别是协议、持久化和线程规则。构建启用了 `-Xlint:all`；请处理新增的编译器警告。

## 测试准则

使用 JUnit Jupiter 5。测试类以 `Test` 结尾；测试方法使用描述行为的 lower-camel 名称，例如 `encodesExactlyTwentyFiveCanonicalBytes`。在修改代码附近添加聚焦的单元测试；变更网络传输格式或模具语义时，更新所有受影响的端点和契约测试。项目未配置覆盖率阈值；最终必须执行 `./gradlew verifyTrms`。

## 提交与拉取请求

当前检出目录没有 `.git` 目录，无法核实历史提交惯例。请使用简洁的祈使式主题，建议采用 `type(scope): summary`，例如 `fix(extension): reject stale carve revisions`。拉取请求应说明行为与兼容性影响；有对应议题时附上链接；列出已执行的 Gradle 命令；涉及可见客户端渲染变更时附截图或短录屏。将面向用户的配置或协议变更记录在 `docs/` 中，并在适用时更新契约测试夹具。
