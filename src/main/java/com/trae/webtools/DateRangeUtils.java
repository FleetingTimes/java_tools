package com.trae.webtools;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * 日期区间工具：基于起止时间点（毫秒）进行区间计算
 */
public final class DateRangeUtils {
    private DateRangeUtils() {}

    /** 区间模型 */
    public static final class Range { public final long start; public final long end; public Range(long start, long end){ if(start>end) throw new IllegalArgumentException("start>end"); this.start=start; this.end=end; } }

    /** 创建区间（Date） */
    public static Range of(Date start, Date end) { return new Range(start.getTime(), end.getTime()); }

    /** 创建区间（毫秒） */
    public static Range of(long startMillis, long endMillis) { return new Range(startMillis, endMillis); }

    /** 区间长度（毫秒） */
    public static long lengthMillis(Range r) { return r.end - r.start; }

    /** 区间是否包含某一时间点（毫秒，闭区间） */
    public static boolean contains(Range r, long millis) { return millis>=r.start && millis<=r.end; }

    /** 区间是否包含另一个区间（闭区间） */
    public static boolean containsRange(Range a, Range b) { return b.start>=a.start && b.end<=a.end; }

    /** 区间是否重叠（有交集） */
    public static boolean overlaps(Range a, Range b) { return a.start<=b.end && b.start<=a.end; }

    /** 交集（无交集返回 null） */
    public static Range intersection(Range a, Range b) { if(!overlaps(a,b)) return null; return new Range(Math.max(a.start,b.start), Math.min(a.end,b.end)); }

    /** 将区间按天界拆分（本地时区），返回多个子区间 */
    public static java.util.List<Range> splitByDay(Range r) {
        java.util.List<Range> out=new java.util.ArrayList<>(); ZoneId zone=ZoneId.systemDefault();
        long curStart=r.start; while(curStart<=r.end){ ZonedDateTime z=ZonedDateTime.ofInstant(Instant.ofEpochMilli(curStart), zone); ZonedDateTime endOfDay=z.toLocalDate().atTime(23,59,59,999_000_000).atZone(zone); long partEnd=Math.min(r.end, endOfDay.toInstant().toEpochMilli()); out.add(new Range(curStart, partEnd)); curStart=partEnd+1; }
        return out;
    }

    /** 将区间向未来偏移指定毫秒数 */
    public static Range shiftForward(Range r, long millis) { return new Range(r.start+millis, r.end+millis); }

    /** 将区间向过去偏移指定毫秒数 */
    public static Range shiftBackward(Range r, long millis) { return new Range(r.start-millis, r.end-millis); }

    /** 将区间钳制到边界之内（若超出则裁剪） */
    public static Range clamp(Range r, Range bounds) { long s=Math.max(r.start, bounds.start); long e=Math.min(r.end, bounds.end); if(s>e) s=e; return new Range(s,e); }

    /** 按固定块大小切分区间（最后一块可能更短） */
    public static java.util.List<Range> chunk(Range r, long blockMillis) { java.util.List<Range> out=new java.util.ArrayList<>(); if(blockMillis<=0) return out; long s=r.start; while(s<=r.end){ long e=Math.min(r.end, s+blockMillis-1); out.add(new Range(s,e)); s=e+1; } return out; }

    /** 判断区间是否为空（长度为 0） */
    public static boolean isEmpty(Range r) { return r.end==r.start; }

    /** 转为人类可读时长 */
    public static String humanizeLength(Range r) { return DateTimeUtils.durationHumanize(Duration.ofMillis(lengthMillis(r))); }
}

