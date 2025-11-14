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
