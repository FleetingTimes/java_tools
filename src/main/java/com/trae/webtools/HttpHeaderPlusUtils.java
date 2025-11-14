package com.trae.webtools;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP 头增强工具：规范化、合并、增删改查
 *
 * 规范化策略：首字母大写，其他连字符分隔的部分同样首字母大写（例如 content-type -> Content-Type）。
 * 增删改查均采用不区分大小写匹配，避免大小写差异导致的重复或漏查。
 */
public final class HttpHeaderPlusUtils {
    private HttpHeaderPlusUtils() {}

    /** 规范化头名称（如 content-type -> Content-Type） */
    public static String normalizeName(String name) { return HttpUtils.normalizeHeaderName(name); }

    /** 规范化并合并两个头集合（后者覆盖前者） */
    public static Map<String,String> merge(Map<String,String> a, Map<String,String> b) {
        Map<String,String> out=new LinkedHashMap<>(); if(a!=null) for(Map.Entry<String,String> e:a.entrySet()) out.put(normalizeName(e.getKey()), e.getValue()); if(b!=null) for(Map.Entry<String,String> e:b.entrySet()) out.put(normalizeName(e.getKey()), e.getValue()); return out;
    }

    /** 设置（规范化名称） */
    public static void set(Map<String,String> headers, String name, String value) { if(headers==null) return; headers.put(normalizeName(name), value); }

    /** 获取（忽略大小写） */
    public static String get(Map<String,String> headers, String name) { if(headers==null) return null; String n=normalizeName(name); for(Map.Entry<String,String> e:headers.entrySet()) if(e.getKey().equalsIgnoreCase(n)) return e.getValue(); return null; }

    /** 删除（忽略大小写） */
    public static void remove(Map<String,String> headers, String name) { if(headers==null) return; String n=normalizeName(name); headers.keySet().removeIf(k->k.equalsIgnoreCase(n)); }
}
