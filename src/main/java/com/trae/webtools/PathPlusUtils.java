package com.trae.webtools;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * 路径与文件名增强工具（不与现有方法重复）
 *
 * 功能概览：
 * - 路径操作：安全拼接与规范化、相对路径计算、子路径判断、共同基路径查找
 * - 文件名操作：扩展名追加/更改、非法字符清理、唯一化后缀、去扩展名
 * - 比较与转换：忽略大小写与分隔符差异的路径比较、路径到 URI 文本转换
 *
 * 使用建议：
 * - 规范化可统一混合分隔符（Windows "\\" 与 Unix "/"）并移除冗余段；跨平台时优先进行规范化再比较。
 * - 相对路径计算对不同根（盘符/挂载点）可能抛出异常；在调用前确保根一致或进行兜底处理。
 * - 文件名清理与唯一化仅处理文本层面，不涉及实际文件系统存在性检查；写入前需结合 IO 操作进行冲突判断。
 */
public final class PathPlusUtils {
    private PathPlusUtils() {}

    /**
     * 安全拼接路径（自动规范化）
     *
     * 将 base 与任意数量的子段进行拼接，并对结果进行规范化（移除 ".."、"." 与冗余斜杠）。
     * 适用场景：构造子目录或文件路径、在跨平台环境统一路径文本。
     * @param base 基路径（null 视为空）
     * @param parts 子段（null 子段视为空）
     * @return 规范化后的路径文本
     */
    public static String joinSafe(String base, String... parts) { java.nio.file.Path p=java.nio.file.Paths.get(base==null?"":base); if(parts!=null) for(String s:parts) p=p.resolve(s==null?"":s); return p.normalize().toString(); }

    /**
     * 计算相对路径（规范化）
     *
     * 将 target 相对于 base 的相对路径计算出来；两者先规范化后再进行 relativize。
     * 注意：当 base 与 target 的根不同（如不同盘符）时，可能抛出 IllegalArgumentException。
     * @param base 基路径
     * @param target 目标路径
     * @return 相对路径文本
     */
    public static String relativePath(String base, String target) { Path b=Paths.get(base).normalize(); Path t=Paths.get(target).normalize(); return b.relativize(t).toString(); }

    /**
     * 判断 target 是否位于 base 子路径（按规范化）
     *
     * 采用规范化后 {@code startsWith} 判断；不访问文件系统，不考虑符号链接与真实路径解析。
     * 适用场景：简单的路径前缀关系判断（如访问限制与路径约束）。
     * @param base 基路径
     * @param target 目标路径
     * @return 是否为子路径或自身
     */
    public static boolean isSubPath(String base, String target) { Path b=Paths.get(base).normalize(); Path t=Paths.get(target).normalize(); return t.startsWith(b); }

    /**
     * 计算共同基路径（返回规范化文本）
     *
     * 查找两个路径的最长共同前缀（逐段比较），并返回规范化文本。
     * 适用场景：在多文件路径中寻找共享根目录或基路径。
     * @param a 路径 A
     * @param b 路径 B
     * @return 共同基路径文本（可能为空）
     */
    public static String commonBasePath(String a, String b) { Path pa=Paths.get(a).normalize(); Path pb=Paths.get(b).normalize(); int n=Math.min(pa.getNameCount(), pb.getNameCount()); int i=0; while(i<n && pa.getName(i).equals(pb.getName(i))) i++; Path base=pa.getRoot()!=null?pa.getRoot():Paths.get(""); for(int k=0;k<i;k++) base=base.resolve(pa.getName(k)); return base.normalize().toString(); }

    /**
     * 若无扩展名则追加扩展名（不含点）
     * @param filename 文件名文本
     * @param ext 扩展名（不含点，允许为空）
     * @return 若已有扩展名则返回原文本，否则追加扩展名
     */
    public static String addExtensionIfMissing(String filename, String ext) { if(filename==null) return null; int idx=filename.lastIndexOf('.'); return idx>=0?filename:filename+(ext==null||ext.isEmpty()?"":"."+ext); }

    /**
     * 更改扩展名（无扩展名时追加）
     * @param filename 文件名文本
     * @param ext 新扩展名（不含点，允许为空）
     * @return 替换/追加扩展名后的文件名
     */
    public static String changeExtension(String filename, String ext) { if(filename==null) return null; int idx=filename.lastIndexOf('.'); String base=idx>=0?filename.substring(0,idx):filename; return base+(ext==null||ext.isEmpty()?"":"."+ext); }

    /**
     * 清理文件名非法字符（替换为下划线，合并重复）
     *
     * 适用场景：跨平台或 Web 下载时生成安全的文件名文本；限制最大长度为 128。
     * @param name 原始文件名
     * @return 清理后的文件名（长度可能被截断）
     */
    public static String sanitizeFilename(String name) { if(name==null) return null; String t=name.replaceAll("[^A-Za-z0-9._-]","_").replaceAll("_+","_"); return t.length()>128?t.substring(0,128):t; }

    /**
     * 唯一化文件名（追加序号后缀）
     * @param filename 基础文件名
     * @param seq 序号（如 1、2、3）
     * @return 追加 "_序号" 的文件名（保留原扩展名）
     */
    public static String ensureUniqueSuffix(String filename, int seq) { if(filename==null) return null; int idx=filename.lastIndexOf('.'); String base=idx>=0?filename.substring(0,idx):filename; String ext=idx>=0?filename.substring(idx):""; return base+"_"+seq+ext; }

    /**
     * 比较两个路径文本（不区分大小写与分隔符差异）
     *
     * 对两者进行规范化并统一为小写后比较；适合跨平台路径文本相等判断。
     * @param a 路径 A
     * @param b 路径 B
     * @return 是否相等
     */
    public static boolean equalsPathIgnoreCase(String a, String b) { if(a==null||b==null) return false; String na=IOUtils.normalizePath(a).toLowerCase(Locale.ROOT); String nb=IOUtils.normalizePath(b).toLowerCase(Locale.ROOT); return na.equals(nb); }

    /**
     * 将文件名去除扩展名部分（仅移除最后一个点之后）
     * @param filename 文件名或路径文本
     * @return 去扩展名后的文件名（保留目录与基础名）
     */
    public static String fileNameWithoutExt(String filename) { if(filename==null) return null; String name=filename.replace('\\','/'); int slash=name.lastIndexOf('/'); if(slash>=0) name=name.substring(slash+1); int dot=name.lastIndexOf('.'); return dot<0?name:name.substring(0,dot); }

    /**
     * 将路径转为 URI 文本（不对片段进行编码）
     * @param path 路径文本
     * @return URI 文本（file:// 或相对 URI）
     */
    public static String toUriString(String path) { return java.nio.file.Paths.get(path).toUri().toString(); }
}
