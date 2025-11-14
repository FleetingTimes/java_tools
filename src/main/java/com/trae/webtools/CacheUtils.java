package com.trae.webtools;

/**
 * 缓存与限流工具：令牌桶与 LRU 缓存
 *
 * 用途：
 * - 快速创建“令牌桶”限流器与“最近最少使用”缓存结构，避免重复造轮子。
 * - 可用于 API 限流、突发流量吸收（burst）、热点数据缓存等常见场景。
 *
 * 设计说明：
 * - RateLimiter：基于令牌桶，按速率向桶中补充令牌；允许设置突发容量 burst（桶最大令牌数）。
 * - LruCache：基于 LinkedHashMap 的访问顺序淘汰实现，容量固定，超过后自动淘汰最久未访问项。
 *
 * 典型使用：
 * - 限流：RateLimiter limiter = CacheUtils.newRateLimiter(50.0, 100); limiter.tryAcquire()
 * - 缓存：LruCache<K,V> cache = CacheUtils.newLruCache(1024); cache.put(k, v); cache.get(k)
 */
public final class CacheUtils {
    private CacheUtils() {}

    /**
     * 创建简易令牌桶速率限制器
     * @param permitsPerSecond 每秒补充令牌数（>0）
     * @param burst 桶最大令牌数（>0），用于吸收短时突发
     * @return RateLimiter 实例
     */
    public static RateLimiter newRateLimiter(double permitsPerSecond, int burst) { return new RateLimiter(permitsPerSecond, burst); }

    /**
     * 创建固定容量 LRU 缓存
     * @param capacity 容量上限（>0）
     * @return LruCache 实例；超过容量时自动淘汰最久未访问项
     */
    public static <K,V> LruCache<K,V> newLruCache(int capacity) { return new LruCache<>(capacity); }
}
