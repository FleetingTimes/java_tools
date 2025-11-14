# Java 项目生命周期命令速查

本文列出在 Maven/Gradle/纯 Java 下，围绕“编译 → 测试 → 打包 → 运行 → 安装/部署 → 清理”的常用命令与对应生命周期阶段。

## Maven
- 编译：`mvn compile`
- 运行测试：`mvn test`（跳过测试：`mvn test -DskipTests` 或在打包时 `mvn package -DskipTests`）
- 打包 Jar：`mvn clean package`（产物在 `target/*.jar`）
- 安装到本地仓库：`mvn install`
- 部署到远端仓库：`mvn deploy`（需配置仓库）
- 验证阶段：`mvn verify`
- 清理：`mvn clean`
- 运行应用：
  - 可执行 Jar：`java -jar target/your-app.jar`
  - 指定主类（需 exec 插件）：`mvn -q exec:java -Dexec.mainClass=com.example.Main`
- 常用参数：
  - 并行和跳过测试：`mvn -T 1C -DskipTests clean package`
  - 强制 UTF-8 编译（若未在 pom 配置）：`mvn -Dproject.build.sourceEncoding=UTF-8 clean package`

### 生命周期与命令对应（Maven）
- validate → `mvn validate`
- compile → `mvn compile`
- test → `mvn test`
- package → `mvn package`
- verify → `mvn verify`
- install → `mvn install`
- deploy → `mvn deploy`
- clean → `mvn clean`

### mvn clean install 详解
- 命令语义：
  - `clean` 删除构建输出目录（默认 `target/`），确保从零开始构建。
  - `install` 执行完整默认生命周期（validate → compile → test → package → verify → install），并将构建产物安装到本地仓库（`~/.m2/repository`）。
- 常见用法：
  - 标准构建并安装：`mvn clean install`
  - 跳过测试加速（仍编译测试源码）：`mvn clean install -DskipTests=true`
  - 完全跳过测试（不编译也不执行）：`mvn clean install -Dmaven.test.skip=true`
  - 并行构建（按 CPU 核心）：`mvn -T 1C clean install`
- 产物位置：
  - Jar 输出：`target/<artifactId>-<version>.jar`
  - 本地仓库：`~/.m2/repository/<groupId>/<artifactId>/<version>/<artifactId>-<version>.jar`
  - Windows 本地仓库默认路径：`C:\Users\<你的用户名>\.m2\repository`
- 多模块工程：
  - 在聚合工程根目录执行 `clean install` 会按模块顺序构建并将每个模块产物安装到本地仓库。
  - 依赖的上游模块会先构建安装，保证下游模块解析到最新坐标。
- 何时使用 `install` 与 `package`：
  - `package` 仅生成产物于 `target/`，适合本工程自测或临时运行。
  - `install` 将产物放入本地仓库，适合让其他项目通过 Maven 坐标依赖本工程。
- SNAPSHOT 提示：
  - 使用 `-SNAPSHOT` 版本表示开发中快照；安装到本地仓库后，其他项目会解析到当前快照版本。
  - 部署到远端仓库时，快照可能带时间戳以实现唯一性；本地安装保持 `-SNAPSHOT` 命名。
- 被其他项目引用：
  - 在其他项目的 `pom.xml` 中添加依赖坐标（与本项目 `groupId/artifactId/version` 一致），即可从本地仓库解析。

## Gradle
- 编译/打包：`gradle build`（或 `./gradlew build`）
- 运行测试：`gradle test`
- 生成 Jar：`gradle jar`（产物在 `build/libs/*.jar`）
- 运行应用（需 application 插件或 JavaExec）：`gradle run`
- 清理：`gradle clean`
- 运行 Jar：`java -jar build/libs/你的Jar文件名.jar`

## 纯 Java（不借助构建工具）
- 编译源到目标目录（确保 UTF-8）：
  - `javac -encoding UTF-8 -d target/classes src/main/java/com/example/Main.java`
- 运行 class：
  - `java -cp target/classes com.example.Main`
- 打包为可执行 Jar（JDK8）：
  - `jar cfe target/app.jar com.example.Main -C target/classes .`
  - 运行：`java -jar target/app.jar`

## 编码与版本建议
- 编译与运行统一使用 UTF-8（中文注释避免 GBK 环境的不可映射字符）
- JDK 版本建议 1.8+；必要时在运行命令附加 `-Dfile.encoding=UTF-8`

## 提示
- 当前仓库已提供 `pom.xml`，可直接执行 `mvn clean install` 生成并安装 `jar` 到本地仓库，在其他项目通过依赖坐标引入。

## 编译/打包/运行/清理与适用场景
- 编译（只编译源码，不打包）：
  - Maven：`mvn compile`
  - Gradle：`gradle classes`
  - 适用场景：快速增量验证语法/依赖是否正确；IDE 外部批量编译。
- 打包（生成可分发产物）：
  - Maven：`mvn package`；彻底从零开始：`mvn clean package`
  - Gradle：`gradle jar` 或 `gradle build`
  - 适用场景：本工程自测、交付 jar/war；确保包含资源与清单。
- 运行（本地验证功能）：
  - 可执行 Jar：`java -jar target/<artifactId>-<version>.jar`
  - Maven 执行主类（需 exec 插件或已配置主类）：`mvn -q exec:java -Dexec.mainClass=com.example.Main`
  - Gradle（需 application 插件）：`gradle run`
  - 适用场景：联调、本地回归、脚本化验证。
- 清理（删除构建输出）：
  - Maven：`mvn clean`
  - Gradle：`gradle clean`
  - 适用场景：切换分支后、插件/资源处理配置变更、遇到构建不一致或旧产物干扰时。
- 重新编译打包并清理（干净构建）：
  - Maven：`mvn clean package`（需要其他项目引用则用 `mvn clean install`）
  - Gradle：`gradle clean build`
  - 适用场景：CI/发布前的可重复构建；修复“莫名其妙”的编译/打包问题；重大依赖或生成物变化后。

### 加速与测试策略
- 跳过测试但仍编译测试源码：`-DskipTests=true`（如 `mvn -DskipTests clean package`）
- 完全跳过测试编译与执行：`-Dmaven.test.skip=true`
- 并行构建：`mvn -T 1C clean package`（按 CPU 核心数）
- 离线构建：`mvn -o clean package`（不访问远端仓库，使用本地缓存）

