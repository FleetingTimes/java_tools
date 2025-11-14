package com.trae.webtools;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * 随机工具：采样、打乱、区间随机、字符串/字节/十六进制/Base64、选择等
 *
 * 工具定位：
 * - 使用 {@link java.security.SecureRandom} 提供安全级（不可预测）随机数，适合安全相关场景（令牌、盐、密钥片段）。
 * - 提供集合采样与打乱、数值/日期区间随机、文本随机生成与选择等常用方法。
 *
 * 使用建议：
 * - 安全需求：令牌/盐/密钥等请使用 {@link #randomBytes(int)}、{@link #randomHex(int)}、{@link #randomBase64(int)} 等；不要使用非安全随机。
 * - 采样与打乱：对大集合的采样/打乱会涉及 O(n) 时间与空间，注意内存与性能；超大集合建议使用流式/水塘采样。
 * - 区间随机：整型/长整型的取模方式存在轻微偏差（当跨度不是 2 的幂次），一般业务可接受；安全场景需更严格方法。
 */
public final class RandomUtils {
    private RandomUtils() {}

    private static final SecureRandom RAND = new SecureRandom();

    /**
     * 高斯分布随机数（均值 0，标准差 1）
     * 适用场景：模拟噪声、评分扰动、正态分布试验。
     * @return N(0,1) 随机数
     */
    public static double randomGaussian() { return RAND.nextGaussian(); }

    /**
     * 按权重返回 true（概率 p ∈ [0,1]）
     * 适用场景：抽样判定、AB 测试比例、按概率开启特性。
     * @param p 概率（小于等于 0 一律为 false；大于等于 1 一律为 true）
     * @return 随机布尔值（按权重）
     */
    public static boolean randomBooleanWeighted(double p) { if(p<=0) return false; if(p>=1) return true; return RAND.nextDouble() < p; }

    /**
     * 从闭区间采样指定数量不重复整数
     * 适用场景：抽签编号、分页索引采样、试验对照组划分。
     * 复杂度：O(n) 构建与打乱，返回前 count 个；当区间很大时注意性能与内存。
     * @param minInclusive 最小值（含）
     * @param maxInclusive 最大值（含）
     * @param count 采样数量（不可超过区间大小）
     * @return 不重复整数列表
     */
    public static List<Integer> sampleIntRange(int minInclusive, int maxInclusive, int count) {
        if (minInclusive>maxInclusive || count<0 || count>(maxInclusive-minInclusive+1)) throw new IllegalArgumentException("bad range/count");
        List<Integer> pool = new ArrayList<>(); for(int i=minInclusive;i<=maxInclusive;i++) pool.add(i);
        Collections.shuffle(pool, RAND); return new ArrayList<>(pool.subList(0, count));
    }

    /**
     * 从列表中采样指定数量不重复元素
     * 适用场景：抽样数据集、生成部分样本进行校验/展示。
     * @param list 源列表（不可为 null）
     * @param count 采样数量（不可超过列表大小）
     * @return 不重复元素列表
     */
    public static <T> List<T> sampleList(List<T> list, int count) {
        if (list==null || count<0 || count>list.size()) throw new IllegalArgumentException("bad list/count");
        List<T> copy = new ArrayList<>(list); Collections.shuffle(copy, RAND); return new ArrayList<>(copy.subList(0, count));
    }

    /**
     * 打乱列表（原地）
     * 适用场景：题目/数据随机化、顺序扰动。
     * @param list 列表（为 null 则忽略）
     */
    public static <T> void shuffleList(List<T> list) { if(list!=null) Collections.shuffle(list, RAND); }

    /**
     * 从字典随机生成字符串
     * 适用场景：验证码/邀请码/随机标识（需避免歧义字符可自定义字典）。
     * @param dict 字典（非空）
     * @param len 长度（>=0）
     * @return 随机字符串
     */
    public static String randomStringFromDict(String dict, int len) {
        if (dict==null || dict.isEmpty() || len<0) throw new IllegalArgumentException("bad args"); StringBuilder sb=new StringBuilder(len);
        for(int i=0;i<len;i++) sb.append(dict.charAt(RAND.nextInt(dict.length()))); return sb.toString();
    }

    /**
     * 随机字节数组
     * 适用场景：令牌、盐、IV、随机填充等安全用途。
     * @param len 字节长度
     * @return 随机字节数组（安全随机）
     */
    public static byte[] randomBytes(int len) { byte[] b=new byte[len]; RAND.nextBytes(b); return b; }

    /**
     * 随机十六进制字符串（小写）
     * 适用场景：人类可读的随机标识或日志输出。
     * @param bytesLen 随机字节长度
     * @return 十六进制文本
     */
    public static String randomHex(int bytesLen) { return SecurityUtils.bytesToHex(randomBytes(bytesLen)); }

    /**
     * 随机 Base64 文本
     * 适用场景：传输友好的随机标识或令牌；非 URL 安全（可能包含 + / =）。
     * @param bytesLen 随机字节长度
     * @return Base64 文本
     */
    public static String randomBase64(int bytesLen) { return SecurityUtils.base64Encode(randomBytes(bytesLen)); }

    /**
     * 随机 UUID（无连字符）
     * 适用场景：紧凑表示的 UUID 标识（日志/URL 友好）。
     * @return 随机 UUID 的 32 位十六进制文本（去除连字符）
     */
    public static String randomUUIDNoHyphen() { return java.util.UUID.randomUUID().toString().replace("-", ""); }

    /**
     * 生成不包含指定集合的随机整数（闭区间）
     * 适用场景：避开黑名单索引、随机端口筛选（示例）。
     * 行为说明：若尝试次数超过两倍区间跨度仍未命中可用数则抛异常。
     * @param minInclusive 最小值（含）
     * @param maxInclusive 最大值（含）
     * @param exclude 排除集合（可为 null）
     * @return 不在排除集合的随机整数
     */
    public static int randomIntExcluding(int minInclusive, int maxInclusive, Set<Integer> exclude) {
        if (minInclusive>maxInclusive) throw new IllegalArgumentException("min>max");
        int tries = 0; int span = maxInclusive - minInclusive + 1;
        while (tries < span*2) {
            int v = RAND.nextInt(span) + minInclusive;
            if (exclude==null || !exclude.contains(v)) return v;
            tries++;
        }
        throw new IllegalStateException("no available number");
    }

    /**
     * 随机长整型在区间 [min,max)
     * 适用场景：ID 段、时间窗口内的随机偏移等。
     * 注意：取模方式在跨度非 2 的幂次时存在轻微偏差，一般业务可接受。
     * @param minInclusive 最小值（含）
     * @param maxExclusive 最大值（不含）
     * @return 区间内随机 long
     */
    public static long randomLongRange(long minInclusive, long maxExclusive) { if(minInclusive>=maxExclusive) throw new IllegalArgumentException("min>=max"); long span=maxExclusive-minInclusive; long r=(RAND.nextLong() & Long.MAX_VALUE)%span; return minInclusive + r; }

    /**
     * 随机浮点型在区间 [min,max)
     * 适用场景：浮点权重随机化、位置/颜色扰动等。
     * @param minInclusive 最小值（含）
     * @param maxExclusive 最大值（不含）
     * @return 区间内随机 double
     */
    public static double randomDoubleRange(double minInclusive, double maxExclusive) { if(minInclusive>=maxExclusive) throw new IllegalArgumentException("min>=max"); return minInclusive + RAND.nextDouble()*(maxExclusive-minInclusive); }

    /**
     * 从列表中随机选一个元素（可能为 null）
     * 适用场景：随机推荐、随机抽取一个候选。
     * @param list 列表（为空或 null 返回 null）
     * @return 随机元素或 null
     */
    public static <T> T pickOne(List<T> list) { if(list==null||list.isEmpty()) return null; return list.get(RAND.nextInt(list.size())); }

    /**
     * 从列表中随机选 n 个互不相同元素（不足抛异常）
     * 适用场景：随机试题/样本选择。
     * @param list 列表
     * @param n 个数（不可超过列表大小）
     * @return 不重复列表
     */
    public static <T> List<T> pickDistinct(List<T> list, int n) { return sampleList(list, n); }

    /**
     * 抛硬币（true 表示正面）
     * 适用场景：二元随机决策。
     * @return 随机布尔值
     */
    public static boolean coinFlip() { return RAND.nextBoolean(); }

    /**
     * 掷骰子（返回 1-6）
     * 适用场景：游戏随机、演示与试验。
     * @return 1-6 的随机整数
     */
    public static int diceRoll() { return RAND.nextInt(6) + 1; }

    /**
     * 在两个日期之间随机选择时间戳（毫秒）
     * 适用场景：生成伪随机时间点、测试数据。
     * @param startMillis 起始毫秒（含）
     * @param endMillis 结束毫秒（不含）
     * @return 区间内随机毫秒时间戳
     */
    public static long randomDateBetween(long startMillis, long endMillis) { if(startMillis>=endMillis) throw new IllegalArgumentException("start>=end"); long span=endMillis-startMillis; long r=(RAND.nextLong() & Long.MAX_VALUE)%span; return startMillis + r; }
}
