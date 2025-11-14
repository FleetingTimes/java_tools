package com.trae.webtools;

import java.util.concurrent.*;

/**
 * 调度工具：一次/固定速率/固定延迟调度、关闭与等待
 *
 * 功能概览：
 * - 创建调度器：单线程与固定线程数的 {@link ScheduledExecutorService}
 * - 任务调度：延迟一次执行、固定速率（fixed rate）与固定延迟（fixed delay）执行、延迟 Callable
 * - 关闭与等待：优雅关闭、强制关闭、等待终止
 *
 * 选择建议：
 * - fixed rate：以固定时间步长触发（相对起始时间），适合周期性心跳/指标上报；若任务耗时过长可能导致下一次紧接执行（需要考虑并发与重入问题）
 * - fixed delay：以上一次执行完成时间为基准延迟触发，适合串行任务轮询；不会在任务执行期间进行新的触发
 * - 单线程调度器：任务串行执行，便于避免共享状态并发问题；固定线程数用于并行定时任务
 *
 * 使用建议：
 * - 任务体中捕获并处理异常，避免未捕获异常导致调度线程终止或任务停止
 * - 为线程设置明确名称与异常处理器（可在自定义 ThreadFactory 中实现），便于定位问题与监控
 * - 关闭前先停止接收新任务并等待一定时间，再必要时强制关闭，确保资源释放与一致性
 */
public final class SchedulerUtils {
    private SchedulerUtils() {}

    /**
     * 创建单线程调度器
     *
     * 适用场景：串行定时任务、避免并发共享状态；任务按提交顺序执行。
     * @return 单线程 {@link ScheduledExecutorService}
     */
    public static ScheduledExecutorService newSingleScheduler() { return Executors.newSingleThreadScheduledExecutor(); }

    /**
     * 创建固定线程数调度器
     *
     * 适用场景：并行定时任务或任务耗时较长需要并行处理；注意线程安全与任务重入。
     * @param threads 线程数（最小 1）
     * @return 固定线程数 {@link ScheduledExecutorService}
     */
    public static ScheduledExecutorService newScheduler(int threads) { return Executors.newScheduledThreadPool(Math.max(1, threads)); }

    /**
     * 延迟一次执行
     *
     * 适用场景：延迟启动任务、超时回调、定时一次性操作。
     * @param sch 调度器
     * @param task 任务（{@link Runnable}）
     * @param delay 延迟时长
     * @param unit 时间单位
     * @return 可取消的 {@link ScheduledFuture}
     */
    public static ScheduledFuture<?> scheduleOnce(ScheduledExecutorService sch, Runnable task, long delay, TimeUnit unit) { return sch.schedule(task, delay, unit); }

    /**
     * 固定速率执行（从初始延迟开始，按固定速率）
     *
     * 适用场景：固定周期心跳、指标采集、周期触发任务（相对起始时间为参考）。
     * 行为说明：下一次触发时间点按固定周期计算；若任务耗时超过周期，后续触发可能紧密跟随（需考虑并发与重入）。
     * @param sch 调度器
     * @param task 任务（周期执行）
     * @param initialDelay 初始延迟
     * @param period 周期时长
     * @param unit 时间单位
     * @return 可取消的 {@link ScheduledFuture}
     */
    public static ScheduledFuture<?> scheduleAtFixedRate(ScheduledExecutorService sch, Runnable task, long initialDelay, long period, TimeUnit unit) { return sch.scheduleAtFixedRate(task, initialDelay, period, unit); }

    /**
     * 固定延迟执行（任务完成后再延迟）
     *
     * 适用场景：串行轮询、需要保证任务间隔的场景（延迟以完成时间为基准）。
     * 行为说明：每次任务完成后再等待 delay 后执行下一次；不会在任务执行期间触发新的执行。
     * @param sch 调度器
     * @param task 任务（串行执行）
     * @param initialDelay 初始延迟
     * @param delay 完成后延迟时长
     * @param unit 时间单位
     * @return 可取消的 {@link ScheduledFuture}
     */
    public static ScheduledFuture<?> scheduleWithFixedDelay(ScheduledExecutorService sch, Runnable task, long initialDelay, long delay, TimeUnit unit) { return sch.scheduleWithFixedDelay(task, initialDelay, delay, unit); }

    /**
     * 延迟执行 Callable 并返回结果
     *
     * 适用场景：延迟获取数据、异步计算在指定时间执行并返回结果。
     * @param sch 调度器
     * @param task 任务（有返回值）
     * @param delay 延迟时长
     * @param unit 时间单位
     * @return 带结果的 {@link ScheduledFuture}
     */
    public static <T> ScheduledFuture<T> scheduleCallable(ScheduledExecutorService sch, Callable<T> task, long delay, TimeUnit unit) { return sch.schedule(task, delay, unit); }

    /**
     * 优雅关闭（停止接受新任务，完成已提交任务）
     *
     * 适用场景：应用退出或模块重载，确保在给定时间内完成已提交任务并释放资源。
     * 行为说明：先 {@code shutdown()} 停止接收新任务，等待至超时；若未终止则 {@code shutdownNow()} 强制中断。
     * @param ex 线程池或调度器
     * @param timeout 最大等待时长
     * @param unit 时间单位
     */
    public static void shutdownGracefully(ExecutorService ex, long timeout, TimeUnit unit) {
        ex.shutdown();
        try { if(!ex.awaitTermination(timeout, unit)) ex.shutdownNow(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); ex.shutdownNow(); }
    }

    /**
     * 立即强制关闭（尝试中断运行中的任务）
     *
     * 适用场景：无法优雅结束或需紧急停止任务时。
     * @param ex 线程池或调度器
     */
    public static void shutdownNow(ExecutorService ex) { ex.shutdownNow(); }

    /**
     * 等待终止（返回是否在超时内终止）
     *
     * 适用场景：外部控制流程需等待线程池在限定时间内结束。
     * @param ex 线程池或调度器
     * @param timeout 最大等待时长
     * @param unit 时间单位
     * @return 是否在给定时间内完成终止
     */
    public static boolean awaitTermination(ExecutorService ex, long timeout, TimeUnit unit) { try { return ex.awaitTermination(timeout, unit); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; } }
}
