package com.trae.webtools;

/**
 * HTML 增强工具（不与现有方法重复）
 */
public final class HtmlPlusUtils {
    private HtmlPlusUtils() {}

    /** 简易移除注释（<!-- ... -->） */
    public static String removeComments(String html) { return html==null?null:html.replaceAll("(?s)<!--.*?-->", ""); }

    /** 移除 style 与 script 标签内容（大小写不敏感） */
    public static String removeStyleAndScript(String html) { if(html==null) return null; String t=html.replaceAll("(?is)<style[^>]*>.*?</style>", ""); t=t.replaceAll("(?is)<script[^>]*>.*?</script>", ""); return t; }

    /** 提取纯文本（移除所有标签，保留实体未解码） */
    public static String extractText(String html) { return html==null?null:html.replaceAll("(?is)<[^>]+>", ""); }

    /** 压缩多余空白（标签外空白合并） */
    public static String collapseWhitespace(String html) { return html==null?null:html.replaceAll("\\s+", " ").trim(); }

    /** 为 img 添加懒加载属性（若不存在则追加） */
    public static String addLazyLoading(String html) { return html==null?null:html.replaceAll("(?i)<img\\b(?![^>]*\\bloading=)", "<img loading=\"lazy\" "); }
}

