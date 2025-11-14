package com.trae.webtools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件增强工具：字节/行范围读取，原子写入，比较与备份等（不与现有 IO/FileTree 重复）
 */
public final class FilePlusUtils {
    private FilePlusUtils() {}

    /** 读取指定字节范围（offset 起，长度 length；自动钳制） */
    public static byte[] readRangeBytes(Path path, long offset, int length) throws IOException {
        byte[] data=Files.readAllBytes(path); int n=data.length; if(offset<0) offset=0; if(length<0) length=0; int start=(int)Math.min(Math.max(0,offset), n); int end=Math.min(start+length, n); byte[] out=new byte[end-start]; System.arraycopy(data, start, out, 0, out.length); return out;
    }

    /** 读取行范围（startLine 起，读取 count 行；自动钳制） */
    public static List<String> readLinesRange(Path path, int startLine, int count) throws IOException { List<String> all=Files.readAllLines(path); int n=all.size(); int s=Math.max(0,startLine); int e=Math.min(n, s+Math.max(0,count)); return new ArrayList<>(all.subList(s,e)); }

    /** 追加多行（UTF-8） */
    public static void appendLines(Path path, List<String> lines) throws IOException { IOUtils.ensureDirectory(path.getParent()); Files.write(path, (lines==null?java.util.Collections.emptyList():lines), java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); }

    /** 读取前 N 字节 */
    public static byte[] headBytes(Path path, int n) throws IOException { byte[] data=Files.readAllBytes(path); n=Math.max(0,Math.min(n,data.length)); byte[] out=new byte[n]; System.arraycopy(data,0,out,0,n); return out; }

    /** 读取后 N 字节 */
    public static byte[] tailBytes(Path path, int n) throws IOException { byte[] data=Files.readAllBytes(path); n=Math.max(0,Math.min(n,data.length)); byte[] out=new byte[n]; System.arraycopy(data,data.length-n,out,0,n); return out; }

    /** 原子写入（写入临时文件再移动替换目标） */
    public static void atomicWrite(Path path, byte[] data) throws IOException { IOUtils.ensureDirectory(path.getParent()); Path tmp=java.nio.file.Files.createTempFile("aw",".tmp"); Files.write(tmp, data==null?new byte[0]:data); Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }

    /** 若内容变化才写入（返回是否发生写入） */
    public static boolean writeIfChanged(Path path, byte[] data) throws IOException { byte[] old=Files.exists(path)?Files.readAllBytes(path):null; if(old!=null && java.util.Arrays.equals(old, data)) return false; atomicWrite(path, data); return true; }

    /** 安全移动（确保父目录存在） */
    public static void safeMove(Path src, Path dst) throws IOException { IOUtils.ensureDirectory(dst.getParent()); Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING); }

    /** 比较两个文件内容是否一致 */
    public static boolean contentEquals(Path a, Path b) throws IOException { byte[] A=Files.readAllBytes(a); byte[] B=Files.readAllBytes(b); return java.util.Arrays.equals(A,B); }

    /** 计算文件 SHA-256（十六进制） */
    public static String checksumSha256(Path path) throws IOException { return SecurityUtils.sha256Hex(Files.readAllBytes(path)); }

    /** 计算文件 SHA-1（十六进制） */
    public static String checksumSha1(Path path) throws IOException { return SecurityUtils.sha1Hex(Files.readAllBytes(path)); }

    /** 确保父目录存在 */
    public static void ensureParent(Path path) throws IOException { IOUtils.ensureDirectory(path.getParent()); }

    /** 备份文件（追加 .bak 序号） */
    public static Path backupFile(Path path, int seq) throws IOException { Path bak=path.resolveSibling(path.getFileName().toString()+".bak"+seq); safeMove(path, bak); return bak; }

    /** 恢复备份（bak 覆盖原文件） */
    public static void restoreFile(Path bak, Path orig) throws IOException { safeMove(bak, orig); }

    /** 读取文件为十六进制字符串 */
    public static String readHex(Path path) throws IOException { return SecurityUtils.bytesToHex(Files.readAllBytes(path)); }

    /** 将十六进制写入文件 */
    public static void writeHex(Path path, String hex) throws IOException { atomicWrite(path, SecurityUtils.hexToBytes(hex)); }

    /** 判断文件是否为空（存在但大小为 0） */
    public static boolean isEmptyFile(Path path) throws IOException { return Files.exists(path) && Files.size(path)==0; }

    /** 若存在则读取，否则返回 null */
    public static byte[] readIfExists(Path path) throws IOException { return Files.exists(path)?Files.readAllBytes(path):null; }
}

