package com.trae.webtools;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Zip 工具：压缩/解压、条目读取与写入、列表与判断
 */
public final class ZipUtils {
    private ZipUtils() {}

    /** 将目录压缩为 zip 文件（保留相对结构） */
    public static void zipDir(Path dir, Path zipFile) throws IOException {
        IOUtils.ensureDirectory(zipFile.getParent());
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walk(dir).forEach(p -> {
                try {
                    if (Files.isDirectory(p)) return;
                    String entryName = dir.relativize(p).toString().replace('\\','/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(p, zos);
                    zos.closeEntry();
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
        }
    }

    /** 将多个文件压缩为 zip 文件（根下平铺） */
    public static void zipFiles(List<Path> files, Path zipFile) throws IOException {
        IOUtils.ensureDirectory(zipFile.getParent());
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (Path p : files) {
                String name = p.getFileName().toString();
                zos.putNextEntry(new ZipEntry(name));
                Files.copy(p, zos);
                zos.closeEntry();
            }
        }
    }

    /** 解压 zip 到目录（覆盖） */
    public static void unzipToDir(Path zipFile, Path dir) throws IOException {
        IOUtils.ensureDirectory(dir);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry e; byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                Path out = dir.resolve(e.getName());
                IOUtils.ensureDirectory(out.getParent());
                try (OutputStream os = Files.newOutputStream(out)) {
                    int n; while ((n = zis.read(buf)) != -1) os.write(buf, 0, n);
                }
                zis.closeEntry();
            }
        }
    }

    /** 列出 zip 条目名称 */
    public static List<String> listZipEntries(Path zipFile) throws IOException {
        List<String> out = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry e; while ((e = zis.getNextEntry()) != null) { out.add(e.getName()); zis.closeEntry(); }
        }
        return out;
    }

    /** 向 zip 添加文件（若存在则覆盖） */
    public static void addFileToZip(Path zipFile, Path file, String entryName) throws IOException {
        Path tmp = Files.createTempFile("zip", ".tmp");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile)); ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tmp))) {
            ZipEntry e; byte[] buf = new byte[8192]; Set<String> names = new HashSet<>();
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals(entryName)) { zis.closeEntry(); continue; }
                zos.putNextEntry(new ZipEntry(e.getName())); int n; while ((n = zis.read(buf)) != -1) zos.write(buf, 0, n); zos.closeEntry(); zis.closeEntry(); names.add(e.getName());
            }
            zos.putNextEntry(new ZipEntry(entryName)); Files.copy(file, zos); zos.closeEntry();
        }
        Files.move(tmp, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /** 从 zip 提取指定条目为字节 */
    public static byte[] extractFileFromZip(Path zipFile, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile)); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ZipEntry e; byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals(entryName)) { int n; while ((n = zis.read(buf)) != -1) baos.write(buf,0,n); break; }
                zis.closeEntry();
            }
            return baos.toByteArray();
        }
    }

    /** 判断文件是否为 zip（检查前两个字节 PK） */
    public static boolean isZipFile(Path zipFile) throws IOException { byte[] b=Files.readAllBytes(zipFile); return b.length>=2 && b[0]=='P' && b[1]=='K'; }

    /** 将字节作为条目追加到 zip 文件 */
    public static void zipBytesToEntry(Path zipFile, String entryName, byte[] data) throws IOException {
        Path tmp = Files.createTempFile("zip", ".tmp");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile)); ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tmp))) {
            ZipEntry e; byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals(entryName)) { zis.closeEntry(); continue; }
                zos.putNextEntry(new ZipEntry(e.getName())); int n; while ((n = zis.read(buf)) != -1) zos.write(buf, 0, n); zos.closeEntry(); zis.closeEntry();
            }
            zos.putNextEntry(new ZipEntry(entryName)); zos.write(data); zos.closeEntry();
        }
        Files.move(tmp, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /** 删除 zip 中的指定条目 */
    public static void removeEntry(Path zipFile, String entryName) throws IOException {
        Path tmp = Files.createTempFile("zip", ".tmp");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile)); ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tmp))) {
            ZipEntry e; byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals(entryName)) { zis.closeEntry(); continue; }
                zos.putNextEntry(new ZipEntry(e.getName())); int n; while ((n = zis.read(buf)) != -1) zos.write(buf, 0, n); zos.closeEntry(); zis.closeEntry();
            }
        }
        Files.move(tmp, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /** 替换 zip 中的指定条目内容 */
    public static void replaceEntry(Path zipFile, String entryName, byte[] data) throws IOException { zipBytesToEntry(zipFile, entryName, data); }

    /** 复制 zip 文件 */
    public static void copyZip(Path src, Path dst) throws IOException { IOUtils.ensureDirectory(dst.getParent()); Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING); }

    /** zip 条目数 */
    public static int entriesCount(Path zipFile) throws IOException { return listZipEntries(zipFile).size(); }

    /** 是否包含指定条目 */
    public static boolean hasEntry(Path zipFile, String entryName) throws IOException { return listZipEntries(zipFile).contains(entryName); }

    /** 计算 zip 总字节大小（所有条目压缩后大小之和） */
    public static long calculateZipSize(Path zipFile) throws IOException { long s=0; try(ZipInputStream zis=new ZipInputStream(Files.newInputStream(zipFile))){ ZipEntry e; while((e=zis.getNextEntry())!=null){ s+=Math.max(0,e.getCompressedSize()); zis.closeEntry(); } } return s; }

    /** 过滤压缩目录（只压缩满足条件的文件） */
    public static void zipDirWithFilter(Path dir, Path zipFile, java.util.function.Predicate<Path> filter) throws IOException {
        IOUtils.ensureDirectory(zipFile.getParent());
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walk(dir).forEach(p -> {
                try {
                    if (Files.isDirectory(p)) return; if(filter!=null && !filter.test(p)) return;
                    String entryName = dir.relativize(p).toString().replace('\\','/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(p, zos);
                    zos.closeEntry();
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
        }
    }

    /** 选择性解压（仅解压满足条件的条目） */
    public static void unzipSelective(Path zipFile, Path dir, java.util.function.Predicate<String> filter) throws IOException {
        IOUtils.ensureDirectory(dir);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry e; byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName(); if(filter!=null && !filter.test(name)) { zis.closeEntry(); continue; }
                Path out = dir.resolve(name); IOUtils.ensureDirectory(out.getParent()); try (OutputStream os = Files.newOutputStream(out)) { int n; while ((n = zis.read(buf)) != -1) os.write(buf, 0, n); }
                zis.closeEntry();
            }
        }
    }

    /** 流式遍历 zip 条目（回调处理） */
    public static void streamZipEntries(Path zipFile, java.util.function.Consumer<String> consumer) throws IOException { try(ZipInputStream zis=new ZipInputStream(Files.newInputStream(zipFile))){ ZipEntry e; while((e=zis.getNextEntry())!=null){ consumer.accept(e.getName()); zis.closeEntry(); } } }
}

