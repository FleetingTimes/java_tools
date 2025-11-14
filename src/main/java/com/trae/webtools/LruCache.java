package com.trae.webtools;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 {@link LinkedHashMap} 的 LRU 缓存
 *
 * 概念说明：
 * - LRU（Least Recently Used，最近最少使用）是一种淘汰策略：当缓存达到容量上限时，优先移除最近最少被访问的条目，保留近期被访问的“热点”数据。
 *
 * 工作原理（本实现）：
 * - 使用访问顺序（accessOrder=true）维护条目，任意一次 get/put 命中都会将该条目移动到队尾；队首为“最近最少使用”。
 * - 通过覆盖 {@link #removeEldestEntry(Map.Entry)}，在插入新条目后检查当前大小，超过容量即移除队首最老条目完成淘汰。
 *
 * 适用场景：
 * - 小型内存缓存：接口返回值、模板片段、配置与元数据等频繁复用对象的就地缓存。
 * - 热点保留：访问存在“时间局部性”的场景（近期访问更可能再次访问）。
 *
 * 与其他策略对比：
 * - 与 LFU（最不常用）相比：LRU关注“最近访问时间”，LFU关注“访问频次”；LRU对短期热点友好，LFU对长期高频更友好。
 * - 与 TTL/过期相比：LRU按访问顺序淘汰，TTL按时间过期，二者可结合（见另一工具中的 TTLCache）。
 *
 * 线程安全与复杂度：
 * - 本实现基于普通 {@link LinkedHashMap}，线程不安全；并发场景需在外层加锁或使用并发映射包裹。
 * - put/get 为 O(1) 平均时间复杂度；淘汰检查在插入时进行一次常数判断。
 *
 * 用法示例：
 * <pre>
 * LruCache<String, String> cache = new LruCache<>(1000);
 * cache.put("k", "v");
 * String v = cache.get("k");
 * </pre>
 */
public class LruCache<K,V> extends LinkedHashMap<K,V> {
    private final int capacity;
    /**
     * 构造 LRU 缓存
     * @param capacity 容量上限（>0），超过时自动淘汰最旧访问的条目
     */
    public LruCache(int capacity) { super(capacity, 0.75f, true); this.capacity = Math.max(1, capacity); }

    /**
     * 当大小超过容量时移除最久未使用条目
     * @param eldest 框架提供的最老条目（访问顺序）
     * @return 是否移除（true 表示触发淘汰）
     */
    @Override protected boolean removeEldestEntry(Map.Entry<K,V> eldest) { return size() > capacity; }
}
