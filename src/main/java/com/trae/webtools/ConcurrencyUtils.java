package com.trae.webtools;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 并发与健壮性工具
 *
 * 聚焦常用并发辅助能力：
 * - 安全睡眠：处理中断并恢复标志，避免静默吞掉中断
 * - 简单重试：固定间隔与最大次数（复杂策略见 {@link RetryPlusUtils}）
 * - 超时执行：单线程执行任务并在指定时间内获取结果，超时抛出异常
 * - 线程池：固定大小线程池构造
 * - 秒表：性能测量辅助（见 {@link Stopwatch}）
 */
public final class ConcurrencyUtils {
    private ConcurrencyUtils() {}

    /**
     * 线程睡眠（毫秒）
     *
     * 将 {@link InterruptedException} 转为 {@link RuntimeException} 并恢复中断标志。
     * @param millis 睡眠时长毫秒
     */
    public static void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
    }

    /** 启动一个秒表用于性能测量 */
    public static Stopwatch startStopwatch() { return new Stopwatch(); }

    /**
     * 重试工具（固定间隔）
     * @param action 执行的操作
     * @param maxAttempts 最大尝试次数（>0）
     * @param delayMillis 每次失败后的等待毫秒
     * @return 成功结果
     * @throws RuntimeException 达到最大次数仍失败时抛出最后一次异常
     */
    public static <T> T retry(Supplier<T> action, int maxAttempts, long delayMillis) {
        RuntimeException last = null;
        for (int i = 0; i < maxAttempts; i++) {
            try { return action.get(); } catch (RuntimeException e) { last = e; sleep(delayMillis); }
        }
        throw last == null ? new RuntimeException("retry failed") : last;
    }

    /**
     * 在超时时间内执行并返回结果（超时抛异常）
     * @param task 待执行任务
     * @param timeoutMillis 超时时长毫秒
     * @return 任务结果
     * @throws Exception 执行失败或超时
     */
    public static <T> T withTimeout(Callable<T> task, long timeoutMillis) throws Exception {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        try { Future<T> f = ex.submit(task); return f.get(timeoutMillis, TimeUnit.MILLISECONDS); } finally { ex.shutdownNow(); }
    }

    /** 创建固定大小线程池 */
    public static ExecutorService threadPoolFixed(int size) { return Executors.newFixedThreadPool(Math.max(1, size)); }
}
