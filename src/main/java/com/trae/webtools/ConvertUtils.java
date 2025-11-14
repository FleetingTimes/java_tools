package com.trae.webtools;

import java.util.Locale;

/**
 * 字符串到数值/布尔的安全解析
 */
public final class ConvertUtils {
    private ConvertUtils() {}

    /** 安全解析 int，失败返回默认值 */
    public static int safeParseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    /** 安全解析 long，失败返回默认值 */
    public static long safeParseLong(String s, long def) {
        if (s == null) return def;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    /** 安全解析 double，失败返回默认值 */
    public static double safeParseDouble(String s, double def) {
        if (s == null) return def;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }

    /** 宽松转换布尔（"true", "1", "yes", "y" 为 true） */
    public static boolean toBoolean(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase(Locale.ROOT);
        return t.equals("true") || t.equals("1") || t.equals("yes") || t.equals("y");
    }
}

