package com.trae.webtools;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP Cache-Control 工具：构造与解析
 */
public final class HttpCacheUtils {
    private HttpCacheUtils() {}

    /** 构造 Cache-Control（如 public, max-age=3600, must-revalidate） */
    public static String build(boolean isPublic, Integer maxAge, boolean mustRevalidate, boolean noCache, boolean noStore) {
        StringBuilder sb=new StringBuilder(); sb.append(isPublic?"public":"private");
        if(maxAge!=null) sb.append(", max-age=").append(Math.max(0,maxAge));
        if(mustRevalidate) sb.append(", must-revalidate");
        if(noCache) sb.append(", no-cache");
        if(noStore) sb.append(", no-store");
        return sb.toString();
    }

    /** 解析 Cache-Control 为键值（指令 -> 值或空串） */
    public static Map<String,String> parse(String header) {
        Map<String,String> m=new LinkedHashMap<>(); if(StringUtils.isBlank(header)) return m;
        String[] parts=header.split(",");
        for(String p:parts){ String t=p.trim().toLowerCase(Locale.ROOT); int idx=t.indexOf('='); if(idx<0) m.put(t, ""); else m.put(t.substring(0,idx), t.substring(idx+1)); }
        return m;
    }

    /** 获取 max-age（不存在返回 -1） */
    public static int getMaxAge(Map<String,String> cc) { String v=cc.get("max-age"); return v==null?-1:ConvertUtils.safeParseInt(v,-1); }

    /** 是否 no-cache */
    public static boolean isNoCache(Map<String,String> cc) { return cc.containsKey("no-cache"); }

    /** 是否 no-store */
    public static boolean isNoStore(Map<String,String> cc) { return cc.containsKey("no-store"); }

    /** 是否 must-revalidate */
    public static boolean isMustRevalidate(Map<String,String> cc) { return cc.containsKey("must-revalidate"); }
}

