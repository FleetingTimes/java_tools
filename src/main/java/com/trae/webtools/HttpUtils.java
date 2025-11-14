package com.trae.webtools;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 查询参数、Cookie 构造与解析
 */
public final class HttpUtils {
    private HttpUtils() {}

    /** URL 编码（UTF-8） */
    public static String urlEncode(String s) { try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { throw new RuntimeException(e); } }

    /** URL 解码（UTF-8） */
    public static String urlDecode(String s) { try { return URLDecoder.decode(s, "UTF-8"); } catch (Exception e) { throw new RuntimeException(e); } }

    /** 将查询字符串解析为 Map（k=v&k2=v2） */
    public static Map<String, String> parseQueryStringToMap(String qs) {
        Map<String, String> map = new LinkedHashMap<>();
        if (StringUtils.isBlank(qs)) return map;
        String[] pairs = qs.split("&");
        for (String p : pairs) {
            int idx = p.indexOf('=');
            String k = idx >= 0 ? p.substring(0, idx) : p;
            String v = idx >= 0 ? p.substring(idx + 1) : "";
            map.put(urlDecode(k), urlDecode(v));
        }
        return map;
    }

    /** 构造查询字符串（Map -> k=v&...） */
    public static String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append('&');
            sb.append(urlEncode(e.getKey()));
            sb.append('=');
            sb.append(urlEncode(e.getValue() == null ? "" : e.getValue()));
            first = false;
        }
        return sb.toString();
    }

    /** 解析 Cookie 文本为 Map */
    public static Map<String, String> parseCookies(String cookieHeader) {
        Map<String, String> map = new LinkedHashMap<>();
        if (StringUtils.isBlank(cookieHeader)) return map;
        String[] parts = cookieHeader.split(";\\s*");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx < 0) continue;
            map.put(part.substring(0, idx).trim(), part.substring(idx + 1).trim());
        }
        return map;
    }

    /** 构造 Set-Cookie（基础版） */
    public static String buildSetCookie(String name, String value, String path, String domain, int maxAgeSeconds, boolean httpOnly, boolean secure) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append('=').append(value);
        if (!StringUtils.isBlank(path)) sb.append("; Path=").append(path);
        if (!StringUtils.isBlank(domain)) sb.append("; Domain=").append(domain);
        if (maxAgeSeconds >= 0) sb.append("; Max-Age=").append(maxAgeSeconds);
        if (secure) sb.append("; Secure");
        if (httpOnly) sb.append("; HttpOnly");
        return sb.toString();
    }

    /** 规范化 HTTP 头名称（如 content-type -> Content-Type） */
    public static String normalizeHeaderName(String name) {
        if (StringUtils.isBlank(name)) return name;
        String[] parts = name.toLowerCase(java.util.Locale.ROOT).split("-");
        for (int i = 0; i < parts.length; i++) parts[i] = StringUtils.capitalize(parts[i]);
        return String.join("-", parts);
    }

    /** 解析 application/x-www-form-urlencoded 文本为 Map */
    public static Map<String,String> parseFormUrlEncoded(String body) { return parseQueryStringToMap(body); }

    /** 构造 application/x-www-form-urlencoded 文本 */
    public static String buildFormUrlEncoded(Map<String,String> params) { return buildQueryString(params); }

    /** 在 URL 中添加查询参数 */
    public static String addQueryParam(String url, String key, String value) {
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + urlEncode(key) + "=" + urlEncode(value==null?"":value);
    }

    /** 从 URL 中移除指定查询参数（简单实现） */
    public static String removeQueryParam(String url, String key) {
        int q = url.indexOf('?'); if (q < 0) return url;
        String base = url.substring(0, q);
        Map<String,String> m = parseQueryStringToMap(url.substring(q+1));
        m.remove(key);
        String qs = buildQueryString(m);
        return qs.isEmpty()?base:base+"?"+qs;
    }

    /** 获取 URL 中的指定查询参数 */
    public static String getQueryParam(String url, String key) {
        int q = url.indexOf('?'); if (q < 0) return null;
        Map<String,String> m = parseQueryStringToMap(url.substring(q+1));
        return m.get(key);
    }

    /** 连接 URL 路径（处理斜杠） */
    public static String pathJoinUrl(String base, String path) {
        if (StringUtils.isBlank(base)) return path;
        if (StringUtils.isBlank(path)) return base;
        String b = IOUtils.normalizePath(base);
        String p = IOUtils.normalizePath(path);
        if (b.endsWith("/")) b = b.substring(0, b.length()-1);
        if (!p.startsWith("/")) p = "/" + p;
        return b + p;
    }

    /** 构造带查询参数的 URL */
    public static String buildUrl(String base, Map<String,String> params) {
        if (params == null || params.isEmpty()) return base;
        String qs = buildQueryString(params);
        return base + (base.contains("?")?"&":"?") + qs;
    }

    /** 判断 URL 是否为 HTTPS */
    public static boolean isHttps(String url) { return url != null && url.toLowerCase(java.util.Locale.ROOT).startsWith("https://"); }
}
