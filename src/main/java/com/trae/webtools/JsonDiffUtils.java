package com.trae.webtools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量 JSON Diff 工具：面向平面 Map<String,Object> 的差异计算
 *
 * 概念说明：
 * - Diff（差异）用于描述从对象 A 变更到对象 B 的最小操作集合；本工具定义三类原子操作：
 *   add（新增键）、remove（删除键）、replace（替换键值）。
 * - 适用于平面 {@code Map<String,Object>}（不支持嵌套结构与数组）；忽略键顺序，仅比较键集合与对应值相等性。
 *
 * 使用场景：
 * - 配置增量下发：通过差异集对当前配置进行最小变更而非整体重写。
 * - 变更审计与回滚：记录 add/remove/replace 操作，支持回放与撤销（结合业务规则）。
 * - 简易同步：对平面 Map 做轻量级同步与合并（注意合并冲突策略由业务决定）。
 *
 * 限制与说明：
 * - 值相等使用 {@link java.util.Objects#equals(Object, Object)}（允许 null），不进行深度比较与类型转换。
 * - 不支持嵌套结构的路径键（如 a.b.c）与数组的结构化 Diff；如需复杂 Diff 请使用专用 JSON 库。
 * - apply（应用差异）为幂等的覆盖式应用：add/replace 将设置新值；remove 将移除键；不包含冲突检测与前置条件判断。
 */
public final class JsonDiffUtils {
    private JsonDiffUtils() {}

    /**
     * 变更项模型
     *
     * 字段：
     * - op：操作类型（"add" / "remove" / "replace"）
     * - key：目标键名
     * - value：新值（add/replace 使用；remove 为 null）
     */
    public static final class Change { public final String op; public final String key; public final Object value; public Change(String op,String key,Object value){ this.op=op; this.key=key; this.value=value; } }

    /**
     * 计算差异（a -> b）：新增（add）、删除（remove）、替换（replace）
     *
     * 规则：
     * - 对于仅存在于 b 的键：生成 add
     * - 对于仅存在于 a 的键：生成 remove
     * - 对于同时存在但值不相等的键：生成 replace（值取 b 的值）
     * 等价性：使用 {@link #equalsSafe(Object, Object)} 判断（允许 null）。
     *
     * @param a 源 Map（允许 null，视为空）
     * @param b 目标 Map（允许 null，视为空）
     * @return 从 a 到 b 的变更列表（按键并集遍历顺序）
     */
    public static List<Change> diff(Map<String,Object> a, Map<String,Object> b) {
        List<Change> out=new ArrayList<>(); Map<String,Object> A=a==null?new LinkedHashMap<>():a; Map<String,Object> B=b==null?new LinkedHashMap<>():b;
        java.util.Set<String> keys=new java.util.LinkedHashSet<>(); keys.addAll(A.keySet()); keys.addAll(B.keySet());
        for(String k:keys){ boolean hasA=A.containsKey(k), hasB=B.containsKey(k); Object va=A.get(k), vb=B.get(k);
            if(!hasA && hasB) out.add(new Change("add", k, vb));
            else if(hasA && !hasB) out.add(new Change("remove", k, null));
            else if(!equalsSafe(va, vb)) out.add(new Change("replace", k, vb));
        }
        return out;
    }

    /**
     * 平面 Map 的安全相等（允许 null）
     *
     * 委托给 {@link java.util.Objects#equals(Object, Object)}，用于判定是否生成 replace 操作。
     * @param a 值 A
     * @param b 值 B
     * @return 是否相等
     */
    public static boolean equalsSafe(Object a, Object b) { return java.util.Objects.equals(a,b); }

    /**
     * 根据变更应用到目标（平面 Map）
     *
     * 语义：
     * - add/replace：设置 {@code key -> value}
     * - remove：移除 key
     * 幂等性：相同变更重复应用结果不变；不包含并发冲突与前置条件检查。
     *
     * @param target 目标 Map（允许 null，视为空）
     * @param changes 变更列表（允许 null，视为空）
     * @return 应用后的新 Map（LinkedHashMap，保留原有键及变更结果）
     */
    public static Map<String,Object> apply(Map<String,Object> target, List<Change> changes) {
        Map<String,Object> out=new LinkedHashMap<>(); if(target!=null) out.putAll(target);
        if(changes!=null) for(Change c:changes){ if("add".equals(c.op)||"replace".equals(c.op)) out.put(c.key,c.value); else if("remove".equals(c.op)) out.remove(c.key); }
        return out;
    }
}
