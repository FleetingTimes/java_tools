package com.trae.webtools;

import java.time.Duration;

/**
 * 秒表：用于性能测量
 *
 * 使用单调时钟 {@code System.nanoTime()} 计算耗时，避免系统时间变更影响。
 * 提供毫秒值与人性化文本两种输出形式。
 *
 * 适用场景：
 * - 代码片段或接口的响应时间统计（性能分析、日志埋点、SLA 度量）
 * - 批处理任务/IO 操作/重试过程的耗时测量与记录
 * - 简易基准测试与对比（非微基准，避免 JIT/GC 干扰的专业基准需使用专用框架）
 *
 * 用法示例：
 * <pre>
 * Stopwatch sw = ConcurrencyUtils.startStopwatch();
 * // 执行业务逻辑
 * long ms = sw.stopMillis();  // 记录耗时毫秒
 * String pretty = sw.stopHuman(); // 记录人性化文本（如 "1.23s"）
 * </pre>
 *
 * 说明：
 * - 实例从创建时刻开始计时；多次调用 stopXXX 方法都会以当前时间减去起始时间进行计算。
 * - 建议一处创建、一处记录，避免重复打印造成日志噪声。
 */
public final class Stopwatch {
    private final long start = System.nanoTime();
    /**
     * 停止并返回毫秒耗时
     *
     * 适用场景：用于日志与监控记录、阈值判断（如超过 500ms 告警）。
     * 说明：返回的是起始到当前的毫秒差值（四舍五入向下取整至毫秒）。
     * @return 耗时毫秒
     */
    public long stopMillis() { return (System.nanoTime() - start) / 1_000_000L; }
    /**
     * 停止并返回人性化描述
     *
     * 适用场景：用于人读友好的日志输出与报表展示（如 "123ms"、"1.2s"、"3m20s"）。
     * 说明：基于纳秒差值构造 {@link java.time.Duration}，再调用 {@code DateTimeUtils.durationHumanize} 进行格式化。
     * @return 人性化耗时文本
     */
    public String stopHuman() { return DateTimeUtils.durationHumanize(Duration.ofNanos(System.nanoTime() - start)); }
}
