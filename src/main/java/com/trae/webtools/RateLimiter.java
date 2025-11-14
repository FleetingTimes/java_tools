package com.trae.webtools;

/**
 * 令牌桶速率限制器
 *
 * 基于令牌桶（Token Bucket）算法的轻量级速率控制实现：
 * - 以固定速率向桶中补充令牌；最大累积不超过桶容量（用于吸收短时突发）
 * - 每次调用 {@link #tryAcquire()} 消耗 1 个令牌，若令牌不足则拒绝
 * - 使用单个对象监视器保证并发安全（方法加 synchronized），适合单实例多线程场景
 *
 * 适用场景：
 * - 限制外部接口调用频率、日志写入速率、任务提交速率
 * - 对突发流量提供有限缓冲（通过更大的 burst）
 *
 * 设计说明：
 * - 时间源采用 {@code System.nanoTime()}，避免系统时间回拨影响
 * - 补充令牌按时间差线性计算；每次获取前先执行补充与裁剪（不超过容量）
 * - rate、burst 在构造时进行下限保护，避免 0/负值导致异常
 *
 * 使用示例：
 * <pre>
 * RateLimiter limiter = new RateLimiter(10.0, 20); // 每秒 10 次，桶容量 20
 * if (limiter.tryAcquire()) {
 *     // 允许执行受限操作
 * } else {
 *     // 超过速率，进行降级或延迟
 * }
 * </pre>
 */
public final class RateLimiter {
    /** 每秒补充令牌速率（>0） */
    private final double rate;
    /** 桶容量（>0），用于吸收短时突发 */
    private final int burst;
    /** 当前令牌数（实时变化，按补充与消耗更新） */
    private double tokens;
    /** 上一次补充时间（纳秒，使用单调时钟） */
    private long lastNs;

    /**
    * 构造令牌桶
    *
    * @param permitsPerSecond 每秒补充令牌数（建议为正实数）
    * @param burst 桶最大令牌数（建议为正整数）
    *
    * 参数保护：当传入非正值时将按最小有效值修正（rate=0.0001，burst=1），避免不可用状态。
    */
    public RateLimiter(double permitsPerSecond, int burst) {
        this.rate = Math.max(0.0001, permitsPerSecond);
        this.burst = Math.max(1, burst);
        this.tokens = this.burst;
        this.lastNs = System.nanoTime();
    }

    /**
    * 尝试获取一个令牌
    *
    * 并发安全：方法加 synchronized；多线程下可直接调用。
    * 算法步骤：
    * - 计算距离上次补充的时间差，将令牌按 {@code rate * deltaSeconds} 线性补充
    * - 令牌上限裁剪到 {@code burst}
    * - 若令牌≥1，则消耗 1 并返回 true；否则返回 false
    *
    * @return 是否成功获取令牌（true=允许执行，false=超速率）
    */
    public synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        double add = (now - lastNs) / 1_000_000_000.0 * rate;
        tokens = Math.min(burst, tokens + add);
        lastNs = now;
        if (tokens >= 1.0) { tokens -= 1.0; return true; }
        return false;
    }
}
