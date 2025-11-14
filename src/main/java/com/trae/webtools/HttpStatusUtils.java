package com.trae.webtools;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 状态辅助：原因短语与分类、状态行构造/解析
 *
 * 功能概览：
 * - 原因短语：按常见状态码提供标准 Reason-Phrase（未知返回空串）。
 * - 分类判断：信息（1xx）、成功（2xx）、重定向（3xx）、客户端错误（4xx）、服务端错误（5xx）。
 * - 状态行操作：构造/解析形如 "HTTP/1.1 404 Not Found" 的状态行文本。
 *
 * 适用场景：
 * - 自实现轻量 HTTP 响应生成（如内嵌服务/Mock 服务）时的状态文本构造。
 * - 日志与调试工具中进行状态分类统计与展示。
 * - 解析上游或抓包数据中的状态行，提取状态码做进一步处理。
 */
public final class HttpStatusUtils {
    private HttpStatusUtils() {}

    private static final Map<Integer,String> REASONS = new LinkedHashMap<>();
    static {
        REASONS.put(100, "Continue"); REASONS.put(101, "Switching Protocols"); REASONS.put(102, "Processing");
        REASONS.put(200, "OK"); REASONS.put(201, "Created"); REASONS.put(202, "Accepted"); REASONS.put(204, "No Content");
        REASONS.put(301, "Moved Permanently"); REASONS.put(302, "Found"); REASONS.put(303, "See Other"); REASONS.put(304, "Not Modified"); REASONS.put(307, "Temporary Redirect"); REASONS.put(308, "Permanent Redirect");
        REASONS.put(400, "Bad Request"); REASONS.put(401, "Unauthorized"); REASONS.put(403, "Forbidden"); REASONS.put(404, "Not Found"); REASONS.put(405, "Method Not Allowed"); REASONS.put(409, "Conflict"); REASONS.put(410, "Gone"); REASONS.put(415, "Unsupported Media Type"); REASONS.put(429, "Too Many Requests");
        REASONS.put(500, "Internal Server Error"); REASONS.put(501, "Not Implemented"); REASONS.put(502, "Bad Gateway"); REASONS.put(503, "Service Unavailable"); REASONS.put(504, "Gateway Timeout");
    }

    /** 获取标准原因短语（未知返回空串） */
    public static String reasonPhrase(int status) { String r=REASONS.get(status); return r==null?"":r; }

    /** 是否信息响应（1xx） */
    public static boolean isInformational(int status) { return status>=100 && status<200; }

    /** 是否成功（2xx） */
    public static boolean isSuccess(int status) { return status>=200 && status<300; }

    /** 是否重定向（3xx） */
    public static boolean isRedirect(int status) { return status>=300 && status<400; }

    /** 是否客户端错误（4xx） */
    public static boolean isClientError(int status) { return status>=400 && status<500; }

    /** 是否服务端错误（5xx） */
    public static boolean isServerError(int status) { return status>=500 && status<600; }

    /**
     * 解析状态行（如 "HTTP/1.1 404 Not Found"），返回状态码；失败返回 -1
     *
     * 说明：使用空白归一化后按空格切分，取第二段作为状态码；当文本不合法或无法解析时返回 -1。
     * @param statusLine 状态行文本
     * @return 状态码或 -1
     */
    public static int parseStatusLine(String statusLine) {
        if (StringUtils.isBlank(statusLine)) return -1; String[] parts=StringUtils.normalizeWhitespace(statusLine).split(" "); if(parts.length<2) return -1; return ConvertUtils.safeParseInt(parts[1], -1);
    }

    /** 构造状态行（HTTP/1.1） */
    public static String buildStatusLine(int status) { String reason=reasonPhrase(status); return "HTTP/1.1 " + status + (reason.isEmpty()?"":" "+reason); }

    /**
     * 构造状态行（可选版本）
     * @param version 协议版本（为空则默认为 "HTTP/1.1"）
     * @param status 状态码
     * @param reasonOverride 原因短语覆盖（为空则取标准短语）
     * @return 状态行文本
     */
    public static String buildStatusLine(String version, int status, String reasonOverride) { String v=StringUtils.isBlank(version)?"HTTP/1.1":version; String reason=StringUtils.isBlank(reasonOverride)?reasonPhrase(status):reasonOverride; return v + " " + status + (StringUtils.isBlank(reason)?"":" "+reason); }
}
