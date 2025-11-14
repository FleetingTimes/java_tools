package com.trae.webtools;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 校验扩展工具：卡号 Luhn、IP/主机名/URL、UUID、颜色、Base64、IBAN、MAC、邮箱、JSON Pointer 等
 *
 * 功能概览：
 * - 金融与标识：Luhn 校验与卡品牌检测、UUID 校验（版本 1-5）
 * - 网络与文本：IPv4/IPv6（简化版）校验、私有 IPv4 识别、主机名严格校验、URL 严格校验（scheme 与 host 必须存在）
 * - 编码与格式：十六进制颜色（3/6 位）、Base64 文本（标准字母表与填充）、IBAN 基本长度与字符校验（不含校验位计算）、MAC 地址（冒号或短横分隔）
 * - 结构标识：JSON Pointer 简易校验（路径段由字母/数字/下划线/点/短横组成）
 * - 字符与口令：ASCII/可打印 ASCII、纯数字、字母与数字混合、强口令（长度≥8，含大小写/数字/特殊字符）、中国大陆手机号（简化）
 *
 * 用途与适用场景：
 * - API 入参预校验：在控制器或服务入口对关键字段进行格式校验，快速反馈 400 错误并减少后续逻辑开销；
 * - 表单与前端校验后端兜底：虽然前端已做校验，后端仍需格式与范围兜底，避免绕过导致非法数据进入系统；
 * - 数据清洗与导入：批量导入时先筛除不合规行（如邮箱/URL/IP），提高后续处理稳定性；
 * - 安全与健壮性：强口令判定辅助账号安全策略；私有网段识别、主机名/URL 严格校验辅助网络与资源访问安全；
 * - 表示一致性：JSON Pointer、Base64、颜色等基础格式校验，保证配置与内容的统一性，减少兼容问题；
 *
 * 最佳实践：
 * - 先做空白归一化与剪裁（如 trim），再进行格式校验；对大小写不敏感的标识（如邮箱域）可统一小写。
 * - 将“格式校验”与“语义校验”分层，例如 URL 可先格式判定，再做可达性或白名单判定；IBAN 可先基本长度字符校验，再做校验位计算。
 * - 对易变规则（如手机号段）封装为可配置或集中维护的正则，减少散落在业务代码中的重复与不一致。
 * - 替换敏感错误提示为通用信息（如“格式不正确”），避免泄露内部规则与安全策略细节。
 *
 * 使用建议：
 * - 正则校验面向格式匹配，不能替代语义校验（如 URL 的连通性、IBAN 的校验位算法等）；需要更严格时应叠加进一步逻辑。
 * - IPv6 校验为简化表达式，不支持所有压缩形式；如需 RFC 级完整性请使用更严格解析器。
 * - 用户输入前可先进行剪裁与标准化（如去除首尾空白），再进行格式校验，以降低误判。
 */
public final class ValidationUtils {
    private ValidationUtils() {}

    /** IPv4 地址：每段 0-255，四段点分 */
    private static final Pattern IPV4 = Pattern.compile("^(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)){3}$");
    /** IPv6 地址（简化版）：完整 8 组或若干压缩形式（不覆盖所有情况） */
    private static final Pattern IPV6 = Pattern.compile("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^(([0-9a-fA-F]{1,4}:){1,7}:)$|^(:([0-9a-fA-F]{1,4}:){1,7})$");
    /** 主机名（严格）：标签由字母数字与短横组成，长度限制符合常见规则 */
    private static final Pattern HOSTNAME = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");
    /** UUID（版本 1-5）：按 RFC4122 的格式约束 */
    private static final Pattern UUID = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");
    /** 十六进制颜色：支持 #RGB 或 #RRGGBB（# 可省略） */
    private static final Pattern HEX_COLOR = Pattern.compile("^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
    /** Base64（标准）：字母/数字/+/，/ 组成，结尾允许 = 填充 */
    private static final Pattern BASE64 = Pattern.compile("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$");
    /** MAC 地址：6 段十六进制，分隔符支持冒号或短横 */
    private static final Pattern MAC = Pattern.compile("^([0-9A-Fa-f]{2}[:\\-]){5}([0-9A-Fa-f]{2})$");
    /** 邮箱（严格）：基本用户名规则 + 域名 + 顶级域长度≥2 */
    private static final Pattern EMAIL_STRICT = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    /** JSON Pointer（简化）：路径段由字母/数字/下划线/点/短横组成 */
    private static final Pattern JSON_POINTER = Pattern.compile("^(?:/[A-Za-z0-9_\\-.]+)*$");

    /**
     * Luhn 校验（信用卡号等）
     * @param number 待校验数字串
     * @return 是否通过 Luhn 算法校验
     */
    public static boolean isLuhnValid(String number) {
        if (StringUtils.isBlank(number)) return false;
        int sum = 0; boolean dbl = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            char c = number.charAt(i);
            if (!Character.isDigit(c)) return false;
            int d = c - '0';
            if (dbl) { d *= 2; if (d > 9) d -= 9; }
            sum += d; dbl = !dbl;
        }
        return sum % 10 == 0;
    }

    /**
     * 粗略识别卡品牌（基于前缀）
     * @param number 卡号（不含空格）
     * @return 品牌（VISA/MASTERCARD/AMEX/DINERS/UNKNOWN）
     */
    public static String detectCardBrand(String number) {
        if (StringUtils.isBlank(number)) return "UNKNOWN";
        String n = number.trim();
        if (n.matches("^4[0-9]{12}(?:[0-9]{3})?$") ) return "VISA";
        if (n.matches("^5[1-5][0-9]{14}$") ) return "MASTERCARD";
        if (n.matches("^3[47][0-9]{13}$") ) return "AMEX";
        if (n.matches("^3(?:0[0-5]|[68][0-9])[0-9]{11}$") ) return "DINERS";
        return "UNKNOWN";
    }

    /** 是否为 IPv4 */
    public static boolean isIPv4(String ip) { return ip != null && IPV4.matcher(ip).matches(); }

    /** 是否为 IPv6（简化版） */
    public static boolean isIPv6(String ip) { return ip != null && IPV6.matcher(ip).matches(); }

    /**
     * 是否为私有 IPv4 地址（RFC1918）
     * @param ip IPv4 文本
     * @return 是否属于 10/8、172.16/12、192.168/16
     */
    public static boolean isIPv4Private(String ip) {
        if (!isIPv4(ip)) return false;
        String[] parts = ip.split("\\.");
        int a = Integer.parseInt(parts[0]), b = Integer.parseInt(parts[1]);
        if (a == 10) return true;
        if (a == 172 && (b >= 16 && b <= 31)) return true;
        if (a == 192 && b == 168) return true;
        return false;
    }

    /** 是否为主机名（严格） */
    public static boolean isHostname(String host) { return host != null && HOSTNAME.matcher(host).matches(); }

    /**
     * URL 严格格式校验（协议、主机、路径）
     * @param url URL 文本
     * @return 是否拥有合法的 scheme 与 host
     */
    public static boolean isUrlStrict(String url) {
        if (StringUtils.isBlank(url)) return false;
        try {
            java.net.URI u = new java.net.URI(url);
            return u.getScheme() != null && u.getHost() != null;
        } catch (Exception e) { return false; }
    }

    /** 是否为 UUID（版本 1-5） */
    public static boolean isUUID(String s) { return s != null && UUID.matcher(s).matches(); }

    /** 是否为十六进制颜色 */
    public static boolean isHexColor(String s) { return s != null && HEX_COLOR.matcher(s).matches(); }

    /** 是否为 Base64 文本 */
    public static boolean isBase64(String s) { return s != null && BASE64.matcher(s).matches(); }

    /** IBAN 基本长度与字符校验（不含校验位计算） */
    public static boolean isIbanBasic(String s) { return s != null && s.matches("^[A-Z]{2}[0-9A-Z]{13,30}$"); }

    /** 是否为 MAC 地址 */
    public static boolean isMacAddress(String s) { return s != null && MAC.matcher(s).matches(); }

    /** 是否为 JSON Pointer（简单） */
    public static boolean isJsonPointer(String s) { return s != null && JSON_POINTER.matcher(s).matches(); }

    /** 严格邮箱校验 */
    public static boolean isEmailStrict(String s) { return s != null && EMAIL_STRICT.matcher(s).matches(); }

    /** 是否为 ASCII 字符串 */
    public static boolean isAscii(String s) { if (s==null) return false; for (char c : s.toCharArray()) if (c>127) return false; return true; }

    /** 是否为可打印 ASCII（32-126） */
    public static boolean isPrintableAscii(String s) { if (s==null) return false; for (char c : s.toCharArray()) if (c<32||c>126) return false; return true; }

    /** 是否仅由数字组成 */
    public static boolean hasOnlyDigits(String s) { if (StringUtils.isBlank(s)) return false; for (char c : s.toCharArray()) if (!Character.isDigit(c)) return false; return true; }

    /** 是否同时含有字母与数字 */
    public static boolean hasLettersAndDigits(String s) {
        if (StringUtils.isBlank(s)) return false; boolean hasL=false, hasD=false;
        for (char c : s.toCharArray()) { if (Character.isLetter(c)) hasL=true; else if (Character.isDigit(c)) hasD=true; }
        return hasL && hasD;
    }

    /** 强口令校验：长度≥8，含大写、小写、数字与特殊字符 */
    public static boolean isStrongPassword(String s) {
        if (s == null || s.length() < 8) return false;
        boolean up=false, lo=false, di=false, sp=false;
        for (char c : s.toCharArray()) {
            if (Character.isUpperCase(c)) up = true;
            else if (Character.isLowerCase(c)) lo = true;
            else if (Character.isDigit(c)) di = true;
            else sp = true;
        }
        return up && lo && di && sp;
    }

    /** 中国大陆手机号（简化） */
    public static boolean isChineseMobile(String s) { return s != null && s.matches("^1[3-9]\\d{9}$"); }
}
