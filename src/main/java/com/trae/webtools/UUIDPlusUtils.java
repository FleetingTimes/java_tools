package com.trae.webtools;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * UUID 增强工具：短编码与字节转换
 *
 * 概念说明：
 * - UUID（Universally Unique Identifier）为 128 位标识符，常见文本格式为 36 字符（含 4 个连字符），如：xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx；
 *   其中 M 表示版本（1-5），N 表示变体（通常为二进制 10xx，对应 IETF 变体）。
 * - Base64URL 是 Base64 的 URL 安全变体，使用 "-" 与 "_" 替代 "+" 与 "/"，并可省略尾部 "=" 填充；适合嵌入 URL、Cookie、日志等场景。
 *
 * 设计与用途：
 * - 提供将 UUID 以 Base64URL（不带填充）进行紧凑表示：16 字节 UUID -> 22 字符 Base64URL；比传统 36 字符 UUID 更短，便于日志/链接传输。
 * - 提供 UUID 与 16 字节数组互转，便于二进制存储（如数据库 BINARY(16)）与跨语言交互；按 Java 的惯例字节序为「先存高 64 位，再存低 64 位」。
 *
 * 适用场景与注意：
 * - 短编码不包含版本信息的可视化提示，但仍能完整还原 UUID；适合不关注版本/变体展示的场景。
 * - 使用 Base64URL 时建议统一大小写与不带填充策略，避免跨语言差异导致解析错误。
 * - 短编码与原始 UUID 拥有同等唯一性；若使用随机 UUID（v4），仍需遵循唯一约束策略（如数据库唯一索引）。
 * - 与他语言交互时需注意字节顺序约定（高位在前），否则会出现还原后的 UUID 与预期不一致问题。
 *
 * 示例：
 * <pre>
 * UUID id = UUID.randomUUID();
 * String shortId = UUIDPlusUtils.toShortBase64Url(id); // 22 字符 URL 安全短编码
 * UUID back = UUIDPlusUtils.fromShortBase64Url(shortId); // 还原为原始 UUID
 * byte[] raw = UUIDPlusUtils.toBytes(id); // 16 字节二进制形式
 * </pre>
 */
public final class UUIDPlusUtils {
    private UUIDPlusUtils() {}

    /**
     * 生成随机 UUID 的 Base64URL 短编码（不带填充）
     *
     * 说明：16 字节经 Base64URL 编码并去除填充后通常为 22 字符；
     * 该短编码为 URL 安全，适合用于链接参数、Cookie 值与日志记录。
     * @return Base64URL 短编码（长度约 22 字符）
     */
    public static String randomShortBase64Url() { return toShortBase64Url(UUID.randomUUID()); }

    /**
     * 将 UUID 转为 Base64URL 短编码（不带填充）
     * @param uuid 原始 UUID
     * @return URL 安全的短编码文本（不包含 "="）
     */
    public static String toShortBase64Url(UUID uuid) { byte[] b=toBytes(uuid); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }

    /**
     * 从 Base64URL 短编码解析 UUID
     *
     * 要求：输入为 URL 安全 Base64 文本，允许不带尾部填充；
     * 若编码为其他变体（如标准 Base64 或带填充），请先转换为 URL 安全形式或去除填充。
     * @param text Base64URL 文本
     * @return 还原得到的 UUID
     * @throws IllegalArgumentException 当长度非法或不能正确解码为 16 字节时抛出
     */
    public static UUID fromShortBase64Url(String text) { byte[] b=Base64.getUrlDecoder().decode(text); return fromBytes(b); }

    /**
     * UUID 转字节数组（16 字节）
     *
     * 字节顺序：先写入高 64 位（mostSignificantBits），再写入低 64 位（leastSignificantBits）。
     * 该顺序与多数语言的常用约定兼容，便于跨语言持久化与解析。
     * @param uuid 原始 UUID
     * @return 长度为 16 的字节数组
     */
    public static byte[] toBytes(UUID uuid) { ByteBuffer bb=ByteBuffer.allocate(16); bb.putLong(uuid.getMostSignificantBits()); bb.putLong(uuid.getLeastSignificantBits()); return bb.array(); }

    /**
     * 字节数组转 UUID（需 16 字节）
     *
     * 字节顺序：按「先高位后低位」的顺序还原两个 64 位整型。
     * @param bytes 字节数组（必须为 16 字节）
     * @return 还原后的 UUID
     * @throws IllegalArgumentException 当字节数组长度不是 16 时抛出
     */
    public static UUID fromBytes(byte[] bytes) { if(bytes==null||bytes.length!=16) throw new IllegalArgumentException("len!=16"); ByteBuffer bb=ByteBuffer.wrap(bytes); long ms=bb.getLong(); long ls=bb.getLong(); return new UUID(ms, ls); }
}
