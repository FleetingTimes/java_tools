package com.trae.webtools;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Web 协议增强工具：内容协商、ETag、缓存、CORS、Range、Trace 等
 *
 * 功能概览：
 * - 内容协商：解析 Accept（带权重）并在支持列表中选择最优类型
 * - ETag：构造弱 ETag（W/"hash"），适合语义上允许弱比较的场景
 * - 安全判断：HTTPS/代理标记判定安全请求；安全重定向校验避免开放式重定向
 * - 路径与分页：URL 路径清理，分页链接构造（first/last/prev/next）
 * - 请求与语言：Ajax 检测、语言标签规范化、基础 UA 解析、同源判断、CORS 允许判断
 * - Range：解析与构造字节范围头
 * - 追踪：Trace-Id 头获取，多名称兼容；缓存键构造（稳定顺序）
 * - 缓存：no-store/no-cache/must-revalidate 的复合指令构造
 *
 * 使用建议与注意：
 * - isSecureRequest 依赖代理头，需结合可信代理链使用；不可信环境可能被伪造
 * - safeRedirectUrl 仅允许相对路径或同源绝对路径，避免开放式重定向导致钓鱼风险
 * - negotiateContentType 简化匹配（支持全通配与类型通配，如任意类型、type/*），复杂优先级可结合 q 权重与通配细化
 * - Range 仅支持单段解析；多段场景需结合更完整解析器与响应拼装
 *
 * 术语与概念（简明介绍）：
 * - 内容协商（Content Negotiation）：
 *   客户端通过 Accept、Accept-Language 等头声明可接受的表示与偏好（权重 q）。服务端在支持的媒体类型中选择最匹配的一种，最终由响应头 Content-Type 确认。常见匹配维度包括精确类型与类型通配（如某主类型下任意子类型）。
 * - ETag（实体标签）：
 *   标识资源版本的令牌，用于条件请求与缓存校验。强标签要求字节级完全一致；弱标签以前缀 W 表示（示例：W\"abc\"），用于语义等价但字节可能不同的场景。配合 If-None-Match（缓存验证，命中返回 304）与 If-Match（并发写保护，不匹配返回 412）。
 * - 缓存（Cache-Control）：
 *   控制客户端/中间层的缓存行为的指令集合，如 max-age、public/private、no-cache、no-store、must-revalidate。强制缓存与协商缓存结合 ETag 或 Last-Modified 提升带宽利用与响应时延。
 * - CORS（跨域资源共享）：
 *   通过响应头允许跨站点访问资源。核心头包括 Access-Control-Allow-Origin、Allow-Methods、Allow-Headers、Allow-Credentials、Max-Age。涉及预检请求（OPTIONS）与安全策略（凭据与白名单）。
 * - Range（部分内容）：
 *   允许客户端请求资源的部分字节段，常用于断点续传与媒体流。成功部分响应使用状态码 206 并携带 Content-Range 表示范围与总长度；无法满足范围时返回 416。
 * - Trace（请求追踪）：
 *   通过 Trace-Id 或 Request-Id 等标识关联不同系统与日志中的同一请求流，便于排错与性能分析。常用头名包括 trace-id、x-request-id、x-correlation-id。
 *
 * 进一步详解：
 * - 内容协商：
 *   1) 匹配优先级：更具体的类型优先（type/subtype > 类型通配 > 全通配）；
 *   2) 权重处理：Accept 中带 q 的条目按权重降序；权重相同按具体度与出现顺序；
 *   3) 常见陷阱：客户端发送 Accept 过宽导致返回非期望类型；未考虑 Vary 头导致代理缓存错误共享；
 *   4) 建议：明确声明支持列表与顺序；必要时返回 406 表示不可接受；为语言/压缩等差异设置 Vary（如 Vary: Accept-Encoding, Accept-Language）。
 * - ETag：
 *   1) 强/弱选择：字节级一致用强标签；模板渲染但语义等价用弱标签；
 *   2) 条件请求：If-None-Match 命中（含 \"*\" 或匹配标签）返回 304 并不含实体；If-Match 不命中返回 412 以保护并发写；
 *   3) 生成策略：可基于内容摘要（SHA-256）、版本号或数据库行版本；字节不同但等价时建议弱标签；
 *   4) 变体：压缩、语言、用户定制等会改变实体，应结合 Vary 或为不同变体生成不同标签；
 *   5) 陷阱：忘记加引号、弱标签误用于 Range 校验、跨变体共享导致误命中。
 * - 缓存：
 *   1) 指令含义：max-age（秒）、public/private（是否可中间缓存）、no-cache（需再验证）、no-store（禁止存储）、must-revalidate（过期后必须再验证）；
 *   2) 强制缓存：在有效期内直接使用；协商缓存：过期后携带 If-None-Match/If-Modified-Since 验证；
 *   3) 代理与共享：public 允许共享代理缓存；private 限制仅浏览器缓存；
 *   4) 建议：对静态资源设置较长 max-age 与内容指纹；对易变资源使用协商缓存与合理的 no-cache/must-revalidate；
 *   5) 陷阱：混淆 no-cache 与 no-store；忽略 Vary 导致缓存污染；未考虑 Authorization 时的缓存策略。
 * - CORS：
 *   1) 预检流程：跨域非简单请求先发 OPTIONS，服务端返回允许的方法/头与缓存时长（Max-Age）；
 *   2) 凭据：Allow-Credentials=true 时 Origin 不能为通配，需明确回显；
 *   3) 安全：仅对受信任 Origin 放行；避免反射所有请求头；严格限制方法与自定义头；
 *   4) 建议：维护白名单，必要时动态校验；与 CSRF、防护策略协同。
 * - Range：
 *   1) 请求：Range: bytes=start-end 或 bytes=start-（至末尾）；
 *   2) 响应：206 状态，Content-Range: bytes start-end/total，并设置 Accept-Ranges: bytes；
 *   3) 不可满足：返回 416，并提供 Content-Range: bytes [*]/total 以声明总长度；
 *   4) 建议：校验范围合法性与资源长度，支持断点续传；多段（multipart/byteranges）需自行拼装。
 * - Trace：
 *   1) 标识传播：入口生成请求标识并贯穿所有下游调用与日志；
 *   2) 兼容名称：trace-id、x-request-id、x-correlation-id 等；
 *   3) 建议：若未提供则生成；确保在错误与慢日志中输出；结合采样与隐私策略。
 */
public final class WebExtraUtils {
    private WebExtraUtils() {}

    /**
     * 构造 Content-Disposition（RFC 5987）用于下载文件
     *
     * 兼容策略：
     * - filename 提供 ASCII 回退（替换双引号为下划线）
     * - filename* 使用 RFC 5987 的 UTF-8 编码并将空格编码为 %20（避免 "+" 互通问题）
     * @param filename 文件名
     * @return 头值，如：attachment; filename="abc.txt"; filename*=UTF-8''abc.txt
     */
    public static String buildContentDisposition(String filename) {
        String fallback = filename.replace('"','_');
        String encoded;
        try { encoded = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { throw new RuntimeException(e); }
        return "attachment; filename=\""+fallback+"\"; filename*=UTF-8''"+encoded;
    }

    /**
     * 解析 Accept 头为带权重的类型列表（type;q=weight）
     * @param header Accept 头
     * @return 按 q 权重降序的类型项列表
     */
    public static List<AcceptItem> parseAcceptWeighted(String header) {
        List<AcceptItem> out = new ArrayList<>(); if (StringUtils.isBlank(header)) return out;
        for (String part : header.split(",")) {
            String[] kv = part.trim().split(";q="); String type = kv[0].trim(); double q = kv.length>1 ? ConvertUtils.safeParseDouble(kv[1], 1.0) : 1.0;
            out.add(new AcceptItem(type, q));
        }
        out.sort((a,b)->Double.compare(b.q,a.q));
        return out;
    }

    /**
     * 根据 Accept 进行内容协商，返回最匹配的类型
     *
     * 匹配规则：
     * - 完全匹配（type/subtype）优先
     * - 通配匹配：全通配（任意类型）或类型通配（type/*）
     * - 若无匹配返回支持列表首项（或 null）
     * @param header Accept 头
     * @param supported 服务器支持的类型列表（如 application/json,text/html）
     * @return 选定类型（可能为 null）
     */
    public static String negotiateContentType(String header, List<String> supported) {
        List<AcceptItem> items = parseAcceptWeighted(header);
        for (AcceptItem ai : items) {
            for (String s : supported) {
                if (ai.type.equals("*/*") || s.equalsIgnoreCase(ai.type)) return s;
                String base = StringUtils.substringBefore(s, "/");
                if (ai.type.equalsIgnoreCase(base+"/*")) return s;
            }
        }
        return supported.isEmpty()?null:supported.get(0);
    }

    /**
     * 构造弱 ETag（W/"hash"）
     *
     * 弱 ETag：允许在弱比较下认为“语义等价”（如仅时间戳不同）；不用于 Range 校验。
     * @param content 文本内容（UTF-8）
     * @return 弱 ETag 文本，如 W"<sha256>"
     */
    public static String buildETagWeak(String content) { String h = SecurityUtils.sha256Hex((content==null?"":content).getBytes(StandardCharsets.UTF_8)); return "W\""+h+"\""; }

    /**
     * 判断是否为安全请求（HTTPS 或经代理标记）
     *
     * 说明：依赖 X-Forwarded-Proto/X-Forwarded-Ssl/Request-URL 等头部；需在可信代理链中使用。
     * @param headers 头集合
     * @return 是否可视为安全请求
     */
    public static boolean isSecureRequest(Map<String,String> headers) {
        if (headers==null) return false;
        String proto = headers.get("X-Forwarded-Proto");
        if (proto != null) return proto.equalsIgnoreCase("https");
        String tls = headers.get("X-Forwarded-Ssl");
        if (tls != null) return tls.equalsIgnoreCase("on");
        String url = headers.get("Request-URL");
        return url != null && url.toLowerCase(Locale.ROOT).startsWith("https://");
    }

    /**
     * 清理 URL 路径（移除 .. 与多重斜杠）
     * @param path 原始路径
     * @return 规范化后的路径
     */
    public static String sanitizeUrlPath(String path) { return UrlUtils.normalizePathSegments(path); }

    /**
     * 构造分页链接（prev,next,first,last）
     * @param baseUrl 基础 URL（不含 page 参数）
     * @param page 当前页码（从 1 开始）
     * @param size 每页大小
     * @param total 总记录数
     * @return 链接映射（first/last/prev/next）
     */
    public static Map<String,String> buildPaginationLinks(String baseUrl, int page, int size, int total) {
        int pages = (int)Math.ceil(Math.max(0, total)/(double)Math.max(1,size)); page = Math.min(Math.max(1,page), Math.max(pages,1));
        Map<String,String> m = new LinkedHashMap<>();
        m.put("first", HttpUtils.addQueryParam(baseUrl, "page", "1"));
        m.put("last", HttpUtils.addQueryParam(baseUrl, "page", String.valueOf(Math.max(1,pages))));
        if (page>1) m.put("prev", HttpUtils.addQueryParam(baseUrl, "page", String.valueOf(page-1)));
        if (page<pages) m.put("next", HttpUtils.addQueryParam(baseUrl, "page", String.valueOf(page+1)));
        return m;
    }

    /**
     * 是否为 Ajax 请求（X-Requested-With=XMLHttpRequest）
     * @param headers 头集合
     * @return 是否为 Ajax 请求
     */
    public static boolean isAjaxRequest(Map<String,String> headers) { String x = headers==null?null:headers.get("X-Requested-With"); return x!=null && x.equals("XMLHttpRequest"); }

    /**
     * 规范化语言标签（全部小写，区域大写，如 zh-CN）
     * @param tag 语言标签（如 zh-cn、en-us）
     * @return 规范化标签（如 zh-CN、en-US）
     */
    public static String normalizeLanguageTag(String tag) {
        if (StringUtils.isBlank(tag)) return tag; String[] p = tag.split("-"); if(p.length==1) return p[0].toLowerCase(Locale.ROOT);
        return p[0].toLowerCase(Locale.ROOT)+"-"+p[1].toUpperCase(Locale.ROOT);
    }

    /**
     * 基础 UA 解析（返回浏览器名）
     * @param ua User-Agent 文本
     * @return 浏览器名称（CHROME/FIREFOX/SAFARI/IE/OTHER）
     */
    public static String parseUserAgentBasic(String ua) {
        if (StringUtils.isBlank(ua)) return "UNKNOWN"; String t=ua.toLowerCase(Locale.ROOT);
        if (t.contains("chrome")) return "CHROME"; if (t.contains("firefox")) return "FIREFOX"; if (t.contains("safari")) return "SAFARI"; if (t.contains("msie")||t.contains("trident")) return "IE"; return "OTHER";
    }

    /**
     * 是否推荐缓存响应（GET 且 2xx）
     * @param method HTTP 方法
     * @param status 响应状态码
     * @return 是否建议缓存（策略为 GET 且成功状态）
     */
    public static boolean shouldCacheResponse(String method, int status) { return "GET".equalsIgnoreCase(method) && status>=200 && status<300; }

    /**
     * 安全重定向地址（阻止开放式重定向，仅允许相对或同源）
     * @param target 目标地址
     * @param currentOrigin 当前站点 Origin（如 https://example.com）
     * @return 是否安全（相对 URL 或与 currentOrigin 同源的绝对 URL）
     */
    public static boolean safeRedirectUrl(String target, String currentOrigin) {
        if (StringUtils.isBlank(target)) return false;
        if (UrlUtils.isRelativeUrl(target)) return true;
        try { java.net.URI t = new java.net.URI(target); return (currentOrigin!=null && target.startsWith(currentOrigin)); } catch (Exception e) { return false; }
    }

    /**
     * 解析 Range 头（bytes=start-end），返回起止（不支持多段）
     * @param range Range 头值（如 bytes=0-499、bytes=500-）
     * @return [start,end]（不可解析返回 null；end 为 -1 表示未指定）
     */
    public static int[] parseRangeHeader(String range) {
        if (StringUtils.isBlank(range) || !range.startsWith("bytes=")) return null;
        String[] se = range.substring(6).split("-");
        int start = ConvertUtils.safeParseInt(se[0], -1);
        int end = se.length>1?ConvertUtils.safeParseInt(se[1], -1):-1;
        return new int[]{start,end};
    }

    /**
     * 构造 Range 头（bytes=start-end）
     * @param start 起始字节（>=0）
     * @param end 结束字节（<0 表示未指定）
     * @return Range 头值
     */
    public static String buildRangeHeader(int start, int end) { return "bytes="+Math.max(0,start)+"-"+(end<0?"":String.valueOf(end)); }

    /**
     * 判断是否同源（依据 Origin 或 Referer）
     * @param headers 头集合
     * @param origin 期望的源（协议+主机+端口）
     * @return 是否同源
     */
    public static boolean isSameOrigin(Map<String,String> headers, String origin) {
        String o = headers==null?null:headers.get("Origin"); if(o!=null) return o.equalsIgnoreCase(origin);
        String r = headers==null?null:headers.get("Referer"); return r!=null && r.startsWith(origin);
    }

    /**
     * 基于通配符判断是否允许 Origin
     * @param origin 请求的 Origin
     * @param patterns 允许模式列表（* 通配符）
     * @return 是否允许
     */
    public static boolean corsAllowOrigin(String origin, List<String> patterns) {
        if (StringUtils.isBlank(origin) || patterns==null) return false; String o=origin.toLowerCase(Locale.ROOT);
        for(String p:patterns){ String t=p.toLowerCase(Locale.ROOT).replace("*",".*"); if(o.matches(t)) return true; }
        return false;
    }

    /**
     * 根据参数生成缓存键（稳定顺序）
     * @param params 参数映射
     * @return 规范化缓存键（按键名排序并串联 k=v&...）
     */
    public static String buildCacheKey(Map<String,String> params) {
        if(params==null||params.isEmpty()) return ""; List<String> keys=new ArrayList<>(params.keySet()); Collections.sort(keys);
        StringBuilder sb=new StringBuilder(); for(String k:keys){ sb.append(k).append('=').append(params.get(k)).append('&'); } if(sb.length()>0) sb.setLength(sb.length()-1); return sb.toString();
    }

    /**
     * 从头中获取 Trace-Id（多名兼容）
     * @param headers 头集合
     * @return Trace/Request/Correlation Id（若不存在返回 null）
     */
    public static String traceIdFromHeaders(Map<String,String> headers) {
        if (headers==null) return null; String[] ks={"trace-id","x-trace-id","x-request-id","x-correlation-id"};
        for(String k:ks){ String v=headers.get(k); if(!StringUtils.isBlank(v)) return v; }
        return null;
    }

    /**
     * 根据 Accept-Language 推断首选 Locale（不解析 q 权重）
     * @param header Accept-Language 头
     * @return 首选 Locale（无则返回默认 Locale）
     */
    public static Locale inferLocaleFromAccept(String header) {
        List<String> langs = WebUtils.parseAcceptLanguages(header); if(langs.isEmpty()) return Locale.getDefault(); String[] p=langs.get(0).split("-");
        return p.length==2?new Locale(p[0], p[1]):new Locale(p[0]);
    }

    /**
     * 构造禁止缓存的 Cache-Control 头值
     * @return 组合指令：no-store, no-cache, must-revalidate, max-age=0
     */
    public static String cacheControlNoStore() { return "no-store, no-cache, must-revalidate, max-age=0"; }

    /**
     * AcceptItem 模型
     * - type：媒体类型（如 application/json、text/html、任意类型通配）
     * - q：权重（0.0-1.0，默认 1.0），用于内容协商排序
     */
    public static final class AcceptItem { public final String type; public final double q; public AcceptItem(String type,double q){this.type=type;this.q=q;} }
}
