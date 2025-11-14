package com.trae.webtools;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 重试增强工具：支持回退策略与最大时长
 *
 * 提供两类重试：
 * - {@link #retryWithBackoff(Supplier, int, long, long)}：按指数回退+随机抖动进行重试，达到最大次数后抛出最后一次异常
 * - {@link #retryWithTimeout(Callable, long, long, long)}：在总超时限制内进行重试，若超时仍失败则抛出最后一次异常
 *
 * 设计要点：
 * - 回退策略通过 {@link BackoffUtils#exponentialJitter(long, int, long)} 计算，避免惊群与同步重试导致的雪崩
 * - 睡眠使用 {@link ConcurrencyUtils#sleep(long)}，将中断转化为运行时异常并恢复中断标志
 * - Supplier 版适用于无受检异常的操作；Callable 版用于可能抛受检异常的场景
 */
public final class RetryPlusUtils {
    private RetryPlusUtils() {}

    /**
     * 带回退策略的重试（Supplier 版）
     *
     * 算法：尝试执行 action，失败则按指数回退+抖动计算下一次等待时间，直到成功或达到最大次数。
     * @param action 要执行的操作（失败应抛 RuntimeException）
     * @param maxAttempts 最大尝试次数（>0）
     * @param baseDelayMillis 基础延迟毫秒（>0）
     * @param maxDelayMillis 最大延迟上限毫秒（>=baseDelayMillis）
     * @return 操作成功的结果
     * @throws RuntimeException 达到最大次数仍失败时抛出最后一次异常
     */
    public static <T> T retryWithBackoff(Supplier<T> action, int maxAttempts, long baseDelayMillis, long maxDelayMillis) {
        RuntimeException last=null; for(int i=0;i<maxAttempts;i++){ try{ return action.get(); }catch(RuntimeException e){ last=e; long d=BackoffUtils.exponentialJitter(baseDelayMillis, i, maxDelayMillis); if(d>0) ConcurrencyUtils.sleep(d); } } throw last==null?new RuntimeException("retry failed"):last; }

    /**
     * 带回退策略与总超时的重试（Callable 版）
     *
     * 在给定的总超时时间内重复执行，失败时进行回退等待；若时间用尽仍失败则抛异常。
     * @param action 要执行的操作（可能抛受检异常）
     * @param totalTimeoutMillis 总超时时长毫秒（>0）
     * @param baseDelayMillis 基础延迟毫秒（>0）
     * @param maxDelayMillis 最大延迟上限毫秒（>=baseDelayMillis）
     * @return 操作成功的结果
     * @throws RuntimeException 超时或最终失败时抛出最后一次异常（受检异常包装为 RuntimeException）
     */
    public static <T> T retryWithTimeout(Callable<T> action, long totalTimeoutMillis, long baseDelayMillis, long maxDelayMillis) {
        long start=System.currentTimeMillis(); RuntimeException last=null; int attempt=0; while(System.currentTimeMillis()-start<totalTimeoutMillis){ try{ return action.call(); }catch(Exception e){ last = e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e); long remain=totalTimeoutMillis-(System.currentTimeMillis()-start); if(remain<=0) break; long d=Math.min(BackoffUtils.exponentialJitter(baseDelayMillis, attempt++, maxDelayMillis), Math.max(0,remain)); if(d>0) ConcurrencyUtils.sleep(d); } } throw last==null?new RuntimeException("retry timeout"):last; }
}
