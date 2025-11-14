package com.trae.webtools;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 集合工具：判空、拼接、过滤、映射、去重、排序、反转、索引、分组
 */
public final class CollectionUtils {
    private CollectionUtils() {}

    /** 判断集合是否为空（null 或 size==0） */
    public static boolean isEmpty(Collection<?> c) { return c == null || c.isEmpty(); }

    /** 判断集合是否非空 */
    public static boolean notEmpty(Collection<?> c) { return !isEmpty(c); }

    /** 安全获取集合大小（null 返回 0） */
    public static int size(Collection<?> c) { return c == null ? 0 : c.size(); }

    /** 将若干元素构造为不可变列表 */
    @SafeVarargs public static <T> List<T> toList(T... items) { return Collections.unmodifiableList(Arrays.asList(items)); }

    /** 连接两个列表（不修改原列表） */
    public static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> out = new ArrayList<>((a==null?0:a.size()) + (b==null?0:b.size()));
        if (a!=null) out.addAll(a); if (b!=null) out.addAll(b); return out;
    }

    /** 过滤列表（保持原顺序） */
    public static <T> List<T> filter(List<T> list, Predicate<T> p) {
        if (list == null) return Collections.emptyList();
        List<T> out = new ArrayList<>(list.size());
        for (T t : list) if (p.test(t)) out.add(t);
        return out;
    }

    /** 映射列表（保持原顺序） */
    public static <A,B> List<B> map(List<A> list, Function<A,B> f) {
        if (list == null) return Collections.emptyList();
        List<B> out = new ArrayList<>(list.size());
        for (A a : list) out.add(f.apply(a));
        return out;
    }

    /** 去重（保持首次出现顺序） */
    public static <T> List<T> distinct(List<T> list) {
        if (list == null) return Collections.emptyList();
        LinkedHashSet<T> set = new LinkedHashSet<>(list);
        return new ArrayList<>(set);
    }

    /** 按比较器排序（返回新列表） */
    public static <T> List<T> sort(List<T> list, Comparator<T> cmp) {
        if (list == null) return Collections.emptyList();
        List<T> out = new ArrayList<>(list);
        out.sort(cmp);
        return out;
    }

    /** 反转列表（返回新列表） */
    public static <T> List<T> reverse(List<T> list) {
        if (list == null) return Collections.emptyList();
        List<T> out = new ArrayList<>(list);
        Collections.reverse(out);
        return out;
    }

    /** 查找满足条件的元素索引，不存在返回 -1 */
    public static <T> int indexOf(List<T> list, Predicate<T> p) {
        if (list == null) return -1;
        for (int i = 0; i < list.size(); i++) if (p.test(list.get(i))) return i;
        return -1;
    }

    /** 按键函数进行分组 */
    public static <T,K> Map<K,List<T>> groupBy(List<T> list, Function<T,K> keyFn) {
        if (list == null) return Collections.emptyMap();
        Map<K,List<T>> m = new LinkedHashMap<>();
        for (T t : list) m.computeIfAbsent(keyFn.apply(t), k -> new ArrayList<>()).add(t);
        return m;
    }
}

