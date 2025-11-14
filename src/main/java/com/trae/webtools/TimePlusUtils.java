package com.trae.webtools;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * 时间增强工具（不与现有方法重复）
 *
 * 提供时长解析与格式化、按分钟/小时取整、周起止、工作日/周末判断与计算、
 * 增加周/月、跨时区转换辅助等。
 */
public final class TimePlusUtils {
    private TimePlusUtils() {}

    /** 将 "1h30m20s" 等文本解析为 Duration（支持 h/m/s/ms） */
    public static Duration parseDurationCompact(String text) {
        if (StringUtils.isBlank(text)) return Duration.ZERO; String t=text.trim().toLowerCase(Locale.ROOT);
        long totalMs=0; java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)(ms|s|m|h)").matcher(t);
        while(m.find()){
            long v=Long.parseLong(m.group(1)); String u=m.group(2);
            if("ms".equals(u)) totalMs+=v; else if("s".equals(u)) totalMs+=v*1000; else if("m".equals(u)) totalMs+=v*60_000; else if("h".equals(u)) totalMs+=v*3_600_000;
        }
        return Duration.ofMillis(totalMs);
    }

    /** 将时长格式化为紧凑文本（如 1h30m20s） */
    public static String formatDurationCompact(Duration d) {
        long ms=d.toMillis(); long h=ms/3_600_000; ms%=3_600_000; long m=ms/60_000; ms%=60_000; long s=ms/1000; ms%=1000;
        StringBuilder sb=new StringBuilder(); if(h>0) sb.append(h).append("h"); if(m>0) sb.append(m).append("m"); if(s>0) sb.append(s).append("s"); if(ms>0) sb.append(ms).append("ms"); return sb.length()==0?"0ms":sb.toString();
    }

    /** 向下取整到分钟 */
    public static Date floorToMinute(Date date) { long t=date.getTime(); return new Date(t - (t%60_000)); }

    /** 向上取整到分钟（若已对齐则不变） */
    public static Date ceilToMinute(Date date) { long t=date.getTime(); return new Date(((t+59_999)/60_000)*60_000); }

    /** 向下取整到小时 */
    public static Date floorToHour(Date date) { long t=date.getTime(); return new Date(t - (t%3_600_000)); }

    /** 向上取整到小时（若已对齐则不变） */
    public static Date ceilToHour(Date date) { long t=date.getTime(); return new Date(((t+3_599_999)/3_600_000)*3_600_000); }

    /** 判断是否周末（本地时区） */
    public static boolean isWeekend(Date date) { LocalDate ld=Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate(); DayOfWeek w=ld.getDayOfWeek(); return w==DayOfWeek.SATURDAY||w==DayOfWeek.SUNDAY; }

    /** 下一个工作日（本地时区） */
    public static Date nextWorkday(Date date) { LocalDate ld=Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1); while(ld.getDayOfWeek()==DayOfWeek.SATURDAY||ld.getDayOfWeek()==DayOfWeek.SUNDAY) ld=ld.plusDays(1); return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant()); }

    /** 周起始（周一 00:00，本地时区） */
    public static Date startOfWeek(Date date) { LocalDate ld=Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate(); LocalDate monday=ld.minusDays((ld.getDayOfWeek().getValue()+6)%7); return Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant()); }

    /** 周结束（周日 23:59:59.999，本地时区） */
    public static Date endOfWeek(Date date) { LocalDate ld=Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate(); LocalDate sunday=ld.plusDays(7-((ld.getDayOfWeek().getValue()+6)%7)-1); LocalDateTime end=sunday.atTime(23,59,59,999_000_000); return Date.from(end.atZone(ZoneId.systemDefault()).toInstant()); }

    /** 增加周数 */
    public static Date addWeeks(Date date, int weeks) { return Date.from(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate().plusWeeks(weeks).atStartOfDay(ZoneId.systemDefault()).toInstant()); }

    /** 增加月数（保留日，不足按月末裁剪） */
    public static Date addMonths(Date date, int months) { LocalDate ld=Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate(); LocalDate t=ld.plusMonths(months); return Date.from(t.atStartOfDay(ZoneId.systemDefault()).toInstant()); }

    /** 在指定时区格式化时间（如 "yyyy-MM-dd HH:mm:ss"） */
    public static String formatInZone(Date date, String pattern, String zoneId) { DateTimeFormatter fmt=DateTimeFormatter.ofPattern(pattern); return fmt.format(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.of(zoneId)).toLocalDateTime()); }

    /** 将文本按指定时区解析为 Date */
    public static Date parseInZone(String text, String pattern, String zoneId) { DateTimeFormatter fmt=DateTimeFormatter.ofPattern(pattern); LocalDateTime ldt=LocalDateTime.parse(text, fmt); return Date.from(ldt.atZone(ZoneId.of(zoneId)).toInstant()); }
}

