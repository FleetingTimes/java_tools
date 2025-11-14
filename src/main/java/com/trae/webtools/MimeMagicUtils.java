package com.trae.webtools;

/**
 * MIME 魔数检测（根据文件头部字节判断常见类型）
 *
 * 工作原理：
 * - 魔数（Magic Number）是文件开头的固定签名字节序列，用于快速识别文件格式；
 *   本工具通过比对常见格式的魔数来推断 MIME 类型，而不解析完整内容。
 *
 * 特殊名称与签名说明：
 * - PNG（Portable Network Graphics）：签名为 89 50 4E 47（即 0x89 'P' 'N' 'G'）；MIME 为 image/png。
 * - GIF（Graphics Interchange Format）：签名以 "GIF8" 开头（常见为 GIF87a/GIF89a）；MIME 为 image/gif。
 * - JPEG（Joint Photographic Experts Group）：签名为 FF D8 FF（SOI 起始标记）；MIME 为 image/jpeg。
 * - WEBP：基于 RIFF 容器（Resource Interchange File Format）；此处仅用 "WEBP" 简化识别，完整判断需检查 RIFF/WEBP 结构；MIME 为 image/webp。
 * - ZIP：签名为 'P' 'K' 03 04（本地文件头）；MIME 为 application/zip。
 * - GZIP：签名为 1F 8B（ID1 与 ID2）；MIME 为 application/gzip。
 * - PDF（Portable Document Format）：签名为 "%PDF"；MIME 为 application/pdf。
 *
 * 局限性：
 * - 某些格式签名复杂或可变（如 WEBP/RIFF、部分容器格式）；此实现为简化判断，仅覆盖常见场景。
 * - 不解析文件内容，仅检查前若干字节，无法区分近似格式或伪造文件头；安全场景需结合更严格解析或校验。
 */
public final class MimeMagicUtils {
    private MimeMagicUtils() {}

    /**
     * 根据魔数判断类型（未知返回 application/octet-stream）
     * @param head 文件头部若干字节（建议至少 4-8 字节）
     * @return MIME 类型（未知返回 application/octet-stream）
     */
    public static String detect(byte[] head) {
        if (head == null || head.length < 4) return "application/octet-stream";
        if (startsWith(head, new byte[]{(byte)0x89, 'P', 'N', 'G'})) return "image/png";
        if (startsWith(head, new byte[]{'G','I','F','8'})) return "image/gif";
        if (startsWith(head, new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF})) return "image/jpeg";
        if (startsWith(head, new byte[]{'W','E','B','P'})) return "image/webp"; // RIFF/WEBP更复杂，此处简化
        if (startsWith(head, new byte[]{'P','K',0x03,0x04})) return "application/zip";
        if (startsWith(head, new byte[]{0x1F, (byte)0x8B})) return "application/gzip";
        if (startsWith(head, new byte[]{'%','P','D','F'})) return "application/pdf";
        return "application/octet-stream";
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (data[i] != prefix[i]) return false;
        return true;
    }
}
