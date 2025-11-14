package com.trae.webtools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 高级缓存工具：TTLCache 与 LFUCache
 *
 * 提供两种与现有 LRU 不同的缓存策略：
 * - TTLCache：按键超时过期；超过容量时按最近最少使用进行淘汰
 * - LFUCache：按访问频次进行淘汰（同频次时依据最近访问时间最早的淘汰）
 *
 * 线程安全：
 * - TTLCache 的 put/get 加了同步，适合轻并发；
 * - LFUCache 使用内部同步与 ConcurrentHashMap 存储，适合轻并发；
 * 如需高并发与弱一致性可考虑使用分段锁或 Caffeine 等专业缓存库（本工具集保持零依赖）。
 */
public final class CacheAdvancedUtils {
    private CacheAdvancedUtils() {}

    /** 创建 TTL 缓存（按键过期，固定容量，最近最少使用淘汰） */
    public static <K,V> TTLCache<K,V> newTTLCache(int capacity, long ttlMillis) { return new TTLCache<>(capacity, ttlMillis); }

    /** 创建 LFU 缓存（最不常用淘汰） */
    public static <K,V> LFUCache<K,V> newLFUCache(int capacity) { return new LFUCache<>(capacity); }

    /** TTLCache：每个键在写入后于指定 TTL 过期；超出容量时按最近最少使用淘汰 */
    public static final class TTLCache<K,V> extends LinkedHashMap<K,TTLCache.Entry<V>> {
        private final int capacity; private final long ttl;
        /**
         * 构造 TTL 缓存
         * @param capacity 容量上限（>0）
         * @param ttlMillis 写入后到过期的毫秒数（>0）
         */
        public TTLCache(int capacity, long ttlMillis) { super(capacity, 0.75f, true); this.capacity=Math.max(1,capacity); this.ttl=Math.max(1,ttlMillis); }
        @Override protected boolean removeEldestEntry(Map.Entry<K,Entry<V>> eldest) { return size()>capacity; }
        /** 写入值（刷新写入时间） */
        public synchronized void putValue(K key, V value) { super.put(key, new Entry<>(value, System.currentTimeMillis())); }
        /** 读取值（若过期返回 null 并移除） */
        public synchronized V getValue(K key) { Entry<V> e=super.get(key); if(e==null) return null; if(System.currentTimeMillis()-e.ts>ttl){ super.remove(key); return null; } return e.value; }
        /** 条目：保存值与写入时间戳 */
        public static final class Entry<V> { public final V value; public final long ts; public Entry(V value,long ts){this.value=value;this.ts=ts;} }
    }

    /** LFUCache：按访问频次淘汰（同频次时最近最早淘汰） */
    public static final class LFUCache<K,V> {
        private final int capacity; private final ConcurrentHashMap<K,Node<K,V>> map=new ConcurrentHashMap<>();
        /** 构造 LFU 缓存 */
        public LFUCache(int capacity) { this.capacity=Math.max(1,capacity); }
        /** 获取值（提升频次） */
        public synchronized V get(K key) { Node<K,V> n=map.get(key); if(n==null) return null; n.freq++; n.last=System.nanoTime(); return n.value; }
        /** 写入值（若超出容量则淘汰最不常用） */
        public synchronized void put(K key, V value) { Node<K,V> n=map.get(key); if(n!=null){ n.value=value; n.freq++; n.last=System.nanoTime(); return; } if(map.size()>=capacity) evict(); map.put(key, new Node<>(key,value)); }
        /** 当前大小 */
        public int size(){ return map.size(); }
        private void evict(){ K victim=null; int vf=Integer.MAX_VALUE; long vl=Long.MAX_VALUE; for(Node<K,V> n:map.values()){ if(n.freq<vf || (n.freq==vf && n.last<vl)){ vf=n.freq; vl=n.last; victim=n.key; } } if(victim!=null) map.remove(victim); }
        /** 节点：保存键、值、频次与最近访问时间 */
        private static final class Node<K,V>{ final K key; V value; int freq=1; long last=System.nanoTime(); Node(K k,V v){key=k;value=v;} }
    }
}
