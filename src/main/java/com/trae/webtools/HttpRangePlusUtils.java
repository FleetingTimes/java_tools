package com.trae.webtools;

/**
 * HTTP Range/Content-Range 工具：构造与校验
 *
 * 支持单段字节范围（bytes），提供 Content-Range 构造、可满足性判断与范围规范化。
 * 注意：此工具不解析复杂多段 Range 请求，适用于文件下载与媒体流的基础场景。
 */
public final class HttpRangePlusUtils {
    private HttpRangePlusUtils() {}

    /** 构造 Content-Range（bytes start-end/length；未知长度用 *） */
    public static String buildContentRange(long start, long end, Long totalLength) {
        String len = totalLength==null?"*":String.valueOf(Math.max(0,totalLength));
        return "bytes " + Math.max(0,start) + "-" + Math.max(start,end) + "/" + len;
    }

    /** 校验请求 Range 是否可满足（单段；返回是否有效且在长度范围内） */
    public static boolean isSatisfiable(long start, Long end, long totalLength) {
        if (totalLength < 0) return false; if (start < 0) return false;
        long e = (end==null) ? (totalLength-1) : end;
        if (e < start) return false; return start < totalLength;
    }

    /** 规范化 Range（将 null 结尾替换为 totalLength-1，裁剪到 [0,total) ） */
    public static long[] normalize(long start, Long end, long totalLength) {
        long e = (end==null) ? (totalLength-1) : end; if(e<start) e=start; start=Math.max(0,start); e=Math.min(e,totalLength-1); return new long[]{start,e};
    }
}
