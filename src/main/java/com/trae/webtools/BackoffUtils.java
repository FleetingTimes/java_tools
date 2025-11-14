package com.trae.webtools;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 回退策略工具：固定、指数、指数带抖动
 *
 * 用途：
 * - 结合重试机制控制两次尝试之间的等待时间，缓解瞬时故障与脉冲拥塞，提高成功率与系统稳定性。
 * - 常用于网络调用、第三方接口、数据库/队列客户端的重试等待策略。
 *
 * 名词说明：
 * - attempt：第几次尝试的序号，通常从 0 开始（第一次重试 attempt=0，对应 baseMillis）。
 * - baseMillis：基础等待时长；指数策略会按 2^attempt 放大。
 * - maxMillis：等待时长上限；用于防止无限膨胀与长时间阻塞。
 *
 * 选择建议：
 * - fixed：简单、确定性；适合轻量重试或对时间敏感的场景。
 * - exponential：失败越多等待越长；适合退避拥塞，但存在“同步抖动”风险（多客户端同时重试）。
 * - exponentialJitter：在指数上限范围内随机，降低同步抖动；适合大规模并发重试，推荐默认策略。
 */
public final class BackoffUtils {
    private BackoffUtils() {}

    /**
     * 固定间隔回退（毫秒）
     *
     * 逻辑：返回一个非负的固定等待时长；负值会被钳制为 0。
     * 适用：简单重试或时间窗口严格受控的场景。
     * @param baseMillis 基础等待时长（毫秒），负值会被视为 0
     * @return 非负等待时长（毫秒）
     */
    public static long fixed(long baseMillis) { return Math.max(0, baseMillis); }

    /**
     * 指数回退：base * (2^attempt)，并钳制到 maxMillis
     *
     * 公式：waitMillis = min(max(0, base * 2^attempt), max(0, maxMillis))
     * 说明：attempt 建议从 0 开始；当 base 或 maxMillis 为负时分别按 0 处理。
     * 优点：失败越多等待越长，避免持续施压；缺点：大规模并发可能出现“同时醒来”导致再次碰撞。
     * @param baseMillis 基础等待时长（毫秒）
     * @param attempt 第几次尝试（>=0，建议从 0 开始）
     * @param maxMillis 最长等待上限（毫秒）
     * @return 指数增长并钳制后的等待时长（毫秒）
     */
    public static long exponential(long baseMillis, int attempt, long maxMillis) {
        double v = baseMillis * Math.pow(2.0, Math.max(0, attempt));
        return (long) Math.min(Math.max(0, v), Math.max(0, maxMillis));
    }

    /**
     * 指数回退（带全抖动）：在 [0, exponential(...)] 区间内随机
     *
     * 参考：AWS/Amazon 的“Full Jitter”策略，可有效减少同步抖动与冲突重试。
     * 逻辑：先计算指数上限 max = exponential(...)，再从 [0, max] 均匀随机一个等待时长。
     * 使用：适合高并发下对外部服务的重试等待，推荐作为默认退避策略。
     * @param baseMillis 基础等待时长（毫秒）
     * @param attempt 第几次尝试（>=0，建议从 0 开始）
     * @param maxMillis 最长等待上限（毫秒）
     * @return 随机等待时长（毫秒）；若上限 <= 0 则返回 0
     */
    public static long exponentialJitter(long baseMillis, int attempt, long maxMillis) {
        long max = exponential(baseMillis, attempt, maxMillis);
        return (max <= 0) ? 0 : ThreadLocalRandom.current().nextLong(0, max + 1);
    }
}
