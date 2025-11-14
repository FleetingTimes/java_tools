package com.trae.webtools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ETag 增强工具：弱/强 ETag 生成（支持字节与流）
 *
 * 什么是 ETag：
 * - ETag 是服务端为“响应表示”生成的唯一标识，用于判断客户端缓存是否仍然有效
 * - 它是一段被双引号包裹的短文本（如 "abc123"），描述的是“内容的具体字节表示”，而不是“抽象资源”
 * - 不同压缩/序列化/变体（如 Accept-Encoding、Content-Type）通常会生成不同的 ETag
 *
 * 强/弱 ETag：
 * - 强 ETag：任意字节变化都视为不同，格式为 "..."；适合严格一致性校验（静态文件、二进制包）
 * - 弱 ETag：允许语义不变的轻微改动（如时间戳、排版），格式以 W"..." 开头；适合动态页或近似一致性场景
 * - 本工具实现：强/弱 ETag 都基于响应字节的 SHA-256 摘要生成，分别输出 "..." 与 W"..."；同时提供流输入版本
 *
 * 请求/响应头交互：
 * - 响应：ETag: "<tag>" 或 ETag: W"<tag>"
 * - 条件获取：If-None-Match: "<tag>"（命中返回 304 Not Modified）
 * - 条件更新：If-Match: "<tag>"（不匹配返回 412 Precondition Failed，用于乐观并发控制）
 * - 变体协同：Vary: Accept-Encoding 等；同一资源的不同编码/语言变体需生成独立 ETag，并正确设置 Vary，避免错配
 *
 * 生成策略建议：
 * - 以“响应字节表示”为基准计算（如 SHA-256），确保不同压缩/序列化差异产生不同 ETag
 * - 强 ETag用于严格字节一致性；弱 ETag用于内容语义一致但字节可能有微差
 * - 保持稳定与可复现：避免非确定性因素（时间戳、随机数）参与强 ETag；必要时选择弱 ETag
 * - 避免信息泄露：不要把内部版本号、路径或敏感数据直接嵌入 ETag；推荐摘要或短标识
 *
 * 常见陷阱：
 * - 表示 vs 资源：ETag标识“响应表示”，同一资源的不同编码应视为不同 ETag
 * - 弱 ETag误用：弱 ETag不保证字节一致性；不适合用在并发写保护的严格校验
 * - 碰撞与截断：理论上摘要总有碰撞风险；选择足够强的摘要（如 SHA-256）并避免过度截断
 * - 非确定性差异：不同平台换行符、序列化字段顺序、浮点格式细节，都可能导致强 ETag不同
 *
 * 选择建议：
 * - 静态资源（JS/CSS/图片）或二进制文件：使用强 ETag
 * - 模板渲染页、仅排版微调且不影响语义：使用弱 ETag
 * - 有内容变体（编码或语言）的资源：为各变体生成独立 ETag，并设置合适的 Vary
 *
 * 使用场景示例：
 * 1) 浏览器缓存验证（GET）：
 *    客户端带上上次的标签：If-None-Match: "<etag>"
 *    服务端比较：匹配则返回 304 Not Modified（不含响应体），否则返回 200 并附新 ETag
 *
 * 2) 并发控制（PUT/DELETE）：
 *    客户端携带期望版本：If-Match: "<etag>"
 *    服务端校验：若不匹配则返回 412 Precondition Failed，避免覆盖他人更新（乐观锁）
 *
 * 3) CDN/代理协同：
 *    对静态资源（含 gzip/br 变体）分别生成 ETag，并设置 Vary: Accept-Encoding
 *    代理/浏览器可精准复用缓存，减少带宽与回源压力
 *
 * 4) API 响应的条件获取：
 *    复杂 JSON 响应（分页列表、统计报表）可根据最终字节表示生成强 ETag
 *    客户端使用 If-None-Match 获取增量更新（命中返回 304）
 *
 * 5) 静态资源管线：
 *    构建时为产物计算哈希并作为 ETag；文件内容变更即更新 ETag，实现“按内容不可变”的缓存策略
 */
 public final class ETagPlusUtils {
     private ETagPlusUtils() {}

    /**
     * 强 ETag（"<sha256>"）
     *
     * 用途：基于响应字节的 SHA-256 摘要生成强 ETag；任何字节变化都会导致标签不同。
     * 适用于：静态资源（JS/CSS/图片）、二进制文件、需要严格一致性的场景。
     * 参数：
     * - data：响应字节内容；允许 null（视为空字节）
     * 返回：
     * - 以双引号包裹的强 ETag 文本，例如 "e3b0c442..."
     */
    public static String strongFromBytes(byte[] data) { return '"' + SecurityUtils.sha256Hex(data==null?new byte[0]:data) + '"'; }

    /**
     * 弱 ETag（W"<sha256>"）
     *
     * 用途：基于响应字节的 SHA-256 摘要生成弱 ETag；允许语义不变的轻微改动被视为同一版本。
     * 适用于：模板渲染页面、仅有排版/空白调整、不影响语义的动态响应。
     * 参数：
     * - data：响应字节内容；允许 null（视为空字节）
     * 返回：
     * - 以 W"..." 形式表示的弱 ETag 文本，例如 W"e3b0c442..."
     */
    public static String weakFromBytes(byte[] data) { return "W\"" + SecurityUtils.sha256Hex(data==null?new byte[0]:data) + "\""; }

    /**
     * 强 ETag（从输入流读入全部内容）
     *
     * 用途：对输入流的全部字节计算强 ETag。
     * 注意：本方法会将输入流读入内存后计算哈希，大文件建议使用 strongFromPath。
     * 参数：
     * - in：输入流；调用方负责准备可读内容
     * 返回：强 ETag 文本
     * 异常：IO 异常时抛出 IOException
     */
    public static String strongFromStream(InputStream in) throws IOException { return strongFromBytes(readAll(in)); }

    /**
     * 弱 ETag（从输入流读入全部内容）
     *
     * 用途：对输入流的全部字节计算弱 ETag。
     * 注意：本方法会将输入流读入内存后计算哈希，大文件建议使用 weakFromPath。
     * 参数/返回/异常：同 strongFromStream
     */
    public static String weakFromStream(InputStream in) throws IOException { return weakFromBytes(readAll(in)); }

    /**
     * 强 ETag（字符串按指定字符集编码）
     *
     * 用途：将字符串按给定字符集编码为字节后计算强 ETag。
     * 参数：
     * - text：文本内容；允许 null（视为空文本）
     * - cs：字符集；为 null 时使用 UTF-8
     * 返回：强 ETag 文本
     */
    public static String strongFromString(String text, Charset cs) { byte[] b=(text==null?new byte[0]:text.getBytes(cs==null?StandardCharsets.UTF_8:cs)); return strongFromBytes(b); }

    /**
     * 弱 ETag（字符串按指定字符集编码）
     *
     * 用途：将字符串按给定字符集编码为字节后计算弱 ETag。
     * 参数/返回：同 strongFromString
     */
    public static String weakFromString(String text, Charset cs) { byte[] b=(text==null?new byte[0]:text.getBytes(cs==null?StandardCharsets.UTF_8:cs)); return weakFromBytes(b); }

    /**
     * 强 ETag（文件流式计算 SHA-256，适合大文件）
     *
     * 用途：以流方式读取文件并计算强 ETag，避免一次性载入内存。
     * 参数：
     * - path：文件路径
     * 返回：强 ETag 文本
     * 异常：文件读取或摘要计算异常时抛出 IOException
     */
    public static String strongFromPath(Path path) throws IOException { return '"' + sha256HexStream(Files.newInputStream(path)) + '"'; }

    /**
     * 弱 ETag（文件流式计算 SHA-256，适合大文件）
     *
     * 用途/参数/返回/异常：同 strongFromPath，但生成弱 ETag
     */
    public static String weakFromPath(Path path) throws IOException { return "W\"" + sha256HexStream(Files.newInputStream(path)) + "\""; }

    /**
     * 基于变体键生成强 ETag（如编码/语言等）
     *
     * 用途：当同一资源根据变体（Content-Encoding/Content-Language 等）产生不同表示时，
     *       将内容字节与变体键共同参与哈希，生成各自独立的强 ETag。
     * 参数：
     * - data：响应字节内容
     * - variants：用于区分的键值（将稳定排序后参与哈希），可为空
     * 返回：强 ETag 文本
     */
    public static String strongVariantFromBytes(byte[] data, Map<String,String> variants) {
        String key = WebExtraUtils.buildCacheKey(variants==null?java.util.Collections.emptyMap():variants);
        byte[] combined = combine(data, key.getBytes(StandardCharsets.UTF_8));
        return strongFromBytes(combined);
    }

    /**
     * If-None-Match 是否命中（支持 "*"；使用弱比较）
     *
     * 用途：用于 GET/HEAD 条件请求；命中表示资源未修改，应返回 304。
     * 规则：弱比较忽略 W/ 前缀，仅比较 opaque-tag 内容；若头为 "*" 则总视为命中。
     * 参数：
     * - ifNoneMatchHeader：原始头值，形如 "W\"abc\", \"def\"" 或 "*"
     * - currentEtag：当前响应的 ETag（强或弱）
     * 返回：是否命中条件
     */
    public static boolean matchIfNoneMatch(String ifNoneMatchHeader, String currentEtag) {
        if (StringUtils.isBlank(ifNoneMatchHeader) || StringUtils.isBlank(currentEtag)) return false;
        String h = ifNoneMatchHeader.trim(); if (h.equals("*")) return true;
        for (String tag : parseEntityTagList(h)) if (weakEqual(tag, currentEtag)) return true;
        return false;
    }

    /**
     * If-Match 是否命中（支持 "*"；使用强比较）
     *
     * 用途：用于写操作（PUT/DELETE/PATCH）并发控制；未命中时应返回 412。
     * 规则：强比较要求双方均非弱标签，且 opaque-tag 内容完全相同；若头为 "*" 则总视为命中。
     * 参数同上；返回是否命中条件。
     */
    public static boolean matchIfMatch(String ifMatchHeader, String currentEtag) {
        if (StringUtils.isBlank(ifMatchHeader) || StringUtils.isBlank(currentEtag)) return false;
        String h = ifMatchHeader.trim(); if (h.equals("*")) return true;
        for (String tag : parseEntityTagList(h)) if (strongEqual(tag, currentEtag)) return true;
        return false;
    }

    /**
     * GET/HEAD 缓存命中判断：If-None-Match 命中则视为未修改（304）
     *
     * 用途：简化条件请求判断；命中时按未修改处理。
     */
    public static boolean isNotModifiedForGet(String ifNoneMatchHeader, String currentEtag) { return matchIfNoneMatch(ifNoneMatchHeader, currentEtag); }

    /**
     * 写操作并发控制：If-Match 未命中则视为前置条件失败（412）
     *
     * 用途：在资源更新/删除时保护版本一致性；未命中返回 true 表示应返回 412。
     */
    public static boolean isPreconditionFailedForWrite(String ifMatchHeader, String currentEtag) { return !StringUtils.isBlank(ifMatchHeader) && !matchIfMatch(ifMatchHeader, currentEtag); }

    private static byte[] readAll(InputStream in) throws IOException { ByteArrayOutputStream baos=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=in.read(buf))!=-1) baos.write(buf,0,n); return baos.toByteArray(); }

    /**
     * 流式计算 SHA-256（返回十六进制）
     *
     * 用途：处理大文件或大响应体，避免一次性载入内存。
     * 参数：输入流（由调用方提供）；异常处理：IO 或摘要异常统一包装为 IOException。
     */
    private static String sha256HexStream(InputStream in) throws IOException {
        try (InputStream is = in) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192]; int n; while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
            return SecurityUtils.bytesToHex(md.digest());
        } catch (Exception e) { throw new IOException(e); }
    }

    private static byte[] combine(byte[] a, byte[] b) { byte[] A=a==null?new byte[0]:a; byte[] B=b==null?new byte[0]:b; byte[] out=new byte[A.length+B.length]; System.arraycopy(A,0,out,0,A.length); System.arraycopy(B,0,out,A.length,B.length); return out; }

    /**
     * 解析实体标签列表（逗号分隔，保留引号与弱标记）
     *
     * 输入示例：W"abc", "def" 或 "*"
     * 输出：按逗号分割并修剪后的标签列表；不做合法性校验。
     */
    private static List<String> parseEntityTagList(String header) {
        String[] parts = header.split(","); List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) { String t = p.trim(); if (!t.isEmpty()) out.add(t); }
        return out;
    }

    /**
     * 弱比较：忽略弱标记（W/），仅比较不含引号的 opaque-tag 内容
     *
     * 规则参考：RFC 7232；用于 If-None-Match 等弱比较场景。
     */
    private static boolean weakEqual(String a, String b) { String oa=opaqueTag(a), ob=opaqueTag(b); return oa.equals(ob); }

    /**
     * 强比较：两者都不应为弱标签，且 opaque-tag 内容完全相同
     *
     * 规则参考：RFC 7232；用于 If-Match 场景。
     */
    private static boolean strongEqual(String a, String b) {
        if (isWeakTag(a) || isWeakTag(b)) return false; return opaqueTag(a).equals(opaqueTag(b));
    }

    private static boolean isWeakTag(String tag) { String t=tag==null?"":tag.trim(); return t.toLowerCase(Locale.ROOT).startsWith("w\"") || t.toLowerCase(Locale.ROOT).startsWith("w/"); }

    /**
     * 提取不含弱标记与引号的 opaque-tag 内容
     *
     * 输入：可能包含 W/ 前缀与双引号的标签文本
     * 输出：去除前缀与引号后的纯标签内容
     */
    private static String opaqueTag(String tag) {
        if (tag == null) return ""; String t = tag.trim();
        if (t.toLowerCase(Locale.ROOT).startsWith("w/")) t = t.substring(2);
        if (t.startsWith("\"") && t.endsWith("\"")) t = t.substring(1, t.length()-1);
        return t;
    }
}
