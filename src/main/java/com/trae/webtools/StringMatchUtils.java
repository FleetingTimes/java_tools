package com.trae.webtools;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * 字符串匹配判断工具（不与现有方法重复）
 *
 * 功能概览：
 * - 命名风格判断：kebab-case、snake_case、camelCase、PascalCase
 * - 内容特征判断：包含数字/字母、仅字母数字、全小写/全大写
 * - 近似相等判断：忽略空白、忽略大小写与空白
 * - 前后缀与包含：是否以任意前缀开头/任意后缀结尾；是否包含任意/全部关键词（忽略大小写）
 * - Unicode 与 ASCII：归一化为 NFC；移除重音与非 ASCII 字符得到基本 ASCII
 * - 正则辅助：计数匹配次数、拆分并保留分隔符、替换并保留分组
 *
 * 适用场景：
 * - 代码生成与规范校验：统一变量命名风格，检测字符串特征以满足规范。
 * - 文本处理与过滤：判断是否包含关键词、归一化 Unicode 后再进行比较（避免等价字符差异）。
 * - 正则操作辅助：统计匹配次数、保留分隔符拆分以进行二次处理、仅替换指定分组文本。
 */
public final class StringMatchUtils {
    private StringMatchUtils() {}

    /**
     * 是否为 kebab-case（小写字母数字，连字符分隔）
     *
     * @param s 输入文本
     * @return 是否匹配 kebab-case 规范（如 my-var-1）
     */
    public static boolean isKebabCase(String s) { return s!=null && s.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$"); }

    /**
     * 是否为 snake_case（小写字母数字，下划线分隔）
     *
     * @param s 输入文本
     * @return 是否匹配 snake_case 规范（如 my_var_1）
     */
    public static boolean isSnakeCase(String s) { return s!=null && s.matches("^[a-z0-9]+(?:_[a-z0-9]+)*$"); }

    /**
     * 是否为 camelCase（首段小写，后续以大写开头的段）
     *
     * @param s 输入文本
     * @return 是否匹配 camelCase 规范（如 myVar1）
     */
    public static boolean isCamelCase(String s) { return s!=null && s.matches("^[a-z]+(?:[A-Z][a-z0-9]+)*$"); }

    /**
     * 是否为 PascalCase（以大写开头的多段）
     *
     * @param s 输入文本
     * @return 是否匹配 PascalCase 规范（如 MyVar1）
     */
    public static boolean isPascalCase(String s) { return s!=null && s.matches("^[A-Z][a-z0-9]+(?:[A-Z][a-z0-9]+)*$"); }

    /**
     * 是否包含数字（至少一个）
     * @param s 输入文本
     * @return 是否包含数字字符
     */
    public static boolean hasDigits(String s) { return s!=null && s.matches(".*\\d+.*"); }

    /**
     * 是否包含字母（至少一个）
     * @param s 输入文本
     * @return 是否包含英文字母
     */
    public static boolean hasLetters(String s) { return s!=null && s.matches(".*[A-Za-z]+.*"); }

    /**
     * 是否仅含字母与数字
     * @param s 输入文本
     * @return 是否仅由 [A-Za-z0-9] 构成
     */
    public static boolean isAlphaNum(String s) { return s!=null && s.matches("^[A-Za-z0-9]+$"); }

    /**
     * 是否全小写（按 Locale.ROOT）
     * @param s 输入文本
     * @return 是否与其 toLowerCase(Locale.ROOT) 相等
     */
    public static boolean isLowerCase(String s) { return s!=null && s.equals(s.toLowerCase(Locale.ROOT)); }

    /**
     * 是否全大写（按 Locale.ROOT）
     * @param s 输入文本
     * @return 是否与其 toUpperCase(Locale.ROOT) 相等
     */
    public static boolean isUpperCase(String s) { return s!=null && s.equals(s.toUpperCase(Locale.ROOT)); }

    /**
     * 忽略空白判断相等（移除所有空白字符后再比较）
     * @param a 字符串 A（null 视为空串）
     * @param b 字符串 B（null 视为空串）
     * @return 是否相等
     */
    public static boolean equalsIgnoreWhitespace(String a, String b) { String aa=a==null?"":a.replaceAll("\\s+",""), bb=b==null?"":b.replaceAll("\\s+",""); return aa.equals(bb); }

    /**
     * 忽略大小写与空白判断相等（移除空白并转小写后比较）
     * @param a 字符串 A（null 视为空串）
     * @param b 字符串 B（null 视为空串）
     * @return 是否相等
     */
    public static boolean equalsIgnoreCaseAndWhitespace(String a, String b) { String aa=a==null?"":a.replaceAll("\\s+","").toLowerCase(Locale.ROOT); String bb=b==null?"":b.replaceAll("\\s+","").toLowerCase(Locale.ROOT); return aa.equals(bb); }

    /**
     * 是否以任意前缀开头（null 或空集合返回 false）
     * @param s 输入文本
     * @param prefixes 前缀列表（逐个按 startsWith 判断）
     * @return 是否至少匹配一个前缀
     */
    public static boolean startsWithAny(String s, List<String> prefixes) { if(s==null||prefixes==null) return false; for(String p:prefixes) if(p!=null && s.startsWith(p)) return true; return false; }

    /**
     * 是否以任意后缀结尾（null 或空集合返回 false）
     * @param s 输入文本
     * @param suffixes 后缀列表（逐个按 endsWith 判断）
     * @return 是否至少匹配一个后缀
     */
    public static boolean endsWithAny(String s, List<String> suffixes) { if(s==null||suffixes==null) return false; for(String p:suffixes) if(p!=null && s.endsWith(p)) return true; return false; }

    /**
     * 是否包含任意关键词（忽略大小写）
     * @param s 输入文本
     * @param parts 关键词列表
     * @return 是否至少包含一个关键词（大小写不敏感）
     */
    public static boolean containsAnyIgnoreCase(String s, List<String> parts) { if(s==null||parts==null) return false; String t=s.toLowerCase(Locale.ROOT); for(String p:parts){ if(p!=null && t.contains(p.toLowerCase(Locale.ROOT))) return true; } return false; }

    /**
     * 是否包含全部关键词（忽略大小写）
     * @param s 输入文本
     * @param parts 关键词列表
     * @return 是否全部包含（大小写不敏感）
     */
    public static boolean containsAllIgnoreCase(String s, List<String> parts) { if(s==null||parts==null) return false; String t=s.toLowerCase(Locale.ROOT); for(String p:parts){ if(p==null || !t.contains(p.toLowerCase(Locale.ROOT))) return false; } return true; }

    /**
     * 统一 Unicode 为 NFC 正规化（组合字符规范至预组形式）
     * @param s 输入文本
     * @return 规范化后的文本（null 返回 null）
     */
    public static String normalizeUnicodeNFC(String s) { return s==null?null:Normalizer.normalize(s, Normalizer.Form.NFC); }

    /**
     * 尝试转 ASCII（移除重音与非 ASCII 字符；使用 NFD 先拆解组合）
     * @param s 输入文本
     * @return 仅包含基本 ASCII 的文本（null 返回 null）
     */
    public static String toAsciiBasic(String s) { if(s==null) return null; String nfd=Normalizer.normalize(s, Normalizer.Form.NFD); String t=nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", ""); return t.replaceAll("[^\\x00-\\x7F]", ""); }

    /** 统计正则匹配次数（m.find() 的出现次数） */
    public static int countRegexMatches(String s, String regex) { if(s==null) return 0; java.util.regex.Matcher m=java.util.regex.Pattern.compile(regex).matcher(s); int c=0; while(m.find()) c++; return c; }

    /** 正则拆分并保留分隔符（返回拆分片段与分隔符交错数组） */
    public static String[] splitKeepingDelimiters(String s, String regex) { if(s==null) return new String[0]; java.util.List<String> out=new java.util.ArrayList<>(); java.util.regex.Pattern p=java.util.regex.Pattern.compile(regex); java.util.regex.Matcher m=p.matcher(s); int last=0; while(m.find()){ out.add(s.substring(last, m.start())); out.add(m.group()); last=m.end(); } out.add(s.substring(last)); return out.toArray(new String[0]); }

    /** 使用正则替换并保留分组（示例：将第 1 组替换为指定文本） */
    public static String replaceAllGroup(String s, String regex, int groupIndex, String repl) { java.util.regex.Pattern p=java.util.regex.Pattern.compile(regex); java.util.regex.Matcher m=p.matcher(s==null?"":s); StringBuffer sb=new StringBuffer(); while(m.find()){ String g=m.group(groupIndex); m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(repl==null?"":repl)); } m.appendTail(sb); return sb.toString(); }
}
