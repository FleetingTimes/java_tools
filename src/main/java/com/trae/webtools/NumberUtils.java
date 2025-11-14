package com.trae.webtools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

/**
 * 数值工具：钳制、舍入、格式化、范围判断、随机、统计
 *
 * 工具定位：
 * - 提供常用的数值操作与统计：区间钳制、四舍五入、格式化、范围判断、随机数生成、平均/中位/求和与百分比。
 * - 仅依赖 JDK 标准库，适合通用业务逻辑与数据处理；不涉及大数据统计或高精金融计算（需谨慎选择精度策略）。
 *
 * 使用建议：
 * - 四舍五入基于 {@link java.math.BigDecimal}，默认 HALF_UP；金融场景请明确定义舍入与精度，避免隐式 Double 损失。
 * - 格式化受模式与区域影响，跨区域显示时需与前端或报表规范统一；大数与科学计数法需专门处理。
 * - 随机数基于 {@link Math#random()} 的非安全随机，适用于一般业务，不适用于安全场景（令牌/密钥）。
 *
 * 术语说明：钳制（Clamp）
 * - 将数值限制到给定闭区间 [min, max]：若小于 min 则返回 min，若大于 max 则返回 max，否则返回原值；又称“限幅/裁剪”。
 * - 常见用途：
 *   1) 输入参数限幅（例如分页大小、重试次数不超过上限）；
 *   2) UI/动画值范围（滑块取值 0-100、透明度 0-1 等）；
 *   3) 指标与百分比的合理范围约束（确保在 0-100 之间）；
 * - 与截断/舍入的区别：钳制是对范围的裁剪，截断/舍入是对数值的小数或位数进行变换。
 */
public final class NumberUtils {
    private NumberUtils() {}

    /**
     * 钳制 int 到区间
     * @param v 数值
     * @param min 最小值（含）
     * @param max 最大值（含）
     * @return 被限制到 [min,max] 的结果
     */
    public static int clamp(int v, int min, int max) { if (min>max) throw new IllegalArgumentException("min>max"); return Math.max(min, Math.min(max, v)); }

    /**
     * 钳制 long 到区间
     * @param v 数值
     * @param min 最小值（含）
     * @param max 最大值（含）
     * @return 被限制到 [min,max] 的结果
     */
    public static long clamp(long v, long min, long max) { if (min>max) throw new IllegalArgumentException("min>max"); return Math.max(min, Math.min(max, v)); }

    /**
     * 钳制 double 到区间
     * @param v 数值
     * @param min 最小值（含）
     * @param max 最大值（含）
     * @return 被限制到 [min,max] 的结果
     */
    public static double clamp(double v, double min, double max) { if (min>max) throw new IllegalArgumentException("min>max"); return Math.max(min, Math.min(max, v)); }

    /**
     * 保留小数位数进行四舍五入
     *
     * 基于 BigDecimal 的 HALF_UP 策略；scale 小于 0 时按 0 处理。
     * @param v 值
     * @param scale 小数位数（>=0）
     * @return 四舍五入后的值
     */
    public static double round(double v, int scale) { return BigDecimal.valueOf(v).setScale(Math.max(0, scale), RoundingMode.HALF_UP).doubleValue(); }

    /**
     * 按模式格式化数字（如 "#,##0.00"）
     *
     * 说明：依赖 {@link java.text.DecimalFormat}；分组与小数显示受模式控制。
     * @param v 值
     * @param pattern 格式化模式
     * @return 格式化文本
     */
    public static String formatDecimal(double v, String pattern) { return new DecimalFormat(pattern).format(v); }

    /**
     * 判断整数是否在闭区间内
     * @param v 值
     * @param min 最小值（含）
     * @param max 最大值（含）
     * @return 是否在 [min,max] 内
     */
    public static boolean isBetweenInclusive(int v, int min, int max) { if (min>max) throw new IllegalArgumentException("min>max"); return v>=min && v<=max; }

    /**
     * 判断长整型是否在闭区间内
     * @param v 值
     * @param min 最小值（含）
     * @param max 最大值（含）
     * @return 是否在 [min,max] 内
     */
    public static boolean isBetweenInclusive(long v, long min, long max) { if (min>max) throw new IllegalArgumentException("min>max"); return v>=min && v<=max; }

    /**
     * 判断浮点是否在闭区间内
     * @param v 值
     * @param min 最小值（含）
     * @param max 最大值（含）
     * @return 是否在 [min,max] 内
     */
    public static boolean isBetweenInclusive(double v, double min, double max) { if (min>max) throw new IllegalArgumentException("min>max"); return v>=min && v<=max; }

    /**
     * 生成区间内随机 double
     *
     * 基于 {@link Math#random()} 的非安全随机；适合一般业务，不适合安全用途。
     * @param minInclusive 最小值（含）
     * @param maxExclusive 最大值（不含）
     * @return 区间内随机数
     */
    public static double randomDouble(double minInclusive, double maxExclusive) { if (minInclusive>=maxExclusive) throw new IllegalArgumentException("min>=max"); return Math.random()*(maxExclusive-minInclusive)+minInclusive; }

    /**
     * 安全除法，除数为 0 返回默认值
     * @param a 被除数
     * @param b 除数
     * @param def 默认值（当 b==0 时返回）
     * @return a/b 或默认值
     */
    public static double safeDivide(double a, double b, double def) { return b==0.0?def:a/b; }

    /**
     * 求平均值（空或 null 返回 NaN）
     *
     * 空元素按 0 参与计算；列表为空或 null 返回 {@link Double#NaN}。
     * @param list 值列表
     * @return 平均值
     */
    public static double avg(List<Double> list) {
        if (list == null || list.isEmpty()) return Double.NaN;
        double sum = 0.0; for (Double d : list) sum += (d==null?0.0:d);
        return sum / list.size();
    }

    /**
     * 求中位数（空或 null 返回 NaN）
     *
     * 空元素按 0 参与排序；奇数个取中位，偶数个取中间两数平均。
     * @param list 值列表
     * @return 中位数
     */
    public static double median(List<Double> list) {
        if (list == null || list.isEmpty()) return Double.NaN;
        List<Double> copy = new java.util.ArrayList<>(); for (Double d : list) copy.add(d==null?0.0:d);
        Collections.sort(copy);
        int n = copy.size();
        if ((n & 1) == 1) return copy.get(n/2);
        return (copy.get(n/2-1) + copy.get(n/2)) / 2.0;
    }

    /**
     * 求和（null 视为 0）
     * @param list 值列表
     * @return 求和结果
     */
    public static double sum(List<Double> list) { if (list==null||list.isEmpty()) return 0.0; double s=0.0; for (Double d : list) s += (d==null?0.0:d); return s; }

    /**
     * 计算百分比（0-100），分母为 0 返回 0
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比值（分母为 0 返回 0）
     */
    public static double percent(double numerator, double denominator) { return denominator==0.0?0.0:(numerator/denominator)*100.0; }
}
