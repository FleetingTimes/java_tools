package com.trae.webtools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 文件树工具：遍历、统计、查找、读写、复制/移动/删除等
 */
public final class FileTreeUtils {
    private FileTreeUtils() {}

    /** 递归列出目录下的所有文件（不包含目录） */
    public static List<Path> walkRecursive(Path dir) throws IOException {
        List<Path> out = new ArrayList<>(); if(dir==null) return out;
        Files.walk(dir).filter(Files::isRegularFile).forEach(out::add); return out;
    }

    /** 统计扩展名出现次数（小写，无点） */
    public static Map<String,Integer> countByExtension(Path dir) throws IOException {
        Map<String,Integer> m = new LinkedHashMap<>(); for(Path p:walkRecursive(dir)){ String name=p.getFileName().toString().toLowerCase(Locale.ROOT); int idx=name.lastIndexOf('.'); String ext=idx<0?"":name.substring(idx+1); m.put(ext, m.getOrDefault(ext,0)+1); } return m;
    }

    /** 计算目录中文件总大小（字节） */
    public static long totalSize(Path dir) throws IOException { long s=0; for(Path p:walkRecursive(dir)) s+=Files.size(p); return s; }

    /** 按正则查找文件名（返回匹配文件） */
    public static List<Path> findByRegex(Path dir, String regex) throws IOException { List<Path> out=new ArrayList<>(); java.util.regex.Pattern pat=java.util.regex.Pattern.compile(regex); for(Path p:walkRecursive(dir)){ String n=p.getFileName().toString(); if(pat.matcher(n).matches()) out.add(p);} return out; }

    /** 复制目录树到目标（覆盖） */
    public static void copyTree(Path src, Path dst) throws IOException { IOUtils.ensureDirectory(dst); Files.walk(src).forEach(p->{ try{ Path t=dst.resolve(src.relativize(p)); if(Files.isDirectory(p)) IOUtils.ensureDirectory(t); else Files.copy(p,t,StandardCopyOption.REPLACE_EXISTING);}catch(IOException e){ throw new RuntimeException(e);} }); }

    /** 移动目录树到目标（覆盖） */
    public static void moveTree(Path src, Path dst) throws IOException { IOUtils.ensureDirectory(dst); Files.walk(src).forEach(p->{ try{ Path t=dst.resolve(src.relativize(p)); if(Files.isDirectory(p)) IOUtils.ensureDirectory(t); else Files.move(p,t,StandardCopyOption.REPLACE_EXISTING);}catch(IOException e){ throw new RuntimeException(e);} }); }

    /** 删除目录树（安全，忽略异常） */
    public static void deleteTree(Path dir) { if(dir==null) return; try{ Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(p->{ try{ Files.deleteIfExists(p);}catch(IOException ignore){} }); } catch(IOException ignore){} }

    /** 查找修改时间晚于指定时间的文件 */
    public static List<Path> findFilesModifiedAfter(Path dir, long millis) throws IOException { List<Path> out=new ArrayList<>(); for(Path p:walkRecursive(dir)) if(Files.getLastModifiedTime(p).toMillis()>millis) out.add(p); return out; }

    /** 查找空目录（无子文件与子目录） */
    public static List<Path> findEmptyDirs(Path dir) throws IOException {
        List<Path> out = new ArrayList<>();
        if (dir == null) return out;
        Files.walk(dir).filter(Files::isDirectory).forEach(d -> {
            try (java.util.stream.Stream<Path> s = Files.list(d)) {
                boolean empty = !s.findAny().isPresent();
                if (empty) out.add(d);
            } catch (IOException ignore) {}
        });
        return out;
    }

    /** 删除空目录（返回删除数量） */
    public static int deleteEmptyDirs(Path dir) throws IOException { int c=0; for(Path d:findEmptyDirs(dir)) { try{ Files.deleteIfExists(d); c++; }catch(Exception ignore){} } return c; }

    /** 确保文件后缀（若无则追加） */
    public static Path ensureFileSuffix(Path path, String suffix) { String n=path.getFileName().toString(); if(!n.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) path=path.resolveSibling(n+suffix); return path; }

    /** 确保文件前缀（若无则追加） */
    public static Path ensureFilePrefix(Path path, String prefix) { String n=path.getFileName().toString(); if(!n.startsWith(prefix)) path=path.resolveSibling(prefix+n); return path; }

    /** 若文件不存在则创建空文件 */
    public static void touchFile(Path path) throws IOException { IOUtils.ensureDirectory(path.getParent()); if(!Files.exists(path)) Files.write(path, new byte[0]); }

    /** 读取所有行（UTF-8） */
    public static List<String> readLines(Path path) throws IOException { return Files.readAllLines(path, StandardCharsets.UTF_8); }

    /** 写入行（UTF-8，覆盖） */
    public static void writeLines(Path path, List<String> lines) throws IOException { IOUtils.ensureDirectory(path.getParent()); Files.write(path, lines, StandardCharsets.UTF_8); }

    /** 读取文件尾部 N 行（UTF-8） */
    public static List<String> tailLines(Path path, int n) throws IOException { List<String> a=readLines(path); return n>=a.size()?a:new ArrayList<>(a.subList(a.size()-n,a.size())); }

    /** 读取文件头部 N 行（UTF-8） */
    public static List<String> headLines(Path path, int n) throws IOException { List<String> a=readLines(path); return n>=a.size()?a:new ArrayList<>(a.subList(0,n)); }

    /** 统计文件行数 */
    public static int countLines(Path path) throws IOException { try(java.io.BufferedReader br=Files.newBufferedReader(path, StandardCharsets.UTF_8)){ int c=0; while(br.readLine()!=null) c++; return c; } }

    /** 按 MD5 检测重复文件（返回 MD5 -> 文件列表） */
    public static Map<String,List<Path>> findDuplicatesByChecksum(Path dir) throws IOException { Map<String,List<Path>> m=new LinkedHashMap<>(); for(Path p:walkRecursive(dir)){ if(Files.isRegularFile(p)){ String md5=IOUtils.checksumMD5(p); m.computeIfAbsent(md5,k->new ArrayList<>()).add(p); } } return m; }

    /** 基于模式重命名文件（只改文件名） */
    public static void renameByPattern(Path dir, java.util.function.Function<String,String> fn) throws IOException { for(Path p:walkRecursive(dir)){ String n=p.getFileName().toString(); String nn=fn.apply(n); if(!n.equals(nn)) Files.move(p, p.resolveSibling(nn)); } }
}
