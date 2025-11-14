package com.trae.webtools;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 流增强工具：复制、读取到字符串、写入字符串、GZIP 流压缩/解压
 *
 * 功能概览：
 * - 流复制：以 8KB 缓冲进行拷贝与刷新，适合通用文件/网络传输场景
 * - 文本读取/写入：按 UTF-8 将流与字符串互转
 * - GZIP 压缩/解压：以流方式进行压缩/解压，便于 HTTP/存储压缩
 *
 * 说明与建议：
 * - 资源生命周期：部分方法会关闭“被包装的”输入/输出流（见各方法注释）；调用方应明确资源管理策略，避免误关或资源泄漏。
 * - 内存与性能：大内容的读为字符串可能产生较大内存占用；生产场景优先选择流式处理或分块策略。
 * - 字符集：文本方法使用 UTF-8；跨系统/协议时需明确双方编码一致性。
 */
public final class StreamPlusUtils {
    private StreamPlusUtils() {}

    /**
     * 复制输入流到输出流
     *
     * 适用场景：文件/网络的通用数据传输、HTTP 请求/响应体代理、流式拼接管道。
     * 行为说明：使用 8KB 缓冲循环读取并写出，最后执行 {@code flush()} 确保数据落入下游；不会关闭输入或输出。
     * 资源管理：输入与输出的关闭由调用方自行控制。
     * @param in 输入流
     * @param out 输出流
     * @return 复制的字节总数
     * @throws IOException 读写失败
     */
    public static long copy(InputStream in, OutputStream out) throws IOException { byte[] buf=new byte[8192]; long total=0; int n; while((n=in.read(buf))!=-1){ out.write(buf,0,n); total+=n; } out.flush(); return total; }

    /**
     * 将输入流读取为字符串（UTF-8）
     *
     * 适用场景：小到中等体量文本的读取（如配置/模板、HTTP 文本体）；适合一次性完整加载。
     * 资源管理：读取过程中不关闭传入的输入流，由 {@code BufferedReader} 负责内部缓冲；调用方可在外部按需关闭。
     * 注意：超大内容可能造成较高内存占用，建议改为流式处理。
     * @param in 输入流
     * @return UTF-8 字符串
     * @throws IOException 读取失败
     */
    public static String readToString(InputStream in) throws IOException { try(BufferedReader br=new BufferedReader(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))){ StringBuilder sb=new StringBuilder(); char[] c=new char[4096]; int n; while((n=br.read(c))!=-1) sb.append(c,0,n); return sb.toString(); } }

    /**
     * 将字符串写入输出流（UTF-8）
     *
     * 适用场景：HTTP 响应体、文件写入、日志或缓冲区输出的字符串写入。
     * 资源管理：由于使用 {@code OutputStreamWriter} 的 try-with-resources，本方法会在写入后关闭输出流；如需保持外部输出流开放，请改为在外部管理资源或复用 {@link #copy(InputStream, OutputStream)}。
     * @param out 输出流
     * @param s 文本（null 视为空）
     * @throws IOException 写入失败
     */
    public static void writeString(OutputStream out, String s) throws IOException { try(OutputStreamWriter w=new OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8)){ if(s!=null) w.write(s); w.flush(); } }

    /**
     * 将输入流 GZIP 压缩到输出流
     *
     * 适用场景：HTTP 传输压缩（如响应体）、文件/对象存储压缩。
     * 资源管理：本方法会关闭 GZIP 包装后的输出流（随之关闭底层输出流），输入流不在此方法中关闭。
     * @param in 原始输入流
     * @param out 目标输出流（会被关闭）
     * @throws IOException 压缩或写入失败
     */
    public static void gzip(InputStream in, OutputStream out) throws IOException { try(GZIPOutputStream gos=new GZIPOutputStream(out)){ copy(in, gos); } }

    /**
     * 将输入流作为 GZIP 解压到输出流
     *
     * 适用场景：解压 HTTP/文件内容至目标流（如文件或响应体）；适合边读边写的流式解压。
     * 资源管理：本方法会关闭 GZIP 包装的输入流（随之关闭底层输入流）；输出流不会被关闭，仅执行写入与刷新由调用方负责。
     * @param in 压缩输入流
     * @param out 解压后输出流（不关闭）
     * @throws IOException 解压或写入失败
     */
    public static void gunzip(InputStream in, OutputStream out) throws IOException { try(GZIPInputStream gis=new GZIPInputStream(in)){ copy(gis, out); } }
}
