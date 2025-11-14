package com.trae.webtools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 字符串增强工具（不与现有方法重复）
 *
 * 提供标题大小写、拆分行、拼接行、前后缀处理、计数与查找、截取与省略、正则转义、
 * 片段插入/移除/替换、去引号、缩进/去缩进、大小写风格转换等。
 */
public final class StringPlusUtils {
    private StringPlusUtils() {}

    /** 标题大小写：单词首字母大写，其他小写 */
    public static String toTitleCase(String s) {
        if (StringUtils.isBlank(s)) return s;
        String[] parts = StringUtils.normalizeWhitespace(s).toLowerCase(Locale.ROOT).split(" ");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) parts[i] = Character.toUpperCase(p.charAt(0)) + p.substring(1);
        }
        return String.join(" ", parts);
    }

    /** 分割为行列表（保留空行） */
    public static List<String> splitLines(String s) { return s == null ? Collections.emptyList() : Arrays.asList(s.split("\r?\n", -1)); }

    /** 将行列表拼接为文本，以换行符连接 */
    public static String joinLines(List<String> lines) { return lines == null ? "" : String.join("\n", lines); }

    /** 确保字符串前缀（若不存在则追加） */
    public static String ensurePrefix(String s, String prefix) { if (s == null) return prefix; return s.startsWith(prefix) ? s : prefix + s; }

    /** 确保字符串后缀（若不存在则追加） */
    public static String ensureSuffix(String s, String suffix) { if (s == null) return suffix; return s.endsWith(suffix) ? s : s + suffix; }

    /** 移除前缀（若存在） */
    public static String removePrefix(String s, String prefix) { return (s != null && prefix != null && s.startsWith(prefix)) ? s.substring(prefix.length()) : s; }

    /** 移除后缀（若存在） */
    public static String removeSuffix(String s, String suffix) { return (s != null && suffix != null && s.endsWith(suffix)) ? s.substring(0, s.length()-suffix.length()) : s; }

    /** 统计子串出现次数（允许重叠） */
    public static int countOccurrences(String s, String sub) {
        if (StringUtils.isBlank(s) || StringUtils.isEmpty(sub)) return 0;
        int c=0; for (int i = 0; i+sub.length() <= s.length(); i++) if (s.startsWith(sub, i)) c++; return c;
    }

    /** 查找第 n 次出现的索引（1-based），未找到返回 -1 */
    public static int indexOfNth(String s, String sub, int n) {
        if (StringUtils.isBlank(s) || StringUtils.isEmpty(sub) || n <= 0) return -1;
        int idx=-1; int start=0; for(int i=0;i<n;i++){ idx=s.indexOf(sub,start); if(idx<0) return -1; start=idx+sub.length(); }
        return idx;
    }

    /** 获取两个分隔符之间的子串（找不到任一分隔符返回空串） */
    public static String substringBetween(String s, String left, String right) {
        if (s == null || left == null || right == null) return "";
        int l = s.indexOf(left); if (l < 0) return "";
        int r = s.indexOf(right, l + left.length()); if (r < 0) return "";
        return s.substring(l + left.length(), r);
    }

    /** 安全截取：索引越界时自动钳制 */
    public static String safeSubstring(String s, int start, int end) {
        if (s == null) return null; int n=s.length(); start=Math.max(0,Math.min(n,start)); end=Math.max(0,Math.min(n,end)); if(start>end){ int t=start; start=end; end=t; } return s.substring(start,end);
    }

    /** 中间省略（保留首尾各半长度） */
    public static String abbreviateMiddle(String s, int max, String mark) {
        if (s == null) return null; if (max <= 0) return mark == null ? "" : mark;
        if (s.length() <= max) return s; String m = mark == null ? "..." : mark; int keep = Math.max(2, max - m.length()); int left = keep / 2; int right = keep - left; return s.substring(0,left) + m + s.substring(s.length()-right);
    }

    /** 正则字面量转义 */
    public static String escapeRegex(String s) { return s == null ? null : s.replaceAll("([\\\\.\\^\\$\\|\\(\\)\\[\\]\\{\\}\\*\\+\\?])", "\\\\$1"); }

    /** 去除包裹的单引号或双引号 */
    public static String stripQuotes(String s) { if (s == null || s.length() < 2) return s; char a=s.charAt(0), b=s.charAt(s.length()-1); if ((a=='"'&&b=='"')||(a=='\''&&b=='\'')) return s.substring(1,s.length()-1); return s; }

    /** 在指定位置插入片段（越界时追加到末尾） */
    public static String insertAt(String s, int index, String fragment) { if (s==null) return fragment; index=Math.max(0,Math.min(s.length(),index)); return s.substring(0,index) + (fragment==null?"":fragment) + s.substring(index); }

    /** 删除指定区间 [start,end) 的内容（越界钳制） */
    public static String removeRange(String s, int start, int end) { if (s==null) return null; int n=s.length(); start=Math.max(0,Math.min(n,start)); end=Math.max(0,Math.min(n,end)); if(start>end){ int t=start; start=end; end=t; } return s.substring(0,start) + s.substring(end); }

    /** 替换首个匹配区间 [start,end) 为片段 */
    public static String replaceRange(String s, int start, int end, String fragment) { return insertAt(removeRange(s,start,end), start, fragment); }

    /** 统一换行符为 \n */
    public static String normalizeNewlines(String s) { return s == null ? null : s.replace("\r\n","\n").replace("\r","\n"); }

    /** 文本缩进（为每行加上 prefix） */
    public static String indent(String s, String prefix) {
        List<String> lines = splitLines(s); List<String> out = new ArrayList<>(lines.size()); for(String l:lines) out.add((prefix==null?"":prefix)+l); return joinLines(out);
    }

    /** 文本去缩进（移除每行最前面的 prefix，若存在） */
    public static String dedent(String s, String prefix) {
        if (prefix == null || prefix.isEmpty()) return s; List<String> lines = splitLines(s); List<String> out = new ArrayList<>(lines.size()); for(String l:lines) out.add(removePrefix(l,prefix)); return joinLines(out);
    }

    /** 转为 kebab-case（中划线分隔） */
    public static String toKebabCase(String s) {
        if (StringUtils.isBlank(s)) return s; String t = s.trim().replace(' ','-'); t = StringUtils.camelToSnake(t).replace('_','-'); return t.toLowerCase(Locale.ROOT);
    }

    /** 转为 SCREAMING_SNAKE_CASE（大写下划线） */
    public static String toSnakeUpper(String s) { if (StringUtils.isBlank(s)) return s; return StringUtils.camelToSnake(s).toUpperCase(Locale.ROOT); }

    /** 转为 PascalCase（首字母也大写） */
    public static String toPascalCase(String s) { if (StringUtils.isBlank(s)) return s; String c = StringUtils.snakeToCamel(s); return StringUtils.capitalize(c); }

    /** 反转行顺序 */
    public static String reverseLines(String s) { List<String> lines = splitLines(s); Collections.reverse(lines); return joinLines(lines); }

    /** 判断是否包含任意关键词 */
    public static boolean containsAny(String s, List<String> parts) { if (StringUtils.isBlank(s)||parts==null) return false; for(String p:parts) if(p!=null && !p.isEmpty() && s.contains(p)) return true; return false; }

    /** 判断是否包含全部关键词 */
    public static boolean containsAll(String s, List<String> parts) { if (StringUtils.isBlank(s)||parts==null) return false; for(String p:parts) if(p==null || p.isEmpty() || !s.contains(p)) return false; return true; }

    /** 将每个字符重复指定次数 */
    public static String repeatEachChar(String s, int times) { if(times<=0||s==null) return ""; StringBuilder sb=new StringBuilder(s.length()*times); for(char c:s.toCharArray()) for(int i=0;i<times;i++) sb.append(c); return sb.toString(); }

    /** 在每两个字符之间插入分隔符 */
    public static String intersperse(String s, String sep) { if(s==null) return null; StringBuilder sb=new StringBuilder(s.length()*2); for(int i=0;i<s.length();i++){ if(i>0) sb.append(sep); sb.append(s.charAt(i)); } return sb.toString(); }

    /** 按最大长度分割为片段列表（最后一段可能更短） */
    public static List<String> partitionByLength(String s, int maxLen) {
        List<String> out = new ArrayList<>(); if (s==null||maxLen<=0) return out; for(int i=0;i<s.length();i+=maxLen) out.add(s.substring(i, Math.min(s.length(), i+maxLen))); return out;
    }

    /** 居中省略：保留两侧固定长度 */
    public static String ellipsisCenter(String s, int leftKeep, int rightKeep, String mark) {
        if (s==null) return null; if (leftKeep<0||rightKeep<0) return s; if (s.length()<=leftKeep+rightKeep) return s; String m=mark==null?"...":mark; return s.substring(0,leftKeep)+m+s.substring(s.length()-rightKeep);
    }

    /** 反转词语顺序（按空白分词） */
    public static String reverseWords(String s) { if (StringUtils.isBlank(s)) return s; List<String> words = Arrays.asList(StringUtils.normalizeWhitespace(s).split(" ")); Collections.reverse(words); return String.join(" ", words); }

    /** 移除所有空白字符（含换行与制表） */
    public static String removeAllWhitespace(String s) { return s == null ? null : s.replaceAll("\\s+", ""); }

    /** 仅保留数字字符 */
    public static String onlyDigits(String s) { return s == null ? null : s.replaceAll("[^0-9]", ""); }

    /** 仅保留字母与数字 */
    public static String onlyAlphaNum(String s) { return s == null ? null : s.replaceAll("[^A-Za-z0-9]", ""); }

    /** 将多个分隔符合并为一个（保留分隔符本身） */
    public static String collapseSeparators(String s, String sepRegex) { return s == null ? null : s.replaceAll("("+sepRegex+")+", "$1"); }

    /** 用分隔符包裹字符串（如前后加引号） */
    public static String wrapWith(String s, String left, String right) { return (left==null?"":left) + (s==null?"":s) + (right==null?"":right); }

    /** 去除左右包裹（若存在），否则返回原串 */
    public static String unwrap(String s, String left, String right) { if (s==null||left==null||right==null) return s; return (s.startsWith(left)&&s.endsWith(right)) ? s.substring(left.length(), s.length()-right.length()) : s; }
}

