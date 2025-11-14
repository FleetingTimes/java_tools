package com.trae.webtools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * 轻量模板工具：{{key}} 替换、严格模式、键抽取等
 *
 * 工具定位：
 * - 提供以 {{key}} 为占位符的轻量级字符串模板渲染功能，无外部依赖，适合配置生成、通知文案、页面片段等场景。
 * - 不支持条件语句/循环/过滤器等高级语法，强调简单、可控与零学习成本。
 *
 * 基本语法：
 * - 占位符：使用双大括号包裹键名，例如 {{name}}、{{title}}；键名默认由字母/数字/下划线/短横组成。
 * - 渲染策略：
 *   1) render：仅替换已知键，未知键保留原样，便于容忍不完整数据；
 *   2) renderStrict：严格模式，若存在未替换键则抛出 IllegalArgumentException；
 *   3) renderIgnoreUnknown：等同于 render，作为语义清晰的别名；
 * - 转义：如需在模板中保留字面量大括号，可使用 {@link #escapeBraces(String)} 进行转义处理。
 * - 嵌套：{@link #renderNestedOnce(String, Map)} 支持进行二次渲染，用于值中包含占位符的简单嵌套情况。
 *
 * 用途与需求场景：
 * - 配置与部署：将环境变量或参数替换进应用配置文件、部署脚本（文件读写见 renderToFile/renderFromFile）。
 * - 通知与邮件：以 Map 驱动文案模板，快速生成带个性化字段的文本。
 * - 页面片段与静态资源：构建简单的片段内容或静态页面占位替换。
 * - CLI/脚手架：从模板生成初始文件内容，结合默认值与严格模式保障完整性。
 *
 * 注意与局限：
 * - 占位符键命名应保持规范与唯一，推荐使用小写蛇形或短横风格，并可通过 {@link #normalizeKey(String)} 统一。
 * - 渲染值通过 String.valueOf 进行转换，复杂类型请先格式化（见 renderWithFormatter）。
 * - 不提供模板安全过滤，请在上游确保内容安全（如 HTML/SQL 场景需显式转义），避免注入风险。
 * - 性能：render 采用逐键替换，模板与键数量较大时会产生多次字符串复制，建议控制键规模或分段处理。
 */
public final class TemplateUtils {
    private TemplateUtils() {}

    /**
     * 简单渲染：将 {{key}} 替换为 Map 值（未知键保留原样）
     * @param tpl 模板文本
     * @param data 键值映射（键为模板中出现的占位符名称）
     * @return 渲染结果（未匹配的占位符将原样保留）
     */
    public static String render(String tpl, Map<String,?> data) {
        if (tpl==null) return null; if (data==null||data.isEmpty()) return tpl;
        String out = tpl;
        for (Map.Entry<String,?> e : data.entrySet()) {
            String k = e.getKey(); String v = String.valueOf(e.getValue());
            out = out.replace("{{"+k+"}}", v);
        }
        return out;
    }

    /**
     * 严格渲染：若存在未替换键则抛异常
     * @param tpl 模板文本
     * @param data 键值映射
     * @return 渲染结果（所有占位符均被替换）
     * @throws IllegalArgumentException 当存在未解析的占位符时抛出
     */
    public static String renderStrict(String tpl, Map<String,?> data) {
        String out = render(tpl, data);
        Set<String> unresolved = extractKeys(out);
        if (!unresolved.isEmpty()) throw new IllegalArgumentException("unresolved keys: "+unresolved);
        return out;
    }

    /**
     * 提取模板中的键集合（如 {{name}} -> name）
     * @param tpl 模板文本
     * @return 键集合（去重、保留出现顺序）
     */
    public static Set<String> extractKeys(String tpl) {
        Set<String> keys = new LinkedHashSet<>(); if (tpl==null) return keys;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{\\{([A-Za-z0-9_\\-]+)\\}\\}").matcher(tpl);
        while (m.find()) keys.add(m.group(1));
        return keys;
    }

    /** 判断模板是否包含所有指定键 */
    public static boolean containsAllKeys(String tpl, Collection<String> keys) { Set<String> k=extractKeys(tpl); return k.containsAll(keys); }

    /**
     * 在渲染时为缺失键设置默认值（缺失键替换为默认）
     * @param tpl 模板文本
     * @param defaultValue 缺失时替换的默认值
     * @return 渲染结果
     */
    public static String substituteDefault(String tpl, String defaultValue) {
        Set<String> ks = extractKeys(tpl); Map<String,String> m = new LinkedHashMap<>(); for(String k:ks) m.put(k, defaultValue);
        return render(tpl, m);
    }

    /**
     * 使用格式化函数渲染（值先经格式化函数处理）
     * @param tpl 模板文本
     * @param data 原始数据映射
     * @param formatter 格式化函数（null 则使用 String::valueOf）
     * @return 渲染结果
     */
    public static String renderWithFormatter(String tpl, Map<String,?> data, Function<Object,String> formatter) {
        if (formatter==null) formatter = String::valueOf; Map<String,String> m = new LinkedHashMap<>();
        if (data!=null) for(Map.Entry<String,?> e:data.entrySet()) m.put(e.getKey(), formatter.apply(e.getValue()));
        return render(tpl, m);
    }

    /**
     * 安全转义模板中的大括号
     * @param tpl 模板文本
     * @return 转义后的文本（大括号转为字面量）
     */
    public static String escapeBraces(String tpl) { return tpl==null?null:tpl.replace("{","\\{").replace("}","\\}"); }

    /** 将键列表转为集合（去重保持顺序） */
    public static Set<String> toKeySet(List<String> keys) { return new LinkedHashSet<>(keys==null?Collections.emptyList():keys); }

    /** 规范化键（小写、去空白） */
    public static String normalizeKey(String key) { return key==null?null:key.trim().toLowerCase(java.util.Locale.ROOT); }

    /** 渲染但忽略未知键（保留未识别占位符原样） */
    public static String renderIgnoreUnknown(String tpl, Map<String,?> data) { return render(tpl, data); }

    /**
     * 使用备用 Map 渲染（缺失键从 fallback 获取）
     * @param tpl 模板文本
     * @param primary 主映射
     * @param fallback 备用映射（主映射缺失时使用）
     * @return 渲染结果
     */
    public static String renderWithFallback(String tpl, Map<String,?> primary, Map<String,?> fallback) {
        Set<String> ks = extractKeys(tpl); Map<String,Object> m = new LinkedHashMap<>();
        for(String k:ks){ Object v = primary!=null?primary.get(k):null; if(v==null && fallback!=null) v=fallback.get(k); m.put(k, v==null?"":v); }
        return render(tpl, m);
    }

    /**
     * 渲染一次嵌套：若值中包含 {{x}} 再渲染一轮
     * @param tpl 模板文本
     * @param data 键值映射
     * @return 进行两轮渲染后的结果（适合简单嵌套占位符）
     */
    public static String renderNestedOnce(String tpl, Map<String,?> data) { String out=render(tpl,data); return render(out,data); }

    /** 是否存在未解析键 */
    public static boolean hasUnresolvedKeys(String tpl) { return !extractKeys(tpl).isEmpty(); }

    /**
     * 渲染失败则抛异常
     * @param tpl 模板文本
     * @param data 键值映射
     * @return 渲染结果
     * @throws IllegalArgumentException 当存在未解析的占位符时抛出
     */
    public static String renderOrThrow(String tpl, Map<String,?> data) { String out=render(tpl,data); if(hasUnresolvedKeys(out)) throw new IllegalArgumentException("unresolved keys"); return out; }

    /**
     * 渲染到文件（UTF-8，覆盖）
     * @param tpl 模板文本
     * @param data 键值映射
     * @param path 目标文件路径
     * @throws IOException 写入失败
     */
    public static void renderToFile(String tpl, Map<String,?> data, Path path) throws IOException { String out=render(tpl,data); IOUtils.writeFileString(path,out,StandardCharsets.UTF_8); }

    /**
     * 从文件读取模板并渲染（UTF-8）
     * @param path 模板文件路径
     * @param data 键值映射
     * @return 渲染结果
     * @throws IOException 读取失败
     */
    public static String renderFromFile(Path path, Map<String,?> data) throws IOException { String tpl=IOUtils.readFileString(path,StandardCharsets.UTF_8); return render(tpl,data); }

    /** 使用环境变量渲染（{{KEY}} 替换为 env 值） */
    public static String renderWithEnv(String tpl) { Map<String,String> env = System.getenv(); return render(tpl, env); }

    /** 将 Map 键转为大写后渲染 */
    public static String uppercaseKeys(String tpl, Map<String,?> data) {
        Map<String,Object> m=new LinkedHashMap<>(); if(data!=null) for(Map.Entry<String,?> e:data.entrySet()) m.put(e.getKey().toUpperCase(java.util.Locale.ROOT), e.getValue());
        return render(tpl,m);
    }

    /** 将 Map 键转为小写后渲染 */
    public static String lowercaseKeys(String tpl, Map<String,?> data) {
        Map<String,Object> m=new LinkedHashMap<>(); if(data!=null) for(Map.Entry<String,?> e:data.entrySet()) m.put(e.getKey().toLowerCase(java.util.Locale.ROOT), e.getValue());
        return render(tpl,m);
    }
}
