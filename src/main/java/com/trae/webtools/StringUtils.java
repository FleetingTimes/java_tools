package com.trae.webtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 字符串处理与转义工具
 *
 * 包含：空白判断、修剪默认、连接拆分、子串、空白归一化、HTML/JSON 转义、基础清理。
 */
public final class StringUtils {
    private StringUtils() {}

    /** 判断字符串是否为 null 或仅空白 */
    public static boolean isBlank(String s) {
        if (s == null) return true;
        for (int i = 0; i < s.length(); i++) if (!Character.isWhitespace(s.charAt(i))) return false;
        return true;
    }

    /** 判断字符串是否为 null 或长度为 0 */
    public static boolean isEmpty(String s) { return s == null || s.length() == 0; }

    /** 判断字符串是否非空白 */
    public static boolean notBlank(String s) { return !isBlank(s); }

    /** 安全相等比较（允许 null） */
    public static boolean safeEquals(String a, String b) { return java.util.Objects.equals(a, b); }

    /** 去除首尾空白，若结果为空则返回 null */
    public static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 当 s 为 null 时返回默认值 */
    public static String defaultString(String s, String def) { return s == null ? def : s; }

    /** 将集合或数组用分隔符连接为单一字符串 */
    public static String join(Iterable<?> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object p : parts) {
            if (!first) sb.append(sep);
            sb.append(p);
            first = false;
        }
        return sb.toString();
    }

    /** 按分隔符拆分为列表（忽略空结果） */
    public static List<String> split(String s, String sep) {
        if (isEmpty(s)) return Collections.emptyList();
        String[] arr = s.split(sep);
        List<String> out = new ArrayList<>(arr.length);
        for (String it : arr) if (!it.isEmpty()) out.add(it);
        return out;
    }

    /** 获取分隔符之前的子串（找不到返回原串） */
    public static String substringBefore(String s, String sep) {
        if (s == null || sep == null) return s;
        int idx = s.indexOf(sep);
        return idx < 0 ? s : s.substring(0, idx);
    }

    /** 获取分隔符之后的子串（找不到返回空串） */
    public static String substringAfter(String s, String sep) {
        if (s == null || sep == null) return "";
        int idx = s.indexOf(sep);
        return idx < 0 ? "" : s.substring(idx + sep.length());
    }

    /** 归一化空白（多空白合并为单空格，修剪首尾） */
    public static String normalizeWhitespace(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ");
    }

    /** HTML 转义（常见符号） */
    public static String htmlEscape(String s) {
        if (s == null) return null;
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** JSON 简易转义（引号与反斜杠） */
    public static String jsonEscape(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** 非法脚本简单清理（移除 <script>...） */
    public static String sanitizeHtmlBasic(String s) {
        if (s == null) return null;
        return s.replaceAll("(?i)<script[^>]*>.*?</script>", "");
    }

    /** 忽略大小写判断前缀 */
    public static boolean startsWithIgnoreCase(String s, String prefix) {
        if (s == null || prefix == null) return false;
        return s.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }

    /** 忽略大小写判断后缀 */
    public static boolean endsWithIgnoreCase(String s, String suffix) {
        if (s == null || suffix == null) return false;
        return s.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT));
    }

    /** 忽略大小写包含判断 */
    public static boolean containsIgnoreCase(String s, String part) {
        if (s == null || part == null) return false;
        return s.toLowerCase(Locale.ROOT).contains(part.toLowerCase(Locale.ROOT));
    }

    /** 左侧填充到指定长度 */
    public static String leftPad(String s, int len, char pad) {
        if (s == null) s = "";
        if (s.length() >= len) return s;
        StringBuilder sb = new StringBuilder(len);
        for (int i = s.length(); i < len; i++) sb.append(pad);
        sb.append(s);
        return sb.toString();
    }

    /** 右侧填充到指定长度 */
    public static String rightPad(String s, int len, char pad) {
        if (s == null) s = "";
        if (s.length() >= len) return s;
        StringBuilder sb = new StringBuilder(len);
        sb.append(s);
        for (int i = s.length(); i < len; i++) sb.append(pad);
        return sb.toString();
    }

    /** 重复字符串指定次数 */
    public static String repeat(String s, int times) {
        if (times <= 0) return "";
        StringBuilder sb = new StringBuilder(s == null ? 0 : s.length() * times);
        for (int i = 0; i < times; i++) sb.append(s);
        return sb.toString();
    }

    /** 首字母大写（其余不变） */
    public static String capitalize(String s) {
        if (isBlank(s)) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** 首字母小写（其余不变） */
    public static String decapitalize(String s) {
        if (isBlank(s)) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /** snake_case 转 camelCase */
    public static String snakeToCamel(String s) {
        if (isBlank(s)) return s;
        StringBuilder sb = new StringBuilder(s.length());
        boolean up = false;
        for (char c : s.toCharArray()) {
            if (c == '_' || c == '-') up = true;
            else {
                sb.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        return sb.toString();
    }

    /** camelCase 转 snake_case */
    public static String camelToSnake(String s) {
        if (isBlank(s)) return s;
        StringBuilder sb = new StringBuilder(s.length()+8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) { sb.append('_').append(Character.toLowerCase(c)); }
            else sb.append(c);
        }
        return sb.toString();
    }

    /** 删除所有出现的目标子串（字面量） */
    public static String remove(String s, String target) { return s == null ? null : s.replace(target, ""); }

    /** 仅替换第一次出现（字面量） */
    public static String replaceFirst(String s, String target, String replacement) {
        if (s == null || target == null) return s;
        int idx = s.indexOf(target);
        return idx < 0 ? s : s.substring(0, idx) + replacement + s.substring(idx + target.length());
    }

    /** 替换所有出现（字面量） */
    public static String replaceAllLiteral(String s, String target, String replacement) { return s == null ? null : s.replace(target, replacement); }

    /** 超长截断并追加尾部标记（如 "..."） */
    public static String truncate(String s, int max, String tail) {
        if (s == null) return null;
        if (max <= 0) return tail == null ? "" : tail;
        if (s.length() <= max) return s;
        String t = tail == null ? "" : tail;
        int keep = Math.max(0, max - t.length());
        return s.substring(0, keep) + t;
    }
}
