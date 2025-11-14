package com.trae.webtools;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Web 常用工具：分页、响应包装、校验、脱敏、ETag、MIME/CORS/IP、版本比较
 *
 * 功能概览：
 * - 分页：将列表按页码与页大小切片并返回 {@link Page}
 * - 响应包装：统一 API 响应结构 {@link ApiResponse}
 * - 输入校验：邮箱/电话/密码强度、正数/非空校验
 * - 脱敏：邮箱/电话打码
 * - 内容类型与缓存：扩展名到 MIME、Cache-Control 与 CORS 头构造
 * - 客户端标识：从代理头推断客户端 IP、User-Agent 移动端判定
 * - ETag：基于数据摘要生成简易 ETag（强 ETag 的一类实现）
 * - 版本与语言：语义版本比较，解析 Accept-Language
 * - 其他：安全文件名生成、URL 缓存破坏参数追加
 *
 * 使用建议：
 * - 生产环境的 ETag 与缓存策略需结合资源变体与条件请求（见 ETagPlusUtils、HttpCacheUtils）
 * - CORS 头的设置需结合实际安全策略（origin 与 allow-credentials），避免过度放开
 * - 从代理头推断 IP 仅为简化版，实际应结合可信代理链与白名单
 */
public final class WebUtils {
    private WebUtils() {}

    /**
     * 简易分页计算（页码从 1 开始）
     * @param items 源列表（可为 null）
     * @param page 页码（从 1 开始，越界将裁剪到合法范围）
     * @param size 每页大小（最小为 1）
     * @return 分页结果模型 {@link Page}
     */
    public static <T> Page<T> paginate(List<T> items, int page, int size) {
        if (items == null) items = Collections.emptyList();
        int total = items.size();
        size = Math.max(1, size);
        int pages = (int) Math.ceil(total / (double) size);
        page = Math.min(Math.max(1, page), Math.max(1, pages));
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<T> slice = from >= to ? Collections.emptyList() : new ArrayList<>(items.subList(from, to));
        return new Page<>(page, size, total, pages, slice);
    }

    /** 统一 API 响应包装（成功） */
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, null, data); }
    /** 统一 API 响应包装（失败） */
    public static <T> ApiResponse<T> fail(String message) { return new ApiResponse<>(false, message, null); }

    /**
     * 参数非空校验（空则抛 IllegalArgumentException）
     * @param obj 对象
     * @param name 参数名（用于错误消息）
     * @return 原对象（非空）
     */
    public static <T> T validateNotNull(T obj, String name) { if (obj == null) throw new IllegalArgumentException(name + " must not be null"); return obj; }

    /**
     * 校验正数（非正则抛 IllegalArgumentException）
     * @param n 数值
     * @param name 参数名（用于错误消息）
     * @return 原数值（>0）
     */
    public static int validatePositive(int n, String name) { if (n <= 0) throw new IllegalArgumentException(name + " must be positive"); return n; }

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{6,15}$");

    /**
     * 邮箱格式校验（简易）
     * @param email 邮箱文本
     * @return 是否匹配基本邮箱格式
     */
    public static boolean validateEmail(String email) { return email != null && EMAIL.matcher(email).matches(); }

    /**
     * 电话格式校验（国际简易）
     * @param phone 电话文本（允许前缀 +，6-15 位数字）
     * @return 是否匹配基本国际电话格式
     */
    public static boolean validatePhone(String phone) { return phone != null && PHONE.matcher(phone).matches(); }

    /**
     * 密码强度校验（长度≥8，包含大小写字母与数字）
     * @param pwd 密码文本
     * @return 是否符合基本强度要求
     */
    public static boolean validatePasswordStrength(String pwd) {
        if (pwd == null || pwd.length() < 8) return false;
        boolean hasUpper=false, hasLower=false, hasDigit=false;
        for (char c : pwd.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            if (hasUpper && hasLower && hasDigit) return true;
        }
        return false;
    }

    /**
     * 邮箱打码（保留前 2 位与域名）
     * @param email 邮箱文本
     * @return 打码后的邮箱（不合法输入原样返回）
     */
    public static String maskEmail(String email) {
        if (StringUtils.isBlank(email) || !email.contains("@")) return email;
        String name = StringUtils.substringBefore(email, "@");
        String domain = StringUtils.substringAfter(email, "@");
        String show = name.length() <= 2 ? name : name.substring(0,2);
        return show + "***@" + domain;
    }

    /**
     * 电话打码（保留前 3 位与后 4 位）
     * @param phone 电话文本
     * @return 打码后的电话（长度不足原样返回）
     */
    public static String maskPhone(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() < 7) return phone;
        return phone.substring(0,3) + "****" + phone.substring(phone.length()-4);
    }

    /**
     * 依据文件扩展名判断简易 MIME 类型
     * @param filename 文件名
     * @return MIME 类型（未知返回 application/octet-stream）
     */
    public static String mimeTypeByExtension(String filename) {
        String f = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (f.endsWith(".json")) return "application/json";
        if (f.endsWith(".xml")) return "application/xml";
        if (f.endsWith(".html") || f.endsWith(".htm")) return "text/html";
        if (f.endsWith(".txt")) return "text/plain";
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".gif")) return "image/gif";
        if (f.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    /**
     * 构造 Cache-Control 响应头值（秒）
     * @param maxAgeSeconds 最大缓存秒数
     * @param isPublic 是否 public（否则为 private）
     * @return 头值字符串，如 "public, max-age=3600"
     */
    public static String buildCacheControl(int maxAgeSeconds, boolean isPublic) {
        String type = isPublic ? "public" : "private";
        return type + ", max-age=" + Math.max(0, maxAgeSeconds);
    }

    /**
     * 构造基本 CORS 响应头集合
     * @param origin 允许的 Origin（为空则为 *）
     * @param methods 允许方法列表（逗号分隔，为空则使用默认）
     * @param headers 允许的请求头列表（逗号分隔，为空则使用默认）
     * @param allowCredentials 是否允许携带凭据（true 则添加 Allow-Credentials）
     * @param maxAgeSeconds 预检缓存秒数
     * @return 头名->头值的有序映射
     */
    public static Map<String, String> buildCorsHeaders(String origin, String methods, String headers, boolean allowCredentials, int maxAgeSeconds) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Access-Control-Allow-Origin", StringUtils.isBlank(origin) ? "*" : origin);
        map.put("Access-Control-Allow-Methods", StringUtils.defaultString(methods, "GET,POST,PUT,DELETE,OPTIONS"));
        map.put("Access-Control-Allow-Headers", StringUtils.defaultString(headers, "Content-Type,Authorization"));
        map.put("Access-Control-Max-Age", String.valueOf(Math.max(0, maxAgeSeconds)));
        if (allowCredentials) map.put("Access-Control-Allow-Credentials", "true");
        return map;
    }

    /**
     * 从常见代理头中推断客户端 IP（简化）
     * @param headers 头集合（大小写敏感的键名）
     * @return 推断的客户端 IP（若不存在返回 null）
     */
    public static String clientIpFromHeaders(Map<String, String> headers) {
        if (headers == null) return null;
        String[] keys = {"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP", "X-Client-IP"};
        for (String k : keys) {
            String v = headers.get(k);
            if (!StringUtils.isBlank(v)) return v.split(",")[0].trim();
        }
        return headers.getOrDefault("Remote-Addr", null);
    }

    /**
     * 生成 ETag（基于 SHA-256；强 ETag 的一类实现）
     * @param data 原始字节数据
     * @return ETag 文本（包含引号）
     */
    public static String etagForBytes(byte[] data) { return '"' + SecurityUtils.sha256Hex(data) + '"'; }

    /**
     * 构造简单 ETag（文本）
     * @param s 文本（UTF-8 编码）
     * @return ETag 文本（包含引号）
     */
    public static String etagForString(String s) { return etagForBytes((s == null ? "" : s).getBytes(StandardCharsets.UTF_8)); }

    /**
     * 语义版本比较（返回 -1/0/1）
     * @param a 版本 A（形如 x.y.z）
     * @param b 版本 B（形如 x.y.z）
     * @return -1 表示 a<b，0 表示相等，1 表示 a>b
     */
    public static int versionCompare(String a, String b) {
        int[] A = parseVersion(a); int[] B = parseVersion(b);
        for (int i = 0; i < 3; i++) { if (A[i] != B[i]) return Integer.compare(A[i], B[i]); }
        return 0;
    }

    private static int[] parseVersion(String v) {
        String[] parts = (v == null ? "0.0.0" : v).split("\\.");
        int[] out = new int[]{0,0,0};
        for (int i = 0; i < Math.min(3, parts.length); i++) out[i] = ConvertUtils.safeParseInt(parts[i], 0);
        return out;
    }

    /**
     * 解析 Accept-Language 为降序语言列表（忽略 q 权重，仅保留语言代码）
     * @param header 请求头值
     * @return 语言代码列表（如 zh-CN,en-US）
     */
    public static List<String> parseAcceptLanguages(String header) {
        if (StringUtils.isBlank(header)) return Collections.emptyList();
        String[] parts = header.split(",");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String lang = StringUtils.substringBefore(p.trim(), ";");
            if (!StringUtils.isBlank(lang)) out.add(lang);
        }
        return out;
    }

    /**
     * 粗略判断是否移动端 User-Agent
     * @param ua User-Agent 文本
     * @return 是否为移动端或常见移动 UA
     */
    public static boolean detectMobileUserAgent(String ua) {
        if (StringUtils.isBlank(ua)) return false;
        String t = ua.toLowerCase(java.util.Locale.ROOT);
        return t.contains("mobile") || t.contains("android") || t.contains("iphone") || t.contains("ipad") || t.contains("micromessenger");
    }

    /** 判断是否 JSON Content-Type */
    public static boolean isJsonContentType(String ct) { return ct != null && ct.toLowerCase(java.util.Locale.ROOT).contains("application/json"); }

    /** 判断是否表单 Content-Type */
    public static boolean isFormContentType(String ct) { return ct != null && ct.toLowerCase(java.util.Locale.ROOT).contains("application/x-www-form-urlencoded"); }

    

    /**
     * 依据标题生成安全文件名（移除非法字符并规范化）
     * @param title 标题文本
     * @return 规范化后的文件名（最长 64 字符）
     */
    public static String safeFilenameFromTitle(String title) {
        if (StringUtils.isBlank(title)) return "";
        String t = title.replaceAll("[^A-Za-z0-9._-]", "_");
        t = t.replaceAll("_+", "_");
        return t.length() > 64 ? t.substring(0,64) : t;
    }

    /**
     * 给 URL 添加缓存破坏参数（如 hash）
     * @param url 原始 URL
     * @param hashParam 参数值（追加为 _=hashParam）
     * @return 新 URL（保留原查询参数）
     */
    public static String cacheBustingUrl(String url, String hashParam) {
        if (StringUtils.isBlank(hashParam)) return url;
        return HttpUtils.addQueryParam(url, "_", hashParam);
    }
}
