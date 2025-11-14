package com.trae.webtools;

/**
 * 后端工具聚合入口（已拆分）
 *
 * 说明：为更好的可维护性与按功能查找，本工具集已按领域拆分为独立类：
 * - StringUtils：字符串处理与转义
 * - ConvertUtils：字符串到数值/布尔安全解析
 * - IdUtils：随机ID与随机数生成
 * - DateTimeUtils：时间戳、日期格式化/解析、人性化时长
 * - SecurityUtils：摘要、HMAC、Base64/Base64URL、JWT/CSRF
 * - IOUtils：文件与流、路径、压缩/解压
 * - WebUtils：分页、API响应、校验、脱敏、ETag、MIME/CORS/IP
 * - HttpUtils：查询参数、Cookie 构造与解析
 * - JsonPropsUtils：极简 JSON 与 Properties 相互转换
 * - ConcurrencyUtils：睡眠、重试、超时执行、线程池、秒表
 * - CacheUtils：令牌桶限流与 LRU 缓存
 * - 类型：Stopwatch、Page、ApiResponse、RateLimiter、LruCache
 *
 * 使用方式：请直接引用以上类中的静态方法与类型。
 */
@Deprecated
public final class BackendTools {
    private BackendTools() {}
}
