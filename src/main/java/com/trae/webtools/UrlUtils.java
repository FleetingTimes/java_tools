package com.trae.webtools;

import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.util.*;

/**
 * URL 工具：解析/构造、路径与查询参数处理、绝对/相对判断
 *
 * 功能概览：
 * - 解析与构造：按组件进行分解与组装（scheme、host、port、path、query、fragment）
 * - 路径操作：路径段切分、连接、规范化（移除点与双点）、前后斜杠处理
 * - 查询参数：添加/替换/移除/获取单值参数；多值参数的构造与解析
 * - URL 判定：绝对与相对 URL 的判断；路径编码/解码（保留分隔符）
 *
 * 使用建议：
 * - 仅对路径片段进行编码，保留斜杠分隔符以避免改变路径结构；查询参数需单独编码键与值。
 * - 构造 URL 时避免重复斜杠与遗漏斜杠；必要时使用 ensureLeadingSlash/TrailingSlash。
 * - 端口缺省时不建议手动添加，以免与默认端口规则冲突；跨环境时应谨慎处理端口与主机名。
 * - fragment（片段）不参与服务端资源定位，通常由客户端使用；构造时可选添加。
 */
public final class UrlUtils {
    private UrlUtils() {}

    /**
     * 解析 URL 为组件 Map（scheme、host、port、path、query、fragment）
     * @param url 完整 URL 文本
     * @return 组件映射（可能包含 null 值），解析失败返回空映射或包含部分信息
     */
    public static Map<String,String> parseUrlComponents(String url) {
        Map<String,String> m = new LinkedHashMap<>();
        try {
            URI u = new URI(url);
            m.put("scheme", u.getScheme());
            m.put("host", u.getHost());
            m.put("port", u.getPort() < 0 ? null : String.valueOf(u.getPort()));
            m.put("path", u.getRawPath());
            m.put("query", u.getRawQuery());
            m.put("fragment", u.getRawFragment());
        } catch (Exception ignore) {}
        return m;
    }

    /**
     * 根据组件构造 URL（缺省不包含）
     * @param c 组件映射（scheme、host、port、path、query、fragment）
     * @return 组装后的 URL 文本
     */
    public static String buildUrlFromComponents(Map<String,String> c) {
        String scheme = c.get("scheme"); String host = c.get("host"); String port = c.get("port"); String path = c.get("path"); String query = c.get("query"); String fragment = c.get("fragment");
        StringBuilder sb = new StringBuilder();
        if (scheme != null) sb.append(scheme).append("://");
        if (host != null) sb.append(host);
        if (port != null) sb.append(":").append(port);
        if (path != null) sb.append(path.startsWith("/")?path:"/"+path);
        if (query != null) sb.append("?").append(query);
        if (fragment != null) sb.append("#").append(fragment);
        return sb.toString();
    }

    /**
     * 获取路径段列表（忽略空段）
     * @param path 原始路径（可包含多重斜杠与相对符号）
     * @return 分段列表（不含空字符串）
     */
    public static List<String> getPathSegments(String path) {
        if (StringUtils.isBlank(path)) return Collections.emptyList();
        String p = IOUtils.normalizePath(path);
        if (p.startsWith("/")) p = p.substring(1);
        if (p.isEmpty()) return Collections.emptyList();
        return Arrays.asList(p.split("/"));
    }

    /**
     * 连接路径段为路径
     * @param parts 路径段（忽略空或空白）
     * @return 以斜杠分隔的路径（至少为根 "/"）
     */
    public static String joinPathSegments(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String s : parts) { if (StringUtils.isBlank(s)) continue; sb.append('/').append(s); }
        return sb.length()==0?"/":sb.toString();
    }

    /**
     * 规范化路径段（去除 "." 与 ".."，保留根）
     * @param path 原始路径
     * @return 规范化后的路径（不包含冗余相对段）
     */
    public static String normalizePathSegments(String path) {
        Deque<String> stack = new ArrayDeque<>();
        for (String seg : getPathSegments(path)) {
            if (seg.equals(".")) continue;
            if (seg.equals("..")) { if(!stack.isEmpty()) stack.removeLast(); }
            else stack.addLast(seg);
        }
        return joinPathSegments(new ArrayList<>(stack));
    }

    /**
     * 添加或替换查询参数
     * @param url 原始 URL
     * @param key 参数名
     * @param value 参数值（null 视为空串）
     * @return 新 URL（保留其他参数与片段）
     */
    public static String addOrReplaceQueryParam(String url, String key, String value) {
        int q = url.indexOf('?'); String base = q<0?url:url.substring(0,q); String qs = q<0?"":url.substring(q+1);
        Map<String,String> m = HttpUtils.parseQueryStringToMap(qs);
        m.put(key, value==null?"":value);
        String built = HttpUtils.buildQueryString(m);
        return built.isEmpty()?base:base+"?"+built;
    }

    /** 移除查询参数 */
    public static String removeQueryParam(String url, String key) { return HttpUtils.removeQueryParam(url, key); }

    /** 获取查询参数 */
    public static String getQueryParam(String url, String key) { return HttpUtils.getQueryParam(url, key); }

    /** 确保尾部有斜杠 */
    public static String ensureTrailingSlash(String url) { return StringUtils.isBlank(url)?url:(url.endsWith("/")?url:url+"/"); }

    /** 移除尾部斜杠 */
    public static String removeTrailingSlash(String url) { return StringUtils.isBlank(url)?url:(url.endsWith("/")?url.substring(0,url.length()-1):url); }

    /** 确保前部有斜杠 */
    public static String ensureLeadingSlash(String path) { return StringUtils.isBlank(path)?path:(path.startsWith("/")?path:"/"+path); }

    /** 移除前部斜杠 */
    public static String removeLeadingSlash(String path) { return StringUtils.isBlank(path)?path:(path.startsWith("/")?path.substring(1):path); }

    /** 是否为绝对 URL */
    public static boolean isAbsoluteUrl(String url) { try { URI u = new URI(url); return u.getScheme()!=null; } catch (Exception e) { return false; } }

    /** 是否为相对 URL */
    public static boolean isRelativeUrl(String url) { return !isAbsoluteUrl(url); }

    /**
     * 对路径进行 URL 编码（保留斜杠）
     *
     * 说明：仅编码路径片段中的保留字符与非 ASCII；分隔符 "/" 原样保留，避免路径结构变化。
     * @param path 原始路径
     * @return 编码后的路径
     */
    public static String encodePath(String path) {
        if (path==null) return null; String[] parts = path.split("/"); StringBuilder sb=new StringBuilder(); boolean first=true;
        for (String p : parts) { if(!first) sb.append('/'); try{ sb.append(URLEncoder.encode(p, "UTF-8")); }catch(Exception e){ throw new RuntimeException(e);} first=false; }
        return sb.toString();
    }

    /**
     * 对路径进行 URL 解码
     * @param path 编码后的路径
     * @return 解码后的路径（保留分隔符）
     */
    public static String decodePath(String path) {
        if (path==null) return null; String[] parts = path.split("/"); StringBuilder sb=new StringBuilder(); boolean first=true;
        for (String p : parts) { if(!first) sb.append('/'); try{ sb.append(URLDecoder.decode(p, "UTF-8")); }catch(Exception e){ throw new RuntimeException(e);} first=false; }
        return sb.toString();
    }

    /**
     * 多值查询参数构造：Map<String,List<String>> -> k=v1&k=v2
     * @param params 多值参数映射
     * @return 查询字符串（已对键值进行 URL 编码）
     */
    public static String buildQueryFromMultiMap(Map<String,List<String>> params) {
        if(params==null||params.isEmpty()) return ""; StringBuilder sb=new StringBuilder(); boolean first=true;
        for(Map.Entry<String,List<String>> e:params.entrySet()){
            String k=e.getKey(); List<String> vs=e.getValue(); if(vs==null||vs.isEmpty()) continue;
            for(String v:vs){ if(!first) sb.append('&'); sb.append(HttpUtils.urlEncode(k)).append('=').append(HttpUtils.urlEncode(v==null?"":v)); first=false; }
        }
        return sb.toString();
    }

    /**
     * 解析多值查询参数：k=v1&k=v2 -> Map<String,List<String>>
     * @param qs 查询字符串
     * @return 多值参数映射（保留顺序）
     */
    public static Map<String,List<String>> parseQueryToMultiMap(String qs) {
        Map<String,List<String>> m=new LinkedHashMap<>(); if(StringUtils.isBlank(qs)) return m;
        String[] pairs=qs.split("&");
        for(String p:pairs){ int idx=p.indexOf('='); String k=idx>=0?p.substring(0,idx):p; String v=idx>=0?p.substring(idx+1):""; k=HttpUtils.urlDecode(k); v=HttpUtils.urlDecode(v); m.computeIfAbsent(k, x->new ArrayList<>()).add(v); }
        return m;
    }
}
