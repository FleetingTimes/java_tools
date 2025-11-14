package com.trae.webtools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量 JSON Map 工具（不与现有 JsonPropsUtils 重复）
 *
 * 说明：面向平面 Map<String,Object> 的简单读写与合并，避免复杂解析；
 * 提供类型安全获取、缺省值、路径键（a.b.c）读写、浅/深拷贝与合并、
 * 选择/排除键、空值清理、数组/字符串互转等。
 */
public final class JsonLiteUtils {
    private JsonLiteUtils() {}

    /** 类型安全获取字符串（若为非字符串则调用 toString） */
    public static String getString(Map<String,Object> m, String key, String def) { Object v=m==null?null:m.get(key); return v==null?def:String.valueOf(v); }

    /** 类型安全获取 int（不可解析返回默认值） */
    public static int getInt(Map<String,Object> m, String key, int def) { Object v=m==null?null:m.get(key); if(v==null) return def; try{ return v instanceof Number?((Number)v).intValue():Integer.parseInt(String.valueOf(v)); }catch(Exception e){ return def; } }

    /** 类型安全获取 long（不可解析返回默认值） */
    public static long getLong(Map<String,Object> m, String key, long def) { Object v=m==null?null:m.get(key); if(v==null) return def; try{ return v instanceof Number?((Number)v).longValue():Long.parseLong(String.valueOf(v)); }catch(Exception e){ return def; } }

    /** 类型安全获取 double（不可解析返回默认值） */
    public static double getDouble(Map<String,Object> m, String key, double def) { Object v=m==null?null:m.get(key); if(v==null) return def; try{ return v instanceof Number?((Number)v).doubleValue():Double.parseDouble(String.valueOf(v)); }catch(Exception e){ return def; } }

    /** 类型安全获取 boolean（支持 true/1/yes/y） */
    public static boolean getBoolean(Map<String,Object> m, String key, boolean def) { Object v=m==null?null:m.get(key); if(v==null) return def; String s=String.valueOf(v).trim().toLowerCase(java.util.Locale.ROOT); return s.equals("true")||s.equals("1")||s.equals("yes")||s.equals("y"); }

    /** 放置缺失键的默认值（不覆盖已存在键） */
    public static Map<String,Object> withDefaults(Map<String,Object> m, Map<String,Object> defaults) { Map<String,Object> out=copyShallow(m); if(defaults!=null) for(Map.Entry<String,Object> e:defaults.entrySet()) out.putIfAbsent(e.getKey(), e.getValue()); return out; }

    /** 浅拷贝 */
    public static Map<String,Object> copyShallow(Map<String,Object> m) { Map<String,Object> out=new LinkedHashMap<>(); if(m!=null) out.putAll(m); return out; }

    /** 深拷贝（Map 与 List 递归，其它值直接引用） */
    @SuppressWarnings("unchecked")
    public static Object deepCopy(Object v) {
        if (v instanceof Map) { Map<String,Object> out=new LinkedHashMap<>(); Map<String,Object> src=(Map<String,Object>)v; for(Map.Entry<String,Object> e:src.entrySet()) out.put(e.getKey(), deepCopy(e.getValue())); return out; }
        if (v instanceof List) { List<Object> out=new ArrayList<>(); for(Object it:(List<Object>)v) out.add(deepCopy(it)); return out; }
        return v;
    }

    /** 深合并（目标优先，Map/List 递归合并） */
    @SuppressWarnings("unchecked")
    public static Map<String,Object> deepMerge(Map<String,Object> base, Map<String,Object> override) {
        Map<String,Object> out=copyShallow(base); if(override==null) return out;
        for(Map.Entry<String,Object> e:override.entrySet()){
            String k=e.getKey(); Object ov=e.getValue(); Object bv=out.get(k);
            if(bv instanceof Map && ov instanceof Map) out.put(k, deepMerge((Map<String,Object>)bv, (Map<String,Object>)ov));
            else if(bv instanceof List && ov instanceof List) out.put(k, mergeList((List<Object>)bv, (List<Object>)ov));
            else out.put(k, deepCopy(ov));
        }
        return out;
    }

    /** 列表合并（追加非重复元素，按文本 equals 判断） */
    private static List<Object> mergeList(List<Object> a, List<Object> b) { List<Object> out=new ArrayList<>(a==null?0:a.size()); if(a!=null) out.addAll(a); if(b!=null) for(Object x:b){ boolean has=false; for(Object y:out) if(String.valueOf(y).equals(String.valueOf(x))) { has=true; break; } if(!has) out.add(x); } return out; }

    /** 路径键读取（如 a.b.c） */
    @SuppressWarnings("unchecked")
    public static Object pathGet(Map<String,Object> m, String path) { if(m==null||StringUtils.isBlank(path)) return null; String[] ps=path.split("\\."); Object cur=m; for(String p:ps){ if(!(cur instanceof Map)) return null; cur=((Map<String,Object>)cur).get(p); if(cur==null) return null; } return cur; }

    /** 路径键写入（不存在时创建中间 Map） */
    @SuppressWarnings("unchecked")
    public static void pathPut(Map<String,Object> m, String path, Object value) { String[] ps=path.split("\\."); Map<String,Object> cur=m; for(int i=0;i<ps.length-1;i++){ String p=ps[i]; Object next=cur.get(p); if(!(next instanceof Map)){ next=new LinkedHashMap<>(); cur.put(p,next);} cur=(Map<String,Object>)next; } cur.put(ps[ps.length-1], value); }

    /** 选择指定键（忽略不存在） */
    public static Map<String,Object> pick(Map<String,Object> m, List<String> keys) { Map<String,Object> out=new LinkedHashMap<>(); if(m!=null&&keys!=null) for(String k:keys) if(m.containsKey(k)) out.put(k, m.get(k)); return out; }

    /** 排除指定键 */
    public static Map<String,Object> omit(Map<String,Object> m, List<String> keys) { Map<String,Object> out=new LinkedHashMap<>(); if(m==null) return out; java.util.Set<String> s=new java.util.LinkedHashSet<>(keys==null?java.util.Collections.emptySet():new java.util.LinkedHashSet<>(keys)); for(Map.Entry<String,Object> e:m.entrySet()) if(!s.contains(e.getKey())) out.put(e.getKey(), e.getValue()); return out; }

    /** 移除值为 null 或空字符串的键 */
    public static Map<String,Object> removeEmpty(Map<String,Object> m) { Map<String,Object> out=new LinkedHashMap<>(); if(m!=null) for(Map.Entry<String,Object> e:m.entrySet()){ Object v=e.getValue(); if(v==null) continue; String s=String.valueOf(v); if(StringUtils.isBlank(s)) continue; out.put(e.getKey(), v); } return out; }

    /** 字符串转列表（按逗号分隔，忽略空项） */
    public static List<String> commaStringToList(String s) { List<String> out=new ArrayList<>(); if(StringUtils.isBlank(s)) return out; for(String it:s.split(",")) if(!StringUtils.isBlank(it)) out.add(it.trim()); return out; }

    /** 列表转逗号字符串 */
    public static String listToCommaString(List<?> list) { if(list==null||list.isEmpty()) return ""; StringBuilder sb=new StringBuilder(); boolean first=true; for(Object o:list){ if(!first) sb.append(','); sb.append(String.valueOf(o)); first=false; } return sb.toString(); }

    /** 扁平化嵌套 Map（路径键） */
    @SuppressWarnings("unchecked")
    public static Map<String,Object> flatten(Map<String,Object> m) { Map<String,Object> out=new LinkedHashMap<>(); if(m!=null) flattenInto(out, "", m); return out; }

    @SuppressWarnings("unchecked")
    private static void flattenInto(Map<String,Object> out, String prefix, Object v) {
        if (v instanceof Map) { Map<String,Object> mm=(Map<String,Object>)v; for(Map.Entry<String,Object> e:mm.entrySet()) flattenInto(out, joinPath(prefix,e.getKey()), e.getValue()); }
        else out.put(prefix, v);
    }

    private static String joinPath(String a, String b) { if(StringUtils.isBlank(a)) return b; return a+"."+b; }

    /** 反扁平化（路径键还原嵌套 Map） */
    public static Map<String,Object> unflatten(Map<String,Object> flat) { Map<String,Object> out=new LinkedHashMap<>(); if(flat!=null) for(Map.Entry<String,Object> e:flat.entrySet()) pathPut(out, e.getKey(), e.getValue()); return out; }
}

