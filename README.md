# Trae WebTools

通用后端工具集（Java 8，零第三方依赖）。涵盖 Web/HTTP、Security、Text/Regex、JSON/XML、IO/Path/Stream、Time/Date、Collections/Maps/Numbers、Concurrency/Retry/Cache、Net/URL 等常用场景。

## 特性
- Web/HTTP：分页、响应包装、CORS、ETag/条件请求、Range、状态分类/状态行
- Security：SHA-256/MD5/SHA-1、HMAC、Base64/Base64URL、JWT/CSRF、PBKDF2、Bcrypt
- Text/Regex：编辑距离/相似度、命名风格判断、正则封装、模板引擎（{{key}}）
- JSON/XML：平面 JSON 与 Properties 互转、JSON Diff/Apply、XML 解析与 XPath
- IO/Path/Stream：文件/流读写、压缩/解压、路径规范化、魔数检测
- Time/Date：RFC1123/850/asctime、时长解析/格式化、秒表
- Collections/Numbers：集合/Map 增强、数值钳制/舍入/统计
- Concurrency/Cache：重试/回退、调度器、令牌桶限流、LRU/TTL/LFU 缓存

## 环境
- JDK：8+
- 编码：UTF-8（含中文注释，如使用 GBK 编译会报不可映射字符）

## 快速开始（Maven）
```xml
<dependency>
  <groupId>com.trae</groupId>
  <artifactId>webtools</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```
编译配置：
```xml
<properties>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <maven.compiler.source>1.8</maven.compiler.source>
  <maven.compiler.target>1.8</maven.compiler.target>
</properties>
```
本地安装：在仓库根目录执行 `mvn clean install`

## 团队日常使用指南
- 编译：`mvn compile`
  - 场景：增量验证语法与依赖是否正确，快速检查。
- 打包：`mvn package`；干净构建：`mvn clean package`
  - 场景：本工程交付与自测，生成分发产物（`target/*.jar`）。
- 安装供其他项目依赖：`mvn clean install`
  - 场景：将产物安装到本地仓库（`~/.m2/repository`），供其他项目通过坐标引用。
- 运行：`java -jar target/<artifactId>-<version>.jar`；或 `mvn -q exec:java -Dexec.mainClass=com.example.Main`
  - 场景：本地联调与回归验证。
- 清理：`mvn clean`
  - 场景：切换分支、构建配置或资源处理变更、旧产物干扰时。
- 重新编译打包并清理：`mvn clean package`；需要可被依赖则 `mvn clean install`
  - 场景：CI/发布前可重复构建、重大依赖或生成物变更后。
- 加速与测试策略：
  - 跳过测试执行但编译：`-DskipTests=true`
  - 完全跳过测试编译与执行：`-Dmaven.test.skip=true`
  - 并行：`mvn -T 1C clean package`
  - 离线：`mvn -o clean package`
- 多模块工程：根目录执行 `clean install` 会按模块顺序构建并安装；上游模块先构建，保证下游解析到最新坐标。
- 本地仓库路径（Windows）：`C:\Users\<用户名>\.m2\repository`

## mvn clean package 与 mvn clean install 的区别
- 执行阶段：
  - `mvn clean package`：执行到 `package` 为止（不包含 `verify/install`）。
  - `mvn clean install`：执行到 `install`，包含 `verify` 与所有中间阶段。
- 产物去向：
  - `package`：仅在当前工程 `target/` 目录生成 Jar/War。
  - `install`：除生成 `target/` 产物外，还将产物安装到本地仓库 `~/.m2/repository`（Windows：`C:\Users\<用户名>\.m2\repository`），供其他项目通过坐标依赖。
- 测试与验证：
  - 两者均会执行测试（除非 `-DskipTests=true` 或 `-Dmaven.test.skip=true`）。
  - 若绑定了 Failsafe 的集成测试在 `verify`，则只有 `install` 会跑并验证集成测试结果。
- 适用场景：
  - 仅生成分发包用于本项目自测/交付 → 用 `clean package`。
  - 需要让其他项目通过坐标依赖当前产物 → 用 `clean install`。
  - 多模块聚合构建并希望各模块产物进入本地仓库 → 用 `clean install`。
- 常用加速参数：
  - 跳过测试但仍编译测试源码：`-DskipTests=true`
  - 完全跳过测试编译与执行：`-Dmaven.test.skip=true`
  - 并行：`mvn -T 1C clean package`
  - 离线：`mvn -o clean package`

## 发布到 GitHub Releases 的注意事项
- 使用稳定版本号：
  - 在 `pom.xml` 设置非 SNAPSHOT 版本（如 `1.0.0`），避免快照命名用于发布。
- 构建产物：
  - 执行 `mvn -q -DskipTests=true clean package`，产物位于 `target/<artifactId>-<version>.jar`。
- 创建 Release：
  - 网页方式：在 GitHub 仓库的 Releases 页面创建版本标签与说明，上传 Jar 文件。
  - CLI 示例：
    - 创建：`gh release create v1.0.0 -t "v1.0.0" -n "变更说明..."`
    - 上传：`gh release upload v1.0.0 target/<artifactId>-<version>.jar`
- 命名与校验：
  - 建议在 Release 说明中注明构建命令、JDK 版本与依赖最小要求。
  - 可选上传校验文件（如 `SHA256SUMS`），提升可验证性。
- 与 Maven 仓库发布的区别：
  - GitHub Releases 仅托管二进制包，不需要 `mvn deploy`。
  - 若需发布到 Maven 仓库（Nexus/Artifactory/Maven Central），需配置 `distributionManagement` 并使用 `mvn clean deploy`，可能还需签名与校验（sources/javadoc）。

## 前端：`npm run dev` 与 `npm run build`（可选）
- 适用范围：使用前端工具链（如 `Vite`、`Webpack`、`Next.js`）的项目；与本 Java 工程独立，但经常协同。
- `npm run dev`：
  - 启动开发服务器，支持热更新（HMR/Hot Reload）、源映射、未压缩代码。
  - 常见行为：本地代理、快速增量编译、错误提示友好；不适合作为生产环境。
  - 示例：`npm run dev`（Vite 默认端口 5173，可 `--host` 暴露到局域网）。
- `npm run build`：
  - 生成生产环境构建产物，进行打包、压缩、代码分割、哈希指纹（缓存友好）。
  - 常见输出目录：`dist/`（Vite/多数 SPA）、`.next/`（Next.js）、`build/`（部分脚手架）。
  - 示例：`npm run build`；Vite 可配合 `npm run preview` 做本地预览。
- 区别与选择：
  - 开发联调与快速迭代 → 用 `npm run dev`。
  - 部署/发布到静态服务器或反向代理（Nginx/CDN） → 用 `npm run build` 并上传构建目录。
  - 环境变量：开发与生产通常使用不同 `.env` 文件（如 `.env.development` vs `.env.production`）。
- 与后端协同：
  - 纯前后端分离：前端 `dist/` 直接交付到静态服务，后端仅提供 API。
  - 打包进后端：可将 `dist/` 置于后端资源目录（如 `src/main/resources/static/`），由 Java 服务统一对外提供。

## 示例
- 分页与响应
```java
Page<String> p = WebUtils.paginate(Arrays.asList("a","b","c"), 1, 2);
ApiResponse<Page<String>> rsp = WebUtils.ok(p);
```
- 安全摘要与 Bcrypt 口令
```java
String sha = SecurityUtils.sha256Hex("hello".getBytes(StandardCharsets.UTF_8));
String hash = SecurityUtils.bcryptHash("password123", 12);
boolean ok = SecurityUtils.bcryptVerify("password123", hash);
```
- URL 解析与构造
```java
Map<String,String> parts = UrlUtils.parseUrlComponents("https://ex.com/a/b?p=1#f");
String url = UrlUtils.buildUrlFromComponents(parts);
```
- ETag 与条件请求
```java
String etag = ETagPlusUtils.strongFromString("body");
boolean notModified = ETagPlusUtils.ifNoneMatch(etag, etag);
```

## 文件索引
- 打开 `ToolsIndex.html`（或 `ToolsIndex.md`）按类别浏览并跳转到源码文件。

## 约定与注意
- 工具类集中在包 `com.trae.webtools`，按领域命名（如 `SecurityUtils`、`UrlUtils`）
- 安全工具：Bcrypt 成本建议 10–14，完整存储返回的 60 字符 Bcrypt 文本即可；登录后可透明升级成本
- 仅使用 JDK 标准库，无第三方依赖；如需外部发布建议配置私有 Maven 仓库（Nexus/Artifactory）

## 许可
本项目仅示例用途，按内部策略使用；如需开源许可请在此处添加。
