package com.trae.webtools;

import java.util.*;

/**
 * 极简 JSON 与 Properties 相互转换
 *
 * 工具定位：
 * - 面向简单配置与参数传递的轻量实现，完成 Map 与 JSON（平面对象）之间的互转，以及 Properties 与 Map 的互转。
 * - JSON 解析与序列化仅支持平面 k:v 结构（字符串/数字/布尔/null），不支持数组与嵌套对象；适合快速、受控的场景。
 *
 * 行为与限制：
 * - 序列化：字符串与键会进行基本转义；数字/布尔按原样输出；null 输出为字面量 null；遍历顺序遵循传入 Map 的迭代顺序（如 LinkedHashMap）。
 * - 解析：仅识别顶层键值对（以逗号分隔），支持字符串中的转义与逗号；数字含小数点或科学记号（e/E）时按 Double 解析，否则按 Long 解析；无法解析的值保留原文本。
 * - 安全：仅作格式转换，未进行内容安全过滤；如需用于 HTML/SQL/日志等场景，请在上游进行必要的转义与校验。
 */
public final class JsonPropsUtils {
    private JsonPropsUtils() {}

    /**
     * Map -> JSON（仅支持字符串/数字/布尔/null）
     *
     * 键与字符串值会进行基本 JSON 转义；数值与布尔按原样输出；null 输出为字面量 "null"。
     * 迭代顺序遵循传入 Map 的迭代顺序（如 LinkedHashMap 可保持插入顺序）。
     * @param map 键值映射（值仅支持字符串/数字/布尔/null）
     * @return 平面 JSON 文本（如 {"k":"v","n":123,"b":true}）
     */
    public static String jsonSerializeMap(Map<String, ?> map) {
        if (map == null) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(StringUtils.jsonEscape(e.getKey())).append('"').append(':');
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(String.valueOf(v));
            else sb.append('"').append(StringUtils.jsonEscape(String.valueOf(v))).append('"');
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * JSON -> Map（不支持嵌套，仅 k:v 平面）
     *
     * 解析顶层平面对象，字符串中的转义与逗号会被正确处理；值解析规则：
     * - "null" -> null；"true"/"false" -> Boolean；
     * - 含小数点或 e/E -> Double；否则尝试 Long；失败则保留原文本字符串。
     * @param json 平面 JSON 文本（如 {"k":"v","n":123}）
     * @return 键值映射（LinkedHashMap，保持解析顺序）
     * @throws IllegalArgumentException 非合法的平面对象文本（不以 { 开始或不以 } 结束）
     */
    public static Map<String, Object> jsonParseToMap(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (StringUtils.isBlank(json)) return map;
        String t = json.trim();
        if (!t.startsWith("{") || !t.endsWith("}")) throw new IllegalArgumentException("invalid json");
        t = t.substring(1, t.length()-1).trim();
        if (t.isEmpty()) return map;
        List<String> parts = splitJsonTopLevel(t);
        for (String part : parts) {
            int idx = part.indexOf(':');
            if (idx < 0) continue;
            String k = unquote(part.substring(0, idx).trim());
            String v = part.substring(idx + 1).trim();
            map.put(k, parseJsonValue(v));
        }
        return map;
    }

    /**
     * 按顶层逗号分割 JSON 对象体
     *
     * 在字符串边界内保留逗号与转义，确保不会在字符串内部错误拆分。
     * @param s 去除大括号后的对象体文本
     * @return 顶层键值对片段列表（仍包含冒号与引号）
     */
    private static List<String> splitJsonTopLevel(String s) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false; boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                cur.append(c);
                if (esc) esc = false;
                else if (c == '\\') esc = true;
                else if (c == '"') inStr = false;
            } else {
                if (c == '"') { inStr = true; cur.append(c); }
                else if (c == ',') { parts.add(cur.toString().trim()); cur.setLength(0); }
                else cur.append(c);
            }
        }
        if (cur.length() > 0) parts.add(cur.toString().trim());
        return parts;
    }

    /**
     * 解析 JSON 值为 Java 对象
     *
     * 规则：null/true/false、字符串（双引号包裹）、数字（含小数点或 e/E 解析为 Double，否则 Long），失败则保留原文本。
     * @param v 值片段文本
     * @return 解析后的对象（null/Boolean/Number/String）
     */
    private static Object parseJsonValue(String v) {
        if (v.equals("null")) return null;
        if (v.equals("true")) return Boolean.TRUE;
        if (v.equals("false")) return Boolean.FALSE;
        if (v.startsWith("\"") && v.endsWith("\"")) return unquote(v);
        try { if (v.contains(".") || v.contains("e") || v.contains("E")) return Double.parseDouble(v); else return Long.parseLong(v); } catch (Exception ignore) {}
        return v;
    }

    /**
     * 去引号并反转义字符串
     *
     * 输入为可能带双引号的文本；移除首尾双引号并处理常见转义（\" 与 \\）。
     * @param s 文本（可能带引号）
     * @return 去引号与反转义后的纯文本
     */
    private static String unquote(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length()-1);
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    /**
     * Properties -> Map
     *
     * 将 {@link java.util.Properties} 转换为保持遍历顺序的 LinkedHashMap。
     * @param p Properties 对象
     * @return 键值映射（LinkedHashMap）
     */
    public static Map<String, String> propertiesToMap(java.util.Properties p) {
        Map<String, String> m = new LinkedHashMap<>();
        if (p == null) return m;
        for (String n : p.stringPropertyNames()) m.put(n, p.getProperty(n));
        return m;
    }

    /**
     * Map -> Properties
     *
     * 将字符串 Map 转换为 {@link java.util.Properties}，键和值均按原样设置。
     * @param m 键值映射
     * @return Properties 对象
     */
    public static java.util.Properties mapToProperties(Map<String, String> m) {
        java.util.Properties p = new java.util.Properties();
        if (m != null) for (Map.Entry<String, String> e : m.entrySet()) p.setProperty(e.getKey(), e.getValue());
        return p;
    }
}
