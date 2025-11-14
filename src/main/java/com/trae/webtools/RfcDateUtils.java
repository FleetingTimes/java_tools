package com.trae.webtools;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * HTTP/RFC 日期工具：RFC1123/RFC850/asctime 格式化与解析
 *
 * 背景说明：
 * - HTTP 日期头常见三种表示：
 *   RFC1123（推荐）：例如 "Sun, 06 Nov 1994 08:49:37 GMT"
 *   RFC850（旧版）：例如 "Sunday, 06-Nov-94 08:49:37 GMT"
 *   asctime：例如 "Sun Nov  6 08:49:37 1994"（注意单数字日期可能留空格对齐）
 * - 时区：按照规范应使用 GMT（等价于 UTC）；本工具在格式化与解析时统一以 UTC 处理，避免本地时区差异。
 * - 语言环境：使用 Locale.US 以确保英文星期与月份缩写的稳定性。
 * - 线程安全：{@link java.time.format.DateTimeFormatter} 是线程安全的，静态复用无并发问题。
 *
 * 使用场景：
 * - 构造/解析 HTTP 头（如 Date、If-Modified-Since、Expires）的日期文本
 * - 在日志或报文中统一输出 RFC1123 标准时间，避免跨区域解析失败
 *
 * 注意：
 * - asctime 的 day 字段可能是单数字并出现对齐空格，解析时需严格匹配模式；本工具的 asctime 解析基于 "EEE MMM d HH:mm:ss yyyy"。
 * - 若客户端/代理使用非标准格式或时区，请在接入处进行兜底或统一转换为标准 RFC1123。
 */
public final class RfcDateUtils {
    private RfcDateUtils() {}

    private static final DateTimeFormatter RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US);
    private static final DateTimeFormatter RFC850 = DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss 'GMT'", Locale.US).withZone(ZoneId.of("UTC"));
    private static final DateTimeFormatter ASC_TIME = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy", Locale.US).withZone(ZoneId.of("UTC"));

    /**
     * 格式化为 RFC1123（GMT/UTC）
     * @param date Java Date（使用其瞬时时间）
     * @return RFC1123 文本（例如 "Sun, 06 Nov 1994 08:49:37 GMT"）
     */
    public static String formatRfc1123(Date date) { return RFC1123.format(ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"))); }

    /**
     * 解析 RFC1123 文本为 Date
     * @param text RFC1123 文本
     * @return Date（UTC 对应的瞬时时间）
     * @throws java.time.format.DateTimeParseException 当文本不符合 RFC1123 格式
     */
    public static Date parseRfc1123(String text) { ZonedDateTime z=ZonedDateTime.parse(text, RFC1123); return Date.from(z.toInstant()); }

    /**
     * 格式化为 RFC850（GMT/UTC）
     * @param date Java Date
     * @return RFC850 文本（例如 "Sunday, 06-Nov-94 08:49:37 GMT"）
     */
    public static String formatRfc850(Date date) { return RFC850.format(date.toInstant()); }

    /**
     * 解析 RFC850 文本为 Date
     * @param text RFC850 文本
     * @return Date（UTC 对应的瞬时时间）
     * @throws java.time.format.DateTimeParseException 当文本不符合 RFC850 格式
     */
    public static Date parseRfc850(String text) { ZonedDateTime z=ZonedDateTime.parse(text, RFC850); return Date.from(z.toInstant()); }

    /**
     * 格式化为 asctime（GMT/UTC）
     * @param date Java Date
     * @return asctime 文本（例如 "Sun Nov  6 08:49:37 1994"）
     */
    public static String formatAsctime(Date date) { return ASC_TIME.format(date.toInstant()); }

    /**
     * 解析 asctime 文本为 Date
     * @param text asctime 文本（模式 "EEE MMM d HH:mm:ss yyyy"）
     * @return Date（UTC 对应的瞬时时间）
     * @throws java.time.format.DateTimeParseException 当文本不符合 asctime 格式
     */
    public static Date parseAsctime(String text) { LocalDateTime l=LocalDateTime.parse(text, DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy", Locale.US)); return Date.from(l.atZone(ZoneId.of("UTC")).toInstant()); }

    /**
     * 将时间戳毫秒格式化为 RFC1123（GMT/UTC）
     * @param epochMillis 毫秒时间戳（UTC 基准）
     * @return RFC1123 文本
     */
    public static String formatRfc1123(long epochMillis) { return RFC1123.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("UTC"))); }

    /**
     * 解析 RFC1123 为毫秒时间戳（UTC）
     * @param text RFC1123 文本
     * @return 毫秒时间戳（UTC）
     */
    public static long parseRfc1123ToMillis(String text) { return parseRfc1123(text).getTime(); }
}
