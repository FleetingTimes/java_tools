package com.trae.webtools;

import java.util.*;
import java.util.function.Supplier;

/**
 * Map 工具：合并、翻转、拷贝、子集、键大小写、排序、必需键校验
 */
public final class MapUtils {
    private MapUtils() {}

    /** 合并两个 Map（后者覆盖前者），返回新 LinkedHashMap */
    public static <K,V> Map<K,V> merge(Map<K,V> a, Map<K,V> b) {
        Map<K,V> out = new LinkedHashMap<>();
        if (a != null) out.putAll(a);
        if (b != null) out.putAll(b);
        return out;
    }

    /** 键翻转（V->K），碰撞时以后者覆盖前者 */
    public static <K,V> Map<V,K> invert(Map<K,V> m) {
        Map<V,K> out = new LinkedHashMap<>();
        if (m != null) for (Map.Entry<K,V> e : m.entrySet()) out.put(e.getValue(), e.getKey());
        return out;
    }

    /** 浅拷贝为新 LinkedHashMap */
    public static <K,V> Map<K,V> copy(Map<K,V> m) { Map<K,V> out = new LinkedHashMap<>(); if (m!=null) out.putAll(m); return out; }

    /** 提取子集键为新 LinkedHashMap（忽略不存在键） */
    public static <K,V> Map<K,V> subMap(Map<K,V> m, Collection<K> keys) {
        Map<K,V> out = new LinkedHashMap<>();
        if (m==null || keys==null) return out;
        for (K k : keys) if (m.containsKey(k)) out.put(k, m.get(k));
        return out;
    }

    /** 若键不存在则根据提供者放入并返回值 */
    public static <K,V> V putIfAbsent(Map<K,V> m, K key, Supplier<V> supplier) {
        V v = m.get(key);
        if (v == null) { v = supplier.get(); m.put(key, v); }
        return v;
    }

    /** 键转小写（新 LinkedHashMap） */
    public static <V> Map<String,V> keysToLowerCase(Map<String,V> m) {
        Map<String,V> out = new LinkedHashMap<>();
        if (m!=null) for (Map.Entry<String,V> e : m.entrySet()) out.put(e.getKey()==null?null:e.getKey().toLowerCase(Locale.ROOT), e.getValue());
        return out;
    }

    /** 键转大写（新 LinkedHashMap） */
    public static <V> Map<String,V> keysToUpperCase(Map<String,V> m) {
        Map<String,V> out = new LinkedHashMap<>();
        if (m!=null) for (Map.Entry<String,V> e : m.entrySet()) out.put(e.getKey()==null?null:e.getKey().toUpperCase(Locale.ROOT), e.getValue());
        return out;
    }

    /** 按键排序为新 LinkedHashMap */
    public static <K,V> Map<K,V> sortByKey(Map<K,V> m, Comparator<K> cmp) {
        if (m == null) return Collections.emptyMap();
        List<K> keys = new ArrayList<>(m.keySet());
        keys.sort(cmp);
        Map<K,V> out = new LinkedHashMap<>();
        for (K k : keys) out.put(k, m.get(k));
        return out;
    }

    /** 校验必需键是否全部存在，不存在则抛 IllegalArgumentException */
    public static void requireKeys(Map<String,?> m, String... keys) {
        for (String k : keys) if (m == null || !m.containsKey(k)) throw new IllegalArgumentException("missing key: " + k);
    }

    /** 判断是否包含所有指定键 */
    public static boolean hasAllKeys(Map<String,?> m, java.util.Collection<String> keys) {
        if (m == null || keys == null) return false;
        for (String k : keys) if (!m.containsKey(k)) return false;
        return true;
    }

    /** 快速构造 String->Object Map（kv 交替传入） */
    public static Map<String,Object> of(Object... kv) {
        Map<String,Object> m = new LinkedHashMap<>();
        for (int i = 0; i+1 < kv.length; i+=2) m.put(String.valueOf(kv[i]), kv[i+1]);
        return m;
    }
}
