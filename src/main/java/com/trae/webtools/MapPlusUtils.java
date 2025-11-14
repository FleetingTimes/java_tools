package com.trae.webtools;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Map 增强工具（不与现有方法重复）
 *
 * 提供过滤/转换、重命名键、保留/移除键、浅/深合并、默认值、排序视图、首尾条目、
 * 键值集合构造、键/值规则校验等。
 */
public final class MapPlusUtils {
    private MapPlusUtils() {}

    /** 按键过滤（保留满足条件的条目） */
    public static <K,V> Map<K,V> filterKeys(Map<K,V> m, Predicate<K> p) { Map<K,V> out=new LinkedHashMap<>(); if(m!=null) for(Map.Entry<K,V> e:m.entrySet()) if(p.test(e.getKey())) out.put(e.getKey(), e.getValue()); return out; }

    /** 按值过滤（保留满足条件的条目） */
    public static <K,V> Map<K,V> filterValues(Map<K,V> m, Predicate<V> p) { Map<K,V> out=new LinkedHashMap<>(); if(m!=null) for(Map.Entry<K,V> e:m.entrySet()) if(p.test(e.getValue())) out.put(e.getKey(), e.getValue()); return out; }

    /** 键转换（生成新键，保留原值；碰撞时后者覆盖） */
    public static <K,V,K2> Map<K2,V> mapKeys(Map<K,V> m, Function<K,K2> fn) { Map<K2,V> out=new LinkedHashMap<>(); if(m!=null) for(Map.Entry<K,V> e:m.entrySet()) out.put(fn.apply(e.getKey()), e.getValue()); return out; }

    /** 值转换（生成新值，保留原键） */
    public static <K,V,V2> Map<K,V2> mapValues(Map<K,V> m, Function<V,V2> fn) { Map<K,V2> out=new LinkedHashMap<>(); if(m!=null) for(Map.Entry<K,V> e:m.entrySet()) out.put(e.getKey(), fn.apply(e.getValue())); return out; }

    /** 重命名键（不存在则忽略） */
    public static <K,V> Map<K,V> renameKey(Map<K,V> m, K oldKey, K newKey) { Map<K,V> out=new LinkedHashMap<>(); if(m==null) return out; for(Map.Entry<K,V> e:m.entrySet()){ if(Objects.equals(e.getKey(),oldKey)) out.put(newKey,e.getValue()); else out.put(e.getKey(),e.getValue()); } return out; }

    /** 保留指定键集合 */
    public static <K,V> Map<K,V> keepKeys(Map<K,V> m, Collection<K> keys) { Map<K,V> out=new LinkedHashMap<>(); if(m==null||keys==null) return out; for(K k:keys) if(m.containsKey(k)) out.put(k, m.get(k)); return out; }

    /** 移除指定键集合 */
    public static <K,V> Map<K,V> removeKeys(Map<K,V> m, Collection<K> keys) { Map<K,V> out=new LinkedHashMap<>(); if(m==null) return out; Set<K> s=new LinkedHashSet<>(keys==null?Collections.emptySet():keys); for(Map.Entry<K,V> e:m.entrySet()) if(!s.contains(e.getKey())) out.put(e.getKey(),e.getValue()); return out; }

    /** 浅合并（后者覆盖前者） */
    public static <K,V> Map<K,V> shallowMerge(Map<K,V> a, Map<K,V> b) { Map<K,V> out=new LinkedHashMap<>(); if(a!=null) out.putAll(a); if(b!=null) out.putAll(b); return out; }

    /** 深合并（值为 Map 时递归合并，其他值后者覆盖前者） */
    @SuppressWarnings("unchecked")
    public static Map<String,Object> deepMerge(Map<String,Object> a, Map<String,Object> b) {
        Map<String,Object> out=new LinkedHashMap<>(); if(a!=null) out.putAll(a);
        if(b!=null) for(Map.Entry<String,Object> e:b.entrySet()){
            String k=e.getKey(); Object v=e.getValue(); Object o=out.get(k);
            if(o instanceof Map && v instanceof Map) out.put(k, deepMerge((Map<String,Object>)o, (Map<String,Object>)v)); else out.put(k,v);
        }
        return out;
    }

    /** 为缺失键设置默认值（不覆盖已存在键） */
    public static <K,V> Map<K,V> withDefaults(Map<K,V> m, Map<K,V> defaults) { Map<K,V> out=new LinkedHashMap<>(); if(m!=null) out.putAll(m); if(defaults!=null) for(Map.Entry<K,V> e:defaults.entrySet()) out.putIfAbsent(e.getKey(), e.getValue()); return out; }

    /** 按键排序视图（新 LinkedHashMap） */
    public static <K,V> Map<K,V> sortedByKeys(Map<K,V> m, Comparator<K> cmp) { if(m==null) return Collections.emptyMap(); List<K> keys=new ArrayList<>(m.keySet()); keys.sort(cmp); Map<K,V> out=new LinkedHashMap<>(); for(K k:keys) out.put(k, m.get(k)); return out; }

    /** 按值排序视图（新 LinkedHashMap） */
    public static <K,V> Map<K,V> sortedByValues(Map<K,V> m, Comparator<V> cmp) { if(m==null) return Collections.emptyMap(); List<Map.Entry<K,V>> es=new ArrayList<>(m.entrySet()); es.sort((x,y)->cmp.compare(x.getValue(), y.getValue())); Map<K,V> out=new LinkedHashMap<>(); for(Map.Entry<K,V> e:es) out.put(e.getKey(), e.getValue()); return out; }

    /** 获取首条目（若为空返回 null） */
    public static <K,V> Map.Entry<K,V> firstEntry(Map<K,V> m) { if(m==null||m.isEmpty()) return null; for(Map.Entry<K,V> e:m.entrySet()) return e; return null; }

    /** 获取尾条目（若为空返回 null） */
    public static <K,V> Map.Entry<K,V> lastEntry(Map<K,V> m) { if(m==null||m.isEmpty()) return null; Map.Entry<K,V> last=null; for(Map.Entry<K,V> e:m.entrySet()) last=e; return last; }

    /** 将键集合与值集合构造成 Map（长度不一致按较短的） */
    public static <K,V> Map<K,V> ofLists(List<K> keys, List<V> values) { Map<K,V> out=new LinkedHashMap<>(); if(keys==null||values==null) return out; int n=Math.min(keys.size(), values.size()); for(int i=0;i<n;i++) out.put(keys.get(i), values.get(i)); return out; }

    /** 键是否全部满足前缀 */
    public static boolean keysStartsWith(Map<String,?> m, String prefix) { if(m==null||prefix==null) return false; for(String k:m.keySet()) if(!k.startsWith(prefix)) return false; return true; }

    /** 键是否全部满足后缀 */
    public static boolean keysEndsWith(Map<String,?> m, String suffix) { if(m==null||suffix==null) return false; for(String k:m.keySet()) if(!k.endsWith(suffix)) return false; return true; }

    /** 值转换为字符串（null 转为空串） */
    public static <K> Map<K,String> valuesToStrings(Map<K,?> m) { Map<K,String> out=new LinkedHashMap<>(); if(m!=null) for(Map.Entry<K,?> e:m.entrySet()) out.put(e.getKey(), String.valueOf(e.getValue()==null?"":e.getValue())); return out; }

    /** 反转为多值 Map（值 -> 键列表） */
    public static <K,V> Map<V,List<K>> invertMulti(Map<K,V> m) { Map<V,List<K>> out=new LinkedHashMap<>(); if(m!=null) for(Map.Entry<K,V> e:m.entrySet()) out.computeIfAbsent(e.getValue(), v->new ArrayList<>()).add(e.getKey()); return out; }

    /** 校验并要求键全部存在（若缺失抛异常） */
    public static void requireAllKeys(Map<String,?> m, Collection<String> keys) { if(m==null||keys==null) throw new IllegalArgumentException("null map/keys"); for(String k:keys) if(!m.containsKey(k)) throw new IllegalArgumentException("missing key: "+k); }

    /** 校验键不为空字符串（trim 后） */
    public static boolean keysNonEmpty(Map<String,?> m) { if(m==null) return false; for(String k:m.keySet()) if(StringUtils.isBlank(k)) return false; return true; }

    /** 将条目列表转为 Map（碰撞时后者覆盖前者） */
    public static <K,V> Map<K,V> fromEntries(List<Map.Entry<K,V>> entries) { Map<K,V> out=new LinkedHashMap<>(); if(entries!=null) for(Map.Entry<K,V> e:entries) out.put(e.getKey(), e.getValue()); return out; }
}

