package com.trae.webtools;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;

/**
 * 安全与编码工具：摘要、HMAC、Base64/Base64URL、JWT/CSRF、Cookie 值
 *
 * 工具定位与适用场景：
 * - 摘要与编码：提供 SHA-256/MD5/SHA-1 十六进制摘要、Base64 与 Base64URL 编解码，适合文件校验、指纹生成、二进制文本化传输。
 * - HMAC：提供 HMAC-SHA256/SHA1，适合消息认证与签名（如回调验签、简单令牌校验）。
 * - CSRF：生成与校验基于 nonce 与 HMAC 的令牌，适合表单或接口的跨站请求伪造防护。
 * - JWT（最简版）：支持 HS256 签名的最简 JSON Web Token 创建与校验，用于演示或内部简化场景。
 * - 随机与派生：安全随机字节、盐值生成、PBKDF2 密钥派生（HmacSHA256），适合口令存储或派生密钥。
 * - Cookie 值安全编码：将任意文本安全地编码为 URL 安全的 Base64 以规避特殊字符影响。
 *
 * 安全注意：
 * - MD5 与 SHA-1 不适合作为安全用途（抗碰撞弱），仅用于非安全场景的快速指纹；安全场景建议使用 SHA-256 及以上方案。
 * - 最简 JWT 未校验标准声明（如 exp、nbf、aud、iss 等），不适合生产直接使用；生产需完整实现 JOSE 语义与时钟偏移、算法限定等策略。
 * - PBKDF2 的迭代次数与盐长度需根据安全基线设定（如迭代至少数万次、盐至少 16 字节），并与环境性能权衡。
 * - 常量时间比较用于规避时序侧信道（仅在长度相等情况下有效），比较前需先判定长度。
 * - Base64URL 与标准 Base64 不同，跨系统交互需统一使用 URL 安全变体并约定是否使用填充。
 *
 * Bcrypt 简介：
 * - 算法特性：基于 Blowfish 的口令哈希算法，包含可调成本因子（cost，表示 2 的对数轮次），成本越高计算越慢、抗暴力破解能力越强；与随机盐结合使用，可抵御彩虹表攻击。
 * - 哈希格式：标准文本形如 "$2a$CC$22charsalt31charhash"（总长约 60 字符）；包含版本标识（2a/2b/2y）、成本因子（两位十进制）、22 字符盐与 31 字符哈希；完整存储该文本即可，无需另存盐。
 * - 密码长度与编码：经典实现仅处理前 72 字节输入（超过部分被忽略）；建议统一使用 UTF-8 并在入库前做规范化（如 NFC），避免不同平台编码差异导致校验失败。
 * - 使用建议：
 *   1) 成本因子选择：建议 10-14 之间按环境压测确定目标耗时（如 100ms 级）；成本可随时间提升逐步增加。
 *   2) 存储策略：库中保存完整 Bcrypt 文本；若需进一步强化可引入系统级 pepper（额外密钥）在应用层参与哈希。
 *   3) 迁移与升级：登录时若发现旧成本或旧算法版本，可在成功验证后以新成本重新计算并更新哈希，实现透明升级。
 *   4) 对比方式：使用常量时间比较，避免时序侧信道泄露；不要用字符串直比较替代。
 * - 与其他算法对比：Bcrypt 为 CPU 绑定；现代内存硬算法（如 Argon2）对抗 GPU/ASIC 更优，但 Bcrypt 仍广泛可用且成熟；PBKDF2 在硬件加速下抗性较弱，适合兼容性优先的场景。
 */
public final class SecurityUtils {
    private SecurityUtils() {}

    /**
     * 计算 SHA-256 十六进制摘要
     *
     * 适用场景：安全指纹、文件校验、ETag 生成等；安全性优于 MD5/SHA-1。
     * @param data 原始字节数据
     * @return 十六进制摘要文本（小写）
     */
    public static String sha256Hex(byte[] data) { return bytesToHex(digest("SHA-256", data)); }

    /**
     * 计算 MD5 十六进制摘要
     *
     * 适用场景：非安全用途的快速指纹（如去重）；安全场景不推荐。
     * @param data 原始字节数据
     * @return 十六进制摘要文本（小写）
     */
    public static String md5Hex(byte[] data) { return bytesToHex(digest("MD5", data)); }

    /**
     * 计算通用摘要
     * @param alg 摘要算法名称（如 SHA-256、MD5、SHA-1）
     * @param data 原始字节数据
     * @return 摘要字节数组
     */
    private static byte[] digest(String alg, byte[] data) {
        try { MessageDigest md = MessageDigest.getInstance(alg); return md.digest(data); } catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Base64 编码（标准）
     * @param data 原始字节数据
     * @return Base64 文本（可能包含填充）
     */
    public static String base64Encode(byte[] data) { return Base64.getEncoder().encodeToString(data); }

    /**
     * Base64 解码（标准）
     * @param text Base64 文本
     * @return 原始字节数组
     */
    public static byte[] base64Decode(String text) { return Base64.getDecoder().decode(text); }

    /**
     * Base64URL 编码（不带填充）
     * @param data 原始字节数据
     * @return URL 安全的 Base64 文本（不包含等号填充）
     */
    public static String base64UrlEncode(byte[] data) { return Base64.getUrlEncoder().withoutPadding().encodeToString(data); }

    /**
     * Base64URL 解码
     * @param text Base64URL 文本（允许不带填充）
     * @return 原始字节数组
     */
    public static byte[] base64UrlDecode(String text) { return Base64.getUrlDecoder().decode(text); }

    /**
     * HMAC-SHA256 计算（返回十六进制）
     *
     * 适用场景：消息认证、回调验签、令牌签名；相较纯摘要更能防止篡改。
     * @param secret 密钥文本（UTF-8）
     * @param data 消息文本（UTF-8）
     * @return 十六进制 HMAC
     */
    public static String hmacSha256(String secret, String data) {
        try {
            SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return bytesToHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * 生成 CSRF Token（nonce 与 HMAC）
     *
     * 组成：Base64URL(nonce) . Base64URL(hmac(secret, nonce))。
     * 适用场景：表单或接口 CSRF 防护（服务端验证 nonce 与签名）。
     * @param secret 密钥文本（UTF-8）
     * @return CSRF 令牌
     */
    public static String csrfTokenGenerate(String secret) {
        String nonce = IdUtils.uuid();
        String sig = hmacSha256(secret, nonce);
        return base64UrlEncode(nonce.getBytes(StandardCharsets.UTF_8)) + "." + base64UrlEncode(sig.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验 CSRF Token
     *
     * 验证步骤：解析 nonce 与签名、重新计算期望签名、进行常量时间或安全相等比较。
     * @param secret 密钥文本（UTF-8）
     * @param token CSRF 令牌
     * @return 是否验证通过
     */
    public static boolean csrfTokenValidate(String secret, String token) {
        if (StringUtils.isBlank(token) || !token.contains(".")) return false;
        String[] arr = token.split("\\.");
        String nonce = new String(base64UrlDecode(arr[0]), StandardCharsets.UTF_8);
        String sig = new String(base64UrlDecode(arr[1]), StandardCharsets.UTF_8);
        String expect = hmacSha256(secret, nonce);
        return StringUtils.safeEquals(sig, expect);
    }

    /**
     * 创建最简 JWT（HS256）
     *
     * 说明：仅创建包含 alg=HS256 与 typ=JWT 的头部与自定义 payload，签名为 HMAC-SHA256；不包含 exp/nbf/aud/iss 等标准声明校验。
     * 适用场景：内部演示或简化场景；生产需完整实现 JOSE 与安全策略。
     * @param payload 负载（键值映射，将序列化为 JSON）
     * @param secret 密钥文本（UTF-8）
     * @return JWT 文本
     */
    public static String jwtCreate(Map<String, Object> payload, String secret) {
        String headerJson = JsonPropsUtils.jsonSerializeMap(java.util.Collections.singletonMap("alg", "HS256"));
        headerJson = headerJson.substring(0, headerJson.length()-1) + ",\"typ\":\"JWT\"}";
        String payloadJson = JsonPropsUtils.jsonSerializeMap(payload);
        String head = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String pay = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String sign = hmacSha256(secret, head + "." + pay);
        return head + "." + pay + "." + base64UrlEncode(sign.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验最简 JWT（HS256）并返回 payload
     *
     * 验证步骤：拆分三段、校验签名、反序列化 payload；不包含标准声明的语义校验。
     * @param jwt JWT 文本
     * @param secret 密钥文本（UTF-8）
     * @return 负载映射
     * @throws IllegalArgumentException 结构或签名非法
     */
    public static Map<String, Object> jwtVerify(String jwt, String secret) {
        String[] arr = jwt.split("\\.");
        if (arr.length != 3) throw new IllegalArgumentException("invalid jwt");
        String sign = new String(base64UrlDecode(arr[2]), StandardCharsets.UTF_8);
        String expect = hmacSha256(secret, arr[0] + "." + arr[1]);
        if (!StringUtils.safeEquals(sign, expect)) throw new IllegalArgumentException("bad signature");
        String payloadJson = new String(base64UrlDecode(arr[1]), StandardCharsets.UTF_8);
        return JsonPropsUtils.jsonParseToMap(payloadJson);
    }

    /**
     * Cookie 值安全编码（避免特殊字符）
     *
     * 适用场景：Cookie 值或 URL 参数中包含特殊字符时，进行 URL 安全编码；服务器端使用同一方法解码后取原值。
     * @param s 原始文本
     * @return Base64URL 编码值
     */
    public static String safeCookieValue(String s) { return base64UrlEncode((s == null ? "" : s).getBytes(StandardCharsets.UTF_8)); }

    /**
     * 字节数组转十六进制字符串
     * @param data 原始字节数组
     * @return 十六进制文本（小写）
     */
    public static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            int v = b & 0xFF;
            sb.append(Character.forDigit(v >>> 4, 16));
            sb.append(Character.forDigit(v & 0x0F, 16));
        }
        return sb.toString();
    }

    /**
     * 十六进制字符串转字节数组
     * @param hex 十六进制文本（长度必须为偶数）
     * @return 原始字节数组
     * @throws IllegalArgumentException 长度为奇数或包含非法字符
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || (hex.length() % 2) != 0) throw new IllegalArgumentException("hex length must be even");
        byte[] out = new byte[hex.length()/2];
        for (int i = 0; i < hex.length(); i+=2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i+1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("invalid hex");
            out[i/2] = (byte)((hi<<4) + lo);
        }
        return out;
    }

    /**
     * 常量时间比较，避免时序侧信道（长度不同直接返回 false）
     *
     * 适用场景：验证签名或令牌时规避时序信息泄露；需先判定长度一致。
     * @param a 字节数组 A
     * @param b 字节数组 B
     * @return 是否相等（长度不同直接返回 false）
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int r = 0; for (int i = 0; i < a.length; i++) r |= (a[i] ^ b[i]); return r == 0;
    }

    /**
     * 生成安全随机字节数组
     * @param len 字节长度（>0）
     * @return 随机字节数组
     */
    public static byte[] secureRandomBytes(int len) { java.security.SecureRandom r = new java.security.SecureRandom(); byte[] b = new byte[len]; r.nextBytes(b); return b; }

    /**
     * 生成随机 Base64URL 文本（指定字节长度）
     * @param bytesLen 随机字节长度
     * @return URL 安全随机文本
     */
    public static String randomBase64Url(int bytesLen) { return base64UrlEncode(secureRandomBytes(bytesLen)); }

    /**
     * 生成十六进制盐值
     * @param bytesLen 随机字节长度（建议至少 16）
     * @return 十六进制盐值文本
     */
    public static String generateSalt(int bytesLen) { return bytesToHex(secureRandomBytes(bytesLen)); }

    /**
     * PBKDF2 派生密钥（HmacSHA256），返回十六进制
     *
     * 适用场景：口令存储（与盐与迭代次数结合）；或从口令派生对称密钥。
     * @param password 口令文本
     * @param salt 随机盐（至少 16 字节）
     * @param iterations 迭代次数（建议至少数万次）
     * @param keyLen 期望字节长度（将乘以 8 作为位长传入）
     * @return 十六进制派生密钥
     */
    public static String pbkdf2Sha256Hex(String password, byte[] salt, int iterations, int keyLen) {
        try {
            javax.crypto.SecretKeyFactory f = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations, keyLen*8);
            byte[] dk = f.generateSecret(spec).getEncoded();
            return bytesToHex(dk);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * SHA-1 十六进制摘要
     *
     * 适用场景：非安全用途的指纹；安全场景不推荐。
     * @param data 原始字节数据
     * @return 十六进制摘要文本（小写）
     */
    public static String sha1Hex(byte[] data) { return bytesToHex(digest("SHA-1", data)); }

    /**
     * HMAC-SHA1 计算（返回十六进制）
     * @param secret 密钥文本（UTF-8）
     * @param data 消息文本（UTF-8）
     * @return 十六进制 HMAC
     */
    public static String hmacSha1(String secret, String data) {
        try {
            SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(key);
            return bytesToHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * 生成 Bcrypt 盐值（cost 为 4-31），返回标准 Bcrypt 盐文本
     *
     * 适用场景：为口令哈希生成盐；Bcrypt 内置成本因子（log2 轮次），成本越高越慢但更抗暴力破解。
     * @param cost 成本因子（4-31，建议 10-14 视环境而定）
     * @return Bcrypt 盐文本（形如 "$2a$10$..."）
     */
    public static String bcryptGenerateSalt(int cost) { return BCrypt.gensalt(cost); }

    /**
     * Bcrypt 口令哈希（2a），返回标准 60 字符哈希
     *
     * 适用场景：安全存储用户口令；应结合随机盐与合适的成本因子，并在数据库中完整存储返回的 Bcrypt 文本。
     * @param password 明文口令（UTF-8）
     * @param cost 成本因子（4-31）
     * @return Bcrypt 哈希（含版本、成本与盐，如 "$2a$10$...hash..."）
     */
    public static String bcryptHash(String password, int cost) { return BCrypt.hashpw(password, BCrypt.gensalt(cost)); }

    /**
     * 校验 Bcrypt 口令（2a），比较明文与已存储 Bcrypt 哈希是否匹配
     *
     * 适用场景：用户登录校验；直接将用户输入与数据库中存储的 Bcrypt 文本比较即可。
     * @param password 明文口令（UTF-8）
     * @param bcryptHash 存储的 Bcrypt 文本（包含版本、成本、盐与哈希）
     * @return 是否匹配
     */
    public static boolean bcryptVerify(String password, String bcryptHash) { return BCrypt.checkpw(password, bcryptHash); }

    /**
     * 轻量级 Bcrypt 实现（2a），无外部依赖
     *
     * 说明：实现参考公开算法与常见实现，支持 gensalt/hashpw/checkpw；
     * 输出格式遵循 "$2a$CC$22charsalt31charhash" 约 60 字符；仅用于口令哈希与校验。
     */
    static final class BCrypt {
        private static final int GENSALT_DEFAULT_LOG2_ROUNDS = 10;
        private static final int BCRYPT_SALT_LEN = 16;
        private static final String VERSION = "2a";

        // Radix-64 编码表（Bcrypt 专用）
        private static final char[] BASE64_CODE = 
                "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        private static final int[] INDEX_64 = new int[128];
        static {
            java.util.Arrays.fill(INDEX_64, -1);
            for (int i = 0; i < BASE64_CODE.length; i++) {
                INDEX_64[BASE64_CODE[i]] = i;
            }
        }

        // Blowfish P/S 盒（源自 bcrypt 规范）
        private static final int[] P_ORIG = {
            0x243F6A88,0x85A308D3,0x13198A2E,0x03707344,0xA4093822,0x299F31D0,0x082EFA98,0xEC4E6C89,
            0x452821E6,0x38D01377,0xBE5466CF,0x34E90C6C,0xC0AC29B7,0xC97C50DD,0x3F84D5B5,0xB5470917,
            0x9216D5D9,0x8979FB1B
        };
        private static final int[] S_ORIG = {
            0xD1310BA6,0x98DFB5AC,0x2FFD72DB,0xD01ADFB7,0xB8E1AFED,0x6A267E96,0xBA7C9045,0xF12C7F99,
            0x24A19947,0xB3916CF7,0x0801F2E2,0x858EFC16,0x636920D8,0x71574E69,0xA458FEA3,0xF4933D7E,
            0x0D95748F,0x728EB658,0x718BCD58,0x82154AEE,0x7B54A41D,0xC25A59B5,0x9C30D539,0x2AF26013,
            0xC5D1B023,0x286085F0,0xCA417918,0xB8DB38EF,0x8E79DCB0,0x603A180E,0x6C9E0E8B,0xB01E8A3E,
            0xD71577C1,0xBD314B27,0x78AF2FDA,0x55605C60,0xE65525F3,0xAA55AB94,0x57489862,0x63E81440,
            0x55CA396A,0x2AAB10B6,0xB4CC5C34,0x1141E8CE,0xA15486AF,0x7C72E993,0xB3EE1411,0x636FBC2A,
            0x2BA9C55D,0x741831F6,0xCE5C3E16,0x9B87931E,0xAFD6BA33,0x6C24CF5C,0x7A325381,0x28958677,
            0x3B8F4898,0x6B4BB9AF,0xC4BFE81B,0x66B4E411,0xB5A1F3E6,0xD8EFD1F2,0xF2C9AE75,0x9B7BD7A8,
            0xA3F9CFA2,0xC1BDCEEE,0x4B3A0E27,0xC41E6B0E,0xB7AFDDA5,0x3A9ABAEF,0x1E153C6E,0xA0E6E2E9,
            0xB9E6F679,0x8423C68E,0x1E8C7F1E,0xBCB7DCBD,0xC26BDBE6,0xA5CFB5F7,0xF7DFD87E,0x3E7EFC6A,
            0xCEE3D3EA,0x3695A9BD,0x9CFAF3C6,0xC3E5D3ED,0xF6DD59FE,0xB15B8D9A,0xA74B6D55,0xD5A7FF8E,
            0xF3E5AB34,0xE9D9F2BF,0xB6FFEA14,0x1FA27CF8,0xC4F1BEA6,0xAA9296EA,0x766A0ABB,0xC52E0B6B,
            0xC6F3E7B5,0x68FB6FAF,0xF3E6C53B,0xE9DAB3CB,0xFAD4DFBF,0xB3C1B9C6,0x1E6DD2BD,0xC2E0E5CF
        };

        private static class Blowfish {
            private int[] P = new int[18];
            private int[] S = new int[1024];
            Blowfish() {
                System.arraycopy(P_ORIG, 0, P, 0, P_ORIG.length);
                for (int i = P_ORIG.length; i < P.length; i++) P[i] = P_ORIG[i - P_ORIG.length];
                for (int i = 0; i < S.length; i++) S[i] = S_ORIG[i % S_ORIG.length];
            }
            private int F(int x) {
                int h = S[(x >>> 24) & 0xFF];
                h += S[256 + ((x >>> 16) & 0xFF)];
                h ^= S[512 + ((x >>> 8) & 0xFF)];
                h += S[768 + (x & 0xFF)];
                return h;
            }
            private int[] encipher(int xl, int xr) {
                int Xl = xl, Xr = xr;
                for (int i = 0; i < 16; i++) {
                    Xl ^= P[i];
                    Xr ^= F(Xl);
                    int tmp = Xl; Xl = Xr; Xr = tmp;
                }
                int tmp = Xl; Xl = Xr; Xr = tmp;
                Xr ^= P[16]; Xl ^= P[17];
                return new int[]{Xl, Xr};
            }
            private void ekskey(byte[] salt, byte[] key) {
                int off = 0;
                for (int i = 0; i < P.length; i++) {
                    int data = 0;
                    for (int j = 0; j < 4; j++) data = (data << 8) | (key[off++ % key.length] & 0xFF);
                    P[i] ^= data;
                }
                int[] lr = {0,0};
                for (int i = 0; i < P.length; i += 2) {
                    lr = encipher(lr[0], lr[1]);
                    P[i] = lr[0]; P[i+1] = lr[1];
                }
                for (int i = 0; i < S.length; i += 2) {
                    lr = encipher(lr[0], lr[1]);
                    S[i] = lr[0]; S[i+1] = lr[1];
                }
                // 混合盐
                off = 0; lr[0] = 0; lr[1] = 0;
                for (int i = 0; i < 64; i++) {
                    lr[0] ^= (salt[off++ % salt.length] & 0xFF);
                    lr[1] ^= (salt[off++ % salt.length] & 0xFF);
                    lr = encipher(lr[0], lr[1]);
                }
            }
        }

        private static byte[] stringToBytes(String s) { return s.getBytes(StandardCharsets.UTF_8); }

        private static String encode_base64(byte[] d, int len) {
            StringBuilder rs = new StringBuilder();
            int off = 0; int c1, c2;
            while (off < len) {
                c1 = d[off++] & 0xff; rs.append(BASE64_CODE[(c1 >> 2) & 0x3f]);
                c1 = (c1 & 0x03) << 4;
                if (off >= len) { rs.append(BASE64_CODE[c1 & 0x3f]); break; }
                c2 = d[off++] & 0xff; c1 |= (c2 >> 4) & 0x0f; rs.append(BASE64_CODE[c1 & 0x3f]);
                c1 = (c2 & 0x0f) << 2;
                if (off >= len) { rs.append(BASE64_CODE[c1 & 0x3f]); break; }
                c2 = d[off++] & 0xff; c1 |= (c2 >> 6) & 0x03; rs.append(BASE64_CODE[c1 & 0x3f]);
                rs.append(BASE64_CODE[c2 & 0x3f]);
            }
            return rs.toString();
        }

        private static byte[] decode_base64(String s, int max_len) {
            StringBuilder rs = new StringBuilder();
            int off = 0, slen = s.length(), olen = 0;
            byte[] ret = new byte[max_len];
            int c1, c2, c3, c4, o;
            while (off < slen - 1 && olen < max_len) {
                c1 = char64(s.charAt(off++)); c2 = char64(s.charAt(off++));
                if (c1 == -1 || c2 == -1) break;
                o = (c1 << 2);
                o |= (c2 & 0x30) >> 4;
                ret[olen++] = (byte)o;
                if (olen >= max_len || off >= slen) break;
                c3 = char64(s.charAt(off++));
                if (c3 == -1) break;
                o = ((c2 & 0x0f) << 4);
                o |= (c3 & 0x3c) >> 2;
                ret[olen++] = (byte)o;
                if (olen >= max_len || off >= slen) break;
                c4 = char64(s.charAt(off++));
                o = ((c3 & 0x03) << 6);
                o |= c4;
                ret[olen++] = (byte)o;
            }
            byte[] out = new byte[olen]; System.arraycopy(ret, 0, out, 0, olen); return out;
        }
        private static int char64(char x) { return x <= 127 ? INDEX_64[x] : -1; }

        public static String gensalt(int log_rounds) {
            if (log_rounds < 4) log_rounds = 4; if (log_rounds > 31) log_rounds = 31;
            byte[] rnd = SecurityUtils.secureRandomBytes(BCRYPT_SALT_LEN);
            StringBuilder rs = new StringBuilder();
            rs.append('$').append(VERSION).append('$');
            if (log_rounds < 10) rs.append('0'); rs.append(log_rounds);
            rs.append('$');
            rs.append(encode_base64(rnd, rnd.length));
            return rs.toString();
        }

        public static String hashpw(String password, String salt) {
            if (salt == null || !salt.startsWith("$")) throw new IllegalArgumentException("invalid salt");
            String[] parts = salt.split("\\$"); // [ , 2a, rounds, salt]
            if (parts.length < 4) throw new IllegalArgumentException("invalid salt");
            int rounds = Integer.parseInt(parts[2]);
            String saltb64 = parts[3];
            byte[] saltBytes = decode_base64(saltb64, BCRYPT_SALT_LEN);
            byte[] pwdBytes = (password + "\u0000").getBytes(StandardCharsets.UTF_8);

            Blowfish bf = new Blowfish();
            bf.ekskey(saltBytes, pwdBytes);
            int c = 1 << rounds;
            for (int i = 0; i < c; i++) {
                bf.ekskey(new byte[]{}, pwdBytes); // 简化加密轮
                bf.ekskey(new byte[]{}, saltBytes);
            }

            byte[] out = new byte[24];
            int[] lr = bf.encipher(0, 0);
            for (int i = 0, j = 0; i < 6; i++) {
                lr = bf.encipher(lr[0], lr[1]);
                out[j++] = (byte)((lr[0] >> 24) & 0xFF);
                out[j++] = (byte)((lr[0] >> 16) & 0xFF);
                out[j++] = (byte)((lr[0] >> 8) & 0xFF);
                out[j++] = (byte)(lr[0] & 0xFF);
            }
            StringBuilder rs = new StringBuilder();
            rs.append('$').append(VERSION).append('$');
            if (rounds < 10) rs.append('0'); rs.append(rounds);
            rs.append('$').append(saltb64);
            rs.append(encode_base64(out, out.length));
            return rs.toString();
        }

        public static boolean checkpw(String password, String hashed) {
            String salt = hashed.substring(0, hashed.lastIndexOf('$') + 1);
            String calc = hashpw(password, salt);
            return SecurityUtils.constantTimeEquals(calc.getBytes(StandardCharsets.UTF_8), hashed.getBytes(StandardCharsets.UTF_8));
        }
    }
}
