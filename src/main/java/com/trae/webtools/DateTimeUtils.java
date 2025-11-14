package com.trae.webtools;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * 时间与日期工具
 */
public final class DateTimeUtils {
    private DateTimeUtils() {}

    /** 当前毫秒时间戳 */
    public static long nowMillis() { return System.currentTimeMillis(); }

    /** 当前时间的 ISO-8601 文本（系统默认时区） */
    public static String nowIso() { return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()); }

    /** 按模板格式化日期 */
    public static String formatDate(Date date, String pattern) {
        java.util.Objects.requireNonNull(date, "date");
        java.util.Objects.requireNonNull(pattern, "pattern");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
        return fmt.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault()));
    }

    /** 按模板解析文本到日期 */
    public static Date parseDate(String text, String pattern) {
        java.util.Objects.requireNonNull(text, "text");
        java.util.Objects.requireNonNull(pattern, "pattern");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime ldt = LocalDateTime.parse(text, fmt);
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    /** 日期增加分钟数 */
    public static Date dateAddMinutes(Date date, int minutes) { return new Date(date.getTime() + minutes * 60_000L); }

    /** 日期增加天数 */
    public static Date dateAddDays(Date date, int days) { return new Date(date.getTime() + days * 24L * 60L * 60L * 1000L); }

    /** 人性化时长（如 "1h 3m 5s"） */
    public static String durationHumanize(Duration d) {
        long sec = d.getSeconds();
        long h = sec / 3600; sec %= 3600;
        long m = sec / 60; sec %= 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        sb.append(sec).append("s");
        return sb.toString().trim();
    }

    /** 人性化字节大小（如 "1.5 MB"） */
    public static String sizeHumanize(long bytes) {
        String[] units = {"B","KB","MB","GB","TB"};
        double v = bytes; int i = 0;
        while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
        return String.format(Locale.ROOT, "%.1f %s", v, units[i]);
    }

    /** 当天开始时间（本地时区） */
    public static Date startOfDay(Date date) {
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
        LocalDateTime s = ldt.toLocalDate().atStartOfDay();
        return Date.from(s.atZone(ZoneId.systemDefault()).toInstant());
    }

    /** 当天结束时间（本地时区，23:59:59.999） */
    public static Date endOfDay(Date date) {
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
        LocalDateTime e = ldt.toLocalDate().atTime(23,59,59,999_000_000);
        return Date.from(e.atZone(ZoneId.systemDefault()).toInstant());
    }

    /** 转为 epoch 秒 */
    public static long toEpochSecond(Date date) { return date.getTime() / 1000L; }

    /** ISO-8601 Instant 格式化（UTC） */
    public static String formatIsoInstant(Date date) { return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(date.getTime())); }

    /** 解析 ISO-8601 LocalDateTime 文本（本地时区） */
    public static Date parseIsoLocal(String text) { LocalDateTime ldt = LocalDateTime.parse(text); return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()); }

    /** 解析 ISO-8601 Instant 文本（UTC） */
    public static Date parseIsoInstant(String text) { Instant ins = Instant.parse(text); return Date.from(ins); }

    /** 判断是否同一天（本地时区） */
    public static boolean isSameDay(Date a, Date b) {
        LocalDateTime la = LocalDateTime.ofInstant(Instant.ofEpochMilli(a.getTime()), ZoneId.systemDefault());
        LocalDateTime lb = LocalDateTime.ofInstant(Instant.ofEpochMilli(b.getTime()), ZoneId.systemDefault());
        return la.toLocalDate().equals(lb.toLocalDate());
    }

    /** 日期增加小时数 */
    public static Date dateAddHours(Date date, int hours) { return new Date(date.getTime() + hours * 3_600_000L); }

    /** 日期增加秒数 */
    public static Date dateAddSeconds(Date date, int seconds) { return new Date(date.getTime() + seconds * 1_000L); }

    /** 计算两个时间的间隔 Duration */
    public static Duration between(Date start, Date end) { return Duration.ofMillis(end.getTime() - start.getTime()); }
}
