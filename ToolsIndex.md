# WebTools 索引

> 快速按类别浏览工具文件，点击路径可在 IDE 中定位。

## Web / HTTP
- WebUtils — 分页、响应包装、校验、脱敏、MIME/CORS/IP、ETag、版本比较
  - `src/main/java/com/trae/webtools/WebUtils.java`
- WebExtraUtils — 内容协商、弱 ETag、HTTPS 判定、分页链接、Ajax/语言、UA、Range、同源/CORS、TraceId
  - `src/main/java/com/trae/webtools/WebExtraUtils.java`
- HttpUtils — 查询参数、Cookie、URL 编解码与路径拼接
  - `src/main/java/com/trae/webtools/HttpUtils.java`
- HttpHeaderPlusUtils — 头名称规范化、合并、增删改查
  - `src/main/java/com/trae/webtools/HttpHeaderPlusUtils.java`
- HttpRangePlusUtils — Content-Range 构造与单段 Range 校验/规范化
  - `src/main/java/com/trae/webtools/HttpRangePlusUtils.java`
- HttpStatusUtils — 原因短语与分类、状态行构造/解析
  - `src/main/java/com/trae/webtools/HttpStatusUtils.java`
- ETagPlusUtils — 强/弱 ETag 生成、条件请求判断与解析
  - `src/main/java/com/trae/webtools/ETagPlusUtils.java`

## Security
- SecurityUtils — 摘要/HMAC/Base64/Base64URL、JWT/CSRF、Cookie 值、PBKDF2、Bcrypt
  - `src/main/java/com/trae/webtools/SecurityUtils.java`
- UUIDPlusUtils — Base64URL 短编码与 UUID 字节互转
  - `src/main/java/com/trae/webtools/UUIDPlusUtils.java`
- Base32Utils — RFC4648 Base32 编解码
  - `src/main/java/com/trae/webtools/Base32Utils.java`

## Text / Regex
- StringUtils / StringPlusUtils — 字符串增强
  - `src/main/java/com/trae/webtools/StringUtils.java`
  - `src/main/java/com/trae/webtools/StringPlusUtils.java`
- StringMatchUtils — 命名风格判断、近似相等、包含/前后缀、Unicode/ASCII、正则辅助
  - `src/main/java/com/trae/webtools/StringMatchUtils.java`
- TextMetricsUtils — 编辑距离、相似度、LCS/最长子串、分词/规范化
  - `src/main/java/com/trae/webtools/TextMetricsUtils.java`
- TemplateUtils — {{key}} 模板渲染、严格模式、文件读写
  - `src/main/java/com/trae/webtools/TemplateUtils.java`
- RegexUtils — 正则封装（编译/匹配/替换）
  - `src/main/java/com/trae/webtools/RegexUtils.java`

## JSON / XML
- JsonPropsUtils — 平面 JSON/Properties 互转
  - `src/main/java/com/trae/webtools/JsonPropsUtils.java`
- JsonDiffUtils — 平面 Map Diff/Apply
  - `src/main/java/com/trae/webtools/JsonDiffUtils.java`
- XMLUtils — 解析/序列化/节点操作
  - `src/main/java/com/trae/webtools/XMLUtils.java`
- XmlXPathUtils — XPath 评估与节点操作
  - `src/main/java/com/trae/webtools/XmlXPathUtils.java`

## IO / Path / Stream / Zip
- IOUtils — 文件与流读写、路径规范化、压缩/解压、资源读取
  - `src/main/java/com/trae/webtools/IOUtils.java`
- FilePlusUtils / FileTreeUtils — 文件范围读写与文件树操作
  - `src/main/java/com/trae/webtools/FilePlusUtils.java`
  - `src/main/java/com/trae/webtools/FileTreeUtils.java`
- StreamPlusUtils — 流复制、读写字符串、GZIP 压缩/解压
  - `src/main/java/com/trae/webtools/StreamPlusUtils.java`
- ZipUtils — 压缩/解压/条目操作
  - `src/main/java/com/trae/webtools/ZipUtils.java`
- PathPlusUtils — 路径与文件名增强
  - `src/main/java/com/trae/webtools/PathPlusUtils.java`
- MimeMagicUtils — 魔数检测（常见类型）
  - `src/main/java/com/trae/webtools/MimeMagicUtils.java`

## Time / Date
- DateTimeUtils / TimePlusUtils / DateRangeUtils — 时间/时长操作与范围
  - `src/main/java/com/trae/webtools/DateTimeUtils.java`
  - `src/main/java/com/trae/webtools/TimePlusUtils.java`
  - `src/main/java/com/trae/webtools/DateRangeUtils.java`
- RfcDateUtils — RFC1123/850/asctime 格式化与解析
  - `src/main/java/com/trae/webtools/RfcDateUtils.java`
- Stopwatch — 耗时测量（单调时钟）
  - `src/main/java/com/trae/webtools/Stopwatch.java`

## Collections / Maps / Numbers
- CollectionUtils / CollectionPlusUtils — 集合增强
  - `src/main/java/com/trae/webtools/CollectionUtils.java`
  - `src/main/java/com/trae/webtools/CollectionPlusUtils.java`
- MapUtils / MapPlusUtils — Map 增强
  - `src/main/java/com/trae/webtools/MapUtils.java`
  - `src/main/java/com/trae/webtools/MapPlusUtils.java`
- NumberUtils — 钳制/舍入/格式化/范围判断/随机/统计
  - `src/main/java/com/trae/webtools/NumberUtils.java`

## Concurrency / Retry / Cache
- ConcurrencyUtils — 睡眠、重试、超时执行、线程池
  - `src/main/java/com/trae/webtools/ConcurrencyUtils.java`
- RetryPlusUtils / BackoffUtils — 重试与回退策略
  - `src/main/java/com/trae/webtools/RetryPlusUtils.java`
  - `src/main/java/com/trae/webtools/BackoffUtils.java`
- CacheUtils / CacheAdvancedUtils — 速率限制器、LRU/TTL/LFU 缓存工厂
  - `src/main/java/com/trae/webtools/CacheUtils.java`
  - `src/main/java/com/trae/webtools/CacheAdvancedUtils.java`
- LruCache / RateLimiter — LRU 缓存与令牌桶限流
  - `src/main/java/com/trae/webtools/LruCache.java`
  - `src/main/java/com/trae/webtools/RateLimiter.java`
- SchedulerUtils — 调度器与固定速率/延迟任务
  - `src/main/java/com/trae/webtools/SchedulerUtils.java`

## Net / URL
- UrlUtils — 解析/构造、路径与查询参数、绝对/相对判断
  - `src/main/java/com/trae/webtools/UrlUtils.java`
- NetUtils — IPv4/CIDR 解析、网络/广播/主机数、子网判断
  - `src/main/java/com/trae/webtools/NetUtils.java`

## Models / Responses
- Page — 分页结果模型
  - `src/main/java/com/trae/webtools/Page.java`
- ApiResponse — API 响应模型
  - `src/main/java/com/trae/webtools/ApiResponse.java`

---
- 提示：可在 IDE 中搜索“Category”或直接展开包结构按类别浏览。
