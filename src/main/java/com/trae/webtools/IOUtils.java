package com.trae.webtools;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.*;

/**
 * IO 与文件、路径、压缩/解压工具
 */
public final class IOUtils {
    private IOUtils() {}

    /** 文件路径拼接（不创建目录，仅字符串处理） */
    public static String pathJoin(String... parts) {
        if (parts == null || parts.length == 0) return "";
        Path p = Paths.get(parts[0]);
        for (int i = 1; i < parts.length; i++) p = p.resolve(parts[i]);
        return p.normalize().toString();
    }

    /** 读取文本文件为字符串 */
    public static String readFileString(Path path, Charset cs) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, cs);
    }

    /** 将字符串写入文本文件（覆盖） */
    public static void writeFileString(Path path, String content, Charset cs) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(path, cs)) { bw.write(content); }
    }

    /** 追加字符串到文本文件（不存在则创建） */
    public static void appendFileString(Path path, String content, Charset cs) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(path, cs, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) { bw.write(content); }
    }

    /** 将输入流完整读为字符串（UTF-8） */
    public static String streamToString(InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) != -1) sb.append(buf, 0, n);
            return sb.toString();
        }
    }

    /** 拷贝输入到输出（不关闭流） */
    public static long copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) { out.write(buf, 0, n); total += n; }
        return total;
    }

    /** 安静关闭 Closeable（忽略异常） */
    public static void closeQuietly(Closeable c) { if (c != null) try { c.close(); } catch (Exception ignored) {} }

    /** 将异常堆栈转为字符串 */
    public static String stackTraceToString(Throwable t) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter w = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            w.write(t.toString()); w.write("\n");
            for (StackTraceElement e : t.getStackTrace()) { w.write("\tat "); w.write(e.toString()); w.write("\n"); }
            w.flush();
            return baos.toString("UTF-8");
        } catch (IOException e) { return t.toString(); }
    }

    /** 规范化路径文本（替换分隔符，去重斜杠） */
    public static String normalizePath(String path) {
        if (path == null) return null;
        String p = path.replace('\\', '/');
        p = p.replaceAll("/+", "/");
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length()-1);
        return p;
    }

    /** 判断文件是否存在 */
    public static boolean fileExists(Path path) { return path != null && Files.exists(path); }

    /** 确保目录存在（不存在则创建） */
    public static void ensureDirectory(Path dir) throws IOException { if (dir != null) Files.createDirectories(dir); }

    /** 安静删除文件（忽略异常） */
    public static void deleteQuietly(Path path) { if (path != null) try { Files.deleteIfExists(path); } catch (Exception ignored) {} }

    /** 复制文件（覆盖目标） */
    public static void copyFile(Path src, Path dst) throws IOException { ensureDirectory(dst.getParent()); Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }

    /** 读取文件字节 */
    public static byte[] readBytes(Path path) throws IOException { return Files.readAllBytes(path); }

    /** 写入文件字节（覆盖） */
    public static void writeBytes(Path path, byte[] data) throws IOException { ensureDirectory(path.getParent()); Files.write(path, data); }

    /** 检测 UTF-8 BOM */
    public static boolean charsetDetectUtf8Bom(byte[] data) { return data != null && data.length >= 3 && (data[0]&0xFF)==0xEF && (data[1]&0xFF)==0xBB && (data[2]&0xFF)==0xBF; }

    /** GZIP 压缩字节 */
    public static byte[] gzipCompress(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); GZIPOutputStream gos = new GZIPOutputStream(baos)) { gos.write(data); gos.finish(); return baos.toByteArray(); } catch (IOException e) { throw new RuntimeException(e); }
    }

    /** GZIP 解压字节 */
    public static byte[] gzipDecompress(byte[] data) {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data)); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096]; int n; while ((n = gis.read(buf)) != -1) baos.write(buf, 0, n); return baos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    /** Deflate 压缩字节 */
    public static byte[] deflateCompress(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(Deflater.DEFAULT_COMPRESSION, true))) { dos.write(data); dos.finish(); return baos.toByteArray(); } catch (IOException e) { throw new RuntimeException(e); }
    }

    /** Deflate 解压字节 */
    public static byte[] deflateDecompress(byte[] data) {
        try (InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(data)); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096]; int n; while ((n = iis.read(buf)) != -1) baos.write(buf, 0, n); return baos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    /** 获取文件扩展名（不包含点） */
    public static String fileExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx+1);
    }

    /** 获取文件基础名（去除路径与扩展名） */
    public static String fileBasename(String filename) {
        if (filename == null) return null;
        String name = filename.replace('\\','/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash+1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    /** 获取文件所在目录（标准化斜杠） */
    public static String fileDirname(String filename) {
        if (filename == null) return null;
        String name = filename.replace('\\','/');
        int slash = name.lastIndexOf('/');
        return slash < 0 ? "" : name.substring(0, slash);
    }

    /** 列出目录下文件（简单后缀匹配，glob 为空列出全部） */
    public static java.util.List<Path> listFiles(Path dir, String suffix) throws IOException {
        java.util.List<Path> out = new java.util.ArrayList<>();
        if (dir == null) return out;
        try (java.nio.file.DirectoryStream<Path> ds = java.nio.file.Files.newDirectoryStream(dir)) {
            for (Path p : ds) if (suffix==null || p.getFileName().toString().endsWith(suffix)) out.add(p);
        }
        return out;
    }

    /** 创建临时文件（系统临时目录） */
    public static Path tempFile(String prefix, String suffix) throws IOException { return java.nio.file.Files.createTempFile(prefix, suffix); }

    /** 移动文件（覆盖目标） */
    public static void moveFile(Path src, Path dst) throws IOException { ensureDirectory(dst.getParent()); java.nio.file.Files.move(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }

    /** 从类路径读取资源为字符串（UTF-8） */
    public static String readResourceAsString(String resourcePath) throws IOException {
        try (InputStream in = IOUtils.class.getClassLoader().getResourceAsStream(resourcePath)) { if (in==null) throw new IOException("resource not found: "+resourcePath); return streamToString(in); }
    }

    /** 判断类路径资源是否存在 */
    public static boolean resourceExists(String resourcePath) { return IOUtils.class.getClassLoader().getResource(resourcePath) != null; }

    /** 计算 CRC32 校验 */
    public static long crc32(byte[] data) {
        java.util.zip.CRC32 c = new java.util.zip.CRC32(); c.update(data); return c.getValue();
    }

    /** 文件 MD5 摘要（十六进制） */
    public static String checksumMD5(Path path) throws IOException { return SecurityUtils.md5Hex(java.nio.file.Files.readAllBytes(path)); }
}
