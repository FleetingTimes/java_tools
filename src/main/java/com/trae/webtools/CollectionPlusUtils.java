package com.trae.webtools;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 集合增强工具（不与现有方法重复）
 *
 * 提供分块、滑窗、交并差、对称差、拉链、展开、频次统计、TopK、截取与丢弃、
 * 旋转与洗牌拷贝、窗口聚合、交错合并、唯一化规则、拉链反解、笛卡尔积等。
 */
public final class CollectionPlusUtils {
    private CollectionPlusUtils() {}

    /** 将列表按固定块大小分割（最后一块可能更短） */
    public static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> out = new ArrayList<>(); if (list==null||size<=0) return out;
        for (int i = 0; i < list.size(); i += size) out.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i+size))));
        return out;
    }

    /** 生成滑动窗口视图（步长为 1） */
    public static <T> List<List<T>> slidingWindow(List<T> list, int window) {
        List<List<T>> out = new ArrayList<>(); if (list==null||window<=0) return out;
        for (int i = 0; i + window <= list.size(); i++) out.add(new ArrayList<>(list.subList(i, i+window)));
        return out;
    }

    /** 集合交集（保持 a 的顺序，去重） */
    public static <T> List<T> intersection(List<T> a, List<T> b) {
        if (a==null||b==null) return Collections.emptyList(); Set<T> sb=new LinkedHashSet<>(b); List<T> out=new ArrayList<>(); for(T x:a) if(sb.contains(x)&&!out.contains(x)) out.add(x); return out;
    }

    /** 集合并集（保持 a 的顺序，再追加 b 的非重复元素） */
    public static <T> List<T> union(List<T> a, List<T> b) {
        List<T> out = new ArrayList<>(); if(a!=null) for(T x:a) if(!out.contains(x)) out.add(x); if(b!=null) for(T x:b) if(!out.contains(x)) out.add(x); return out;
    }

    /** 集合差集 a\b（保持 a 的顺序） */
    public static <T> List<T> difference(List<T> a, List<T> b) {
        if (a==null) return Collections.emptyList(); Set<T> sb=new LinkedHashSet<>(b==null?Collections.emptyList():b); List<T> out=new ArrayList<>(); for(T x:a) if(!sb.contains(x)) out.add(x); return out;
    }

    /** 集合对称差（属于其中一个但不属于交集） */
    public static <T> List<T> symmetricDifference(List<T> a, List<T> b) {
        return union(difference(a,b), difference(b,a));
    }

    /** 拉链两个列表为对（短的部分忽略） */
    public static <A,B> List<Map.Entry<A,B>> zip(List<A> a, List<B> b) {
        List<Map.Entry<A,B>> out=new ArrayList<>(); if(a==null||b==null) return out; int n=Math.min(a.size(),b.size()); for(int i=0;i<n;i++) out.add(new AbstractMap.SimpleEntry<>(a.get(i), b.get(i))); return out;
    }

    /** 拉链反解：键为第一个列表，值为第二个列表（长度不一致按较短的） */
    public static <A,B> Map<A,B> unzipToMap(List<A> a, List<B> b) {
        Map<A,B> m=new LinkedHashMap<>(); if(a==null||b==null) return m; int n=Math.min(a.size(),b.size()); for(int i=0;i<n;i++) m.put(a.get(i), b.get(i)); return m;
    }

    /** 展开二维列表为一维列表（浅拷贝） */
    public static <T> List<T> flatten(List<List<T>> lists) { List<T> out=new ArrayList<>(); if(lists!=null) for(List<T> l:lists) if(l!=null) out.addAll(l); return out; }

    /** 频次统计（保留首次出现顺序） */
    public static <T> Map<T,Integer> frequencies(List<T> list) { Map<T,Integer> m=new LinkedHashMap<>(); if(list!=null) for(T t:list) m.put(t, m.getOrDefault(t,0)+1); return m; }

    /** TopK（按比较器降序返回前 K 个） */
    public static <T> List<T> topK(List<T> list, int k, Comparator<T> cmp) { if(list==null||k<=0) return Collections.emptyList(); List<T> a=new ArrayList<>(list); a.sort(cmp); Collections.reverse(a); return a.subList(0, Math.min(k,a.size())); }

    /** 取前 N 个元素（不足返回全部） */
    public static <T> List<T> head(List<T> list, int n) { if(list==null||n<=0) return Collections.emptyList(); return new ArrayList<>(list.subList(0, Math.min(n, list.size()))); }

    /** 取后 N 个元素（不足返回全部） */
    public static <T> List<T> tail(List<T> list, int n) { if(list==null||n<=0) return Collections.emptyList(); int s=Math.max(0,list.size()-n); return new ArrayList<>(list.subList(s, list.size())); }

    /** 按条件保留（保持顺序） */
    public static <T> List<T> keepIf(List<T> list, Predicate<T> p) { if(list==null||p==null) return Collections.emptyList(); List<T> out=new ArrayList<>(); for(T t:list) if(p.test(t)) out.add(t); return out; }

    /** 按条件丢弃（保持顺序） */
    public static <T> List<T> dropIf(List<T> list, Predicate<T> p) { if(list==null||p==null) return Collections.emptyList(); List<T> out=new ArrayList<>(); for(T t:list) if(!p.test(t)) out.add(t); return out; }

    /** 取前缀直到条件不满足（类似 takeWhile） */
    public static <T> List<T> takeWhile(List<T> list, Predicate<T> p) { List<T> out=new ArrayList<>(); if(list==null||p==null) return out; for(T t:list){ if(!p.test(t)) break; out.add(t);} return out; }

    /** 丢弃前缀直到条件不满足（类似 dropWhile） */
    public static <T> List<T> dropWhile(List<T> list, Predicate<T> p) { List<T> out=new ArrayList<>(); if(list==null||p==null) return out; boolean dropping=true; for(T t:list){ if(dropping && !p.test(t)) dropping=false; if(!dropping) out.add(t);} return out; }

    /** 按规则唯一化（保留首次出现） */
    public static <T,K> List<T> distinctBy(List<T> list, Function<T,K> keyFn) { if(list==null) return Collections.emptyList(); Set<K> seen=new LinkedHashSet<>(); List<T> out=new ArrayList<>(); for(T t:list){ K k=keyFn.apply(t); if(seen.add(k)) out.add(t);} return out; }

    /** 交错合并两个列表（若长度不同，尾部追加较长列表的剩余） */
    public static <T> List<T> interleave(List<T> a, List<T> b) { List<T> out=new ArrayList<>(); int n=Math.max(a==null?0:a.size(), b==null?0:b.size()); for(int i=0;i<n;i++){ if(a!=null&&i<a.size()) out.add(a.get(i)); if(b!=null&&i<b.size()) out.add(b.get(i)); } return out; }

    /** 旋转列表（正数向右，负数向左） */
    public static <T> List<T> rotate(List<T> list, int k) { if(list==null||list.isEmpty()) return Collections.emptyList(); int n=list.size(); k=((k%n)+n)%n; List<T> out=new ArrayList<>(n); out.addAll(list.subList(n-k,n)); out.addAll(list.subList(0,n-k)); return out; }

    /** 洗牌拷贝（不改变原列表） */
    public static <T> List<T> shuffleCopy(List<T> list) { if(list==null) return Collections.emptyList(); List<T> out=new ArrayList<>(list); java.util.Collections.shuffle(out, new java.security.SecureRandom()); return out; }

    /** 生成长度为 N 的窗口聚合（将窗口元素经函数映射为值） */
    public static <T,R> List<R> mapWindows(List<T> list, int window, Function<List<T>,R> fn) { List<R> out=new ArrayList<>(); for(List<T> w:slidingWindow(list,window)) out.add(fn.apply(w)); return out; }

    /** 枚举笛卡尔积（a × b） */
    public static <A,B> List<Map.Entry<A,B>> cartesianProduct(List<A> a, List<B> b) { List<Map.Entry<A,B>> out=new ArrayList<>(); if(a==null||b==null) return out; for(A x:a) for(B y:b) out.add(new AbstractMap.SimpleEntry<>(x,y)); return out; }

    /** 是否存在任意满足条件的元素 */
    public static <T> boolean anyMatch(List<T> list, Predicate<T> p) { if(list==null||p==null) return false; for(T t:list) if(p.test(t)) return true; return false; }

    /** 是否全部满足条件 */
    public static <T> boolean allMatch(List<T> list, Predicate<T> p) { if(list==null||p==null) return false; for(T t:list) if(!p.test(t)) return false; return true; }

    /** 是否无任何满足条件的元素 */
    public static <T> boolean noneMatch(List<T> list, Predicate<T> p) { return !anyMatch(list, p); }

    /** 将列表分为满足与不满足条件两部分 */
    public static <T> Map<Boolean,List<T>> partitionBy(List<T> list, Predicate<T> p) { Map<Boolean,List<T>> m=new LinkedHashMap<>(); m.put(Boolean.TRUE,new ArrayList<>()); m.put(Boolean.FALSE,new ArrayList<>()); if(list!=null) for(T t:list) (p.test(t)?m.get(Boolean.TRUE):m.get(Boolean.FALSE)).add(t); return m; }

    /** 在每 N 个元素后插入分隔元素（返回新列表） */
    public static <T> List<T> insertSeparatorEveryN(List<T> list, int n, T sep) { List<T> out=new ArrayList<>(); if(list==null||n<=0) return out; for(int i=0;i<list.size();i++){ out.add(list.get(i)); if((i+1)%n==0 && i<list.size()-1) out.add(sep); } return out; }
}

