package com.trae.webtools;

import java.text.Normalizer;
import java.util.*;

/**
 * 文本度量与处理工具（不依赖外部库）
 *
 * 功能概览：
 * - 编辑距离：Levenshtein、Damerau-Levenshtein（支持相邻转置）、Hamming（等长字符串）
 * - 相似度：Jaccard、Dice 系数、词频向量余弦相似度、Overlap 系数
 * - 公共子序列/子串：LCS（最长公共子序列）与最长公共子串长度
 * - 文本操作：公共前缀/后缀、回文判断、反转、分词、词数/字符数统计、规范化比较
 * - 规范化与生成：去重音（Unicode NFD）、slug 化（小写、连字符、移除非法字符）、n-gram 生成
 *
 * 适用场景与建议：
 * - 去重与匹配：用距离/相似度度量进行近似匹配（如用户输入纠错、记录去重）；不同度量适用于不同场景（如 Jaccard 面向集合、余弦面向频次）。
 * - 文本清洗与索引：去重音、标准化空白与大小写、生成 slug 与 n-gram，为搜索或路由友好化提供辅助。
 * - 复杂度考量：大多数 DP 算法复杂度为 O(m*n)，在极长字符串上需谨慎；余弦相似度需构建词频向量，键集合大小影响性能。
 */
public final class TextMetricsUtils {
    private TextMetricsUtils() {}

    /**
     * 计算 Levenshtein 编辑距离（插入/删除/替换成本均为 1）
     * @param a 源字符串（null 视为空串）
     * @param b 目标字符串（null 视为空串）
     * @return 编辑距离（非负整数）
     * 算法：经典 DP；dp[i][j] 表示 a[0..i) 与 b[0..j) 的最小编辑代价
     */
    public static int levenshteinDistance(String a, String b) {
        if (a == null) a = ""; if (b == null) b = "";
        int m = a.length(), n = b.length();
        int[][] dp = new int[m+1][n+1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            char ca = a.charAt(i-1);
            for (int j = 1; j <= n; j++) {
                char cb = b.charAt(j-1);
                int cost = (ca == cb) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1), dp[i-1][j-1] + cost);
            }
        }
        return dp[m][n];
    }

    /**
     * 计算 Damerau-Levenshtein 距离（支持相邻转置）
     * @param a 源字符串（null 视为空串）
     * @param b 目标字符串（null 视为空串）
     * @return 编辑距离（非负整数）
     * 说明：在 Levenshtein 的基础上加入一次相邻转置的代价 1。
     */
    public static int damerauLevenshteinDistance(String a, String b) {
        if (a == null) a = ""; if (b == null) b = "";
        int m = a.length(), n = b.length();
        int[][] dp = new int[m+1][n+1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i-1) == b.charAt(j-1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1), dp[i-1][j-1] + cost);
                if (i > 1 && j > 1 && a.charAt(i-1) == b.charAt(j-2) && a.charAt(i-2) == b.charAt(j-1)) {
                    dp[i][j] = Math.min(dp[i][j], dp[i-2][j-2] + 1);
                }
            }
        }
        return dp[m][n];
    }

    /**
     * 计算 Hamming 距离（字符串长度必须相同）
     * @param a 字符串 A
     * @param b 字符串 B（长度必须与 A 相同）
     * @return 不同位置的计数
     * @throws IllegalArgumentException 长度不相等或为空
     */
    public static int hammingDistance(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) throw new IllegalArgumentException("length mismatch");
        int d = 0; for (int i = 0; i < a.length(); i++) if (a.charAt(i) != b.charAt(i)) d++; return d;
    }

    /**
     * 根据空白分词并计算 Jaccard 相似度（0-1）
     * @param a 文本 A（按空白分词，先进行空白归一化）
     * @param b 文本 B（按空白分词，先进行空白归一化）
     * @return Jaccard(A,B) = |A∩B| / |A∪B|（若两者均空则返回 1.0）
     */
    public static double jaccardSimilarityTokens(String a, String b) {
        Set<String> A = new LinkedHashSet<>(StringUtils.split(StringUtils.normalizeWhitespace(a), "\\s+"));
        Set<String> B = new LinkedHashSet<>(StringUtils.split(StringUtils.normalizeWhitespace(b), "\\s+"));
        if (A.isEmpty() && B.isEmpty()) return 1.0;
        Set<String> inter = new LinkedHashSet<>(A); inter.retainAll(B);
        Set<String> union = new LinkedHashSet<>(A); union.addAll(B);
        return union.isEmpty() ? 0.0 : inter.size() / (double) union.size();
    }

    /**
     * 根据空白分词并计算 Dice 系数（0-1）
     * @param a 文本 A
     * @param b 文本 B
     * @return Dice(A,B) = 2|A∩B| / (|A| + |B|)（若两者均空返回 1.0）
     */
    public static double diceCoefficientTokens(String a, String b) {
        Set<String> A = new LinkedHashSet<>(StringUtils.split(StringUtils.normalizeWhitespace(a), "\\s+"));
        Set<String> B = new LinkedHashSet<>(StringUtils.split(StringUtils.normalizeWhitespace(b), "\\s+"));
        if (A.isEmpty() && B.isEmpty()) return 1.0;
        Set<String> inter = new LinkedHashSet<>(A); inter.retainAll(B);
        return (2.0 * inter.size()) / (A.size() + B.size());
    }

    /**
     * 计算基于词频向量的余弦相似度（0-1）
     * @param a 文本 A（按空白分词构建词频向量）
     * @param b 文本 B（按空白分词构建词频向量）
     * @return 余弦相似度（若任一向量长度为 0 则返回 0.0）
     */
    public static double cosineSimilarityTokens(String a, String b) {
        Map<String,Integer> A = termFreq(StringUtils.split(StringUtils.normalizeWhitespace(a), "\\s+"));
        Map<String,Integer> B = termFreq(StringUtils.split(StringUtils.normalizeWhitespace(b), "\\s+"));
        Set<String> keys = new LinkedHashSet<>(); keys.addAll(A.keySet()); keys.addAll(B.keySet());
        double dot = 0, na = 0, nb = 0;
        for (String k : keys) { int va = A.getOrDefault(k,0), vb = B.getOrDefault(k,0); dot += va*vb; na += va*va; nb += vb*vb; }
        return (na==0 || nb==0) ? 0.0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static Map<String,Integer> termFreq(List<String> tokens) {
        Map<String,Integer> m = new LinkedHashMap<>();
        for (String t : tokens) if (!StringUtils.isBlank(t)) m.put(t, m.getOrDefault(t, 0) + 1);
        return m;
    }

    /**
     * 计算最长公共前缀
     * @param a 字符串 A
     * @param b 字符串 B
     * @return 共同前缀（任一为 null 返回空串）
     */
    public static String commonPrefix(String a, String b) {
        if (a == null || b == null) return "";
        int len = Math.min(a.length(), b.length());
        int i = 0; while (i < len && a.charAt(i) == b.charAt(i)) i++;
        return a.substring(0, i);
    }

    /**
     * 计算最长公共后缀
     * @param a 字符串 A
     * @param b 字符串 B
     * @return 共同后缀（任一为 null 返回空串）
     */
    public static String commonSuffix(String a, String b) {
        if (a == null || b == null) return "";
        int ia = a.length()-1, ib = b.length()-1;
        int cnt = 0; while (ia>=0 && ib>=0 && a.charAt(ia)==b.charAt(ib)) { ia--; ib--; cnt++; }
        return a.substring(a.length()-cnt);
    }

    /**
     * 判断是否回文（忽略大小写与非字母数字）
     * @param s 输入文本
     * @return 是否为回文
     */
    public static boolean isPalindrome(String s) {
        if (s == null) return false;
        String t = s.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        int i=0,j=t.length()-1; while (i<j) { if (t.charAt(i)!=t.charAt(j)) return false; i++; j--; }
        return true;
    }

    /** 统计词数（按空白分割，先归一化空白） */
    public static int wordCount(String s) { return StringUtils.isBlank(s) ? 0 : StringUtils.split(StringUtils.normalizeWhitespace(s), "\\s+").size(); }

    /** 统计字符数（包括空白；基于 char 长度，非 code point） */
    public static int charCount(String s) { return s == null ? 0 : s.length(); }

    /**
     * 去除重音与附加符号（Unicode NFD）
     * @param s 输入文本
     * @return 去除组合附加符号后的文本
     */
    public static String removeAccents(String s) {
        if (s == null) return null;
        String nfd = Normalizer.normalize(s, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * 生成 slug（小写、空白转连字符、去重音、移除非法字符）
     * @param s 输入文本
     * @return 适合 URL/路径的 slug（仅含 [a-z0-9-]）
     */
    public static String slugify(String s) {
        if (StringUtils.isBlank(s)) return "";
        String t = removeAccents(s.toLowerCase(Locale.ROOT)).replaceAll("[^a-z0-9\\s-]", "");
        t = t.trim().replaceAll("[\\s-]+", "-");
        return t;
    }

    /**
     * 生成白空分词 n-gram（n>=1）
     * @param s 输入文本
     * @param n 词窗口大小（>=1）
     * @return 连续词组列表（空文本或词数不足返回空列表）
     */
    public static List<String> ngramTokens(String s, int n) {
        List<String> tokens = StringUtils.split(StringUtils.normalizeWhitespace(s), "\\s+");
        List<String> out = new ArrayList<>();
        for (int i = 0; i + n <= tokens.size(); i++) {
            out.add(String.join(" ", tokens.subList(i, i+n)));
        }
        return out;
    }

    /**
     * 最长公共子序列长度（LCS）
     * @param a 字符串 A
     * @param b 字符串 B
     * @return LCS 长度
     * 复杂度：O(m*n) 时间，O(m*n) 空间
     */
    public static int longestCommonSubsequenceLength(String a, String b) {
        if (a == null) a = ""; if (b == null) b = "";
        int m = a.length(), n = b.length();
        int[][] dp = new int[m+1][n+1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i-1) == b.charAt(j-1)) dp[i][j] = dp[i-1][j-1] + 1;
                else dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
            }
        }
        return dp[m][n];
    }

    /**
     * 最长公共子串长度（连续）
     * @param a 字符串 A
     * @param b 字符串 B
     * @return 最长公共连续子串长度
     * 复杂度：O(m*n) 时间，O(m*n) 空间
     */
    public static int longestCommonSubstringLength(String a, String b) {
        if (a == null) a = ""; if (b == null) b = "";
        int m = a.length(), n = b.length();
        int[][] dp = new int[m+1][n+1];
        int best = 0;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i-1) == b.charAt(j-1)) { dp[i][j] = dp[i-1][j-1]+1; best = Math.max(best, dp[i][j]); }
                else dp[i][j] = 0;
            }
        }
        return best;
    }

    /**
     * Overlap 系数（交集大小除以较小集合大小）
     * @param a 文本 A（空白分词）
     * @param b 文本 B（空白分词）
     * @return Overlap(A,B) = |A∩B| / min(|A|,|B|)
     */
    public static double overlapCoefficientTokens(String a, String b) {
        Set<String> A = new LinkedHashSet<>(StringUtils.split(StringUtils.normalizeWhitespace(a), "\\s+"));
        Set<String> B = new LinkedHashSet<>(StringUtils.split(StringUtils.normalizeWhitespace(b), "\\s+"));
        if (A.isEmpty() && B.isEmpty()) return 1.0;
        Set<String> inter = new LinkedHashSet<>(A); inter.retainAll(B);
        int denom = Math.min(A.size(), B.size());
        return denom == 0 ? 0.0 : inter.size() / (double) denom;
    }

    /** 基于空白的简单分词（忽略空 token；先归一化空白） */
    public static List<String> tokenizeWhitespace(String s) { return StringUtils.split(StringUtils.normalizeWhitespace(s), "\\s+"); }

    /** 为比较标准化（修剪、合并空白、转小写） */
    public static String normalizeForComparison(String s) { return StringUtils.isBlank(s)?"":StringUtils.normalizeWhitespace(s).toLowerCase(Locale.ROOT); }

    /** 反转字符串（null 返回 null） */
    public static String reverseString(String s) { return s == null ? null : new StringBuilder(s).reverse().toString(); }
}
