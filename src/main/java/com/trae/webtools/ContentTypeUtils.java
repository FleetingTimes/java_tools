package com.trae.webtools;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Content-Type 工具：解析、构造与匹配
 */
public final class ContentTypeUtils {
    private ContentTypeUtils() {}

    /** 模型 */
    public static final class ContentType { public final String type; public final String subtype; public final String charset; public ContentType(String type,String subtype,String charset){ this.type=type; this.subtype=subtype; this.charset=charset; } }

    /** 解析 Content-Type（如 application/json; charset=UTF-8） */
    public static ContentType parse(String ct) {
        if (StringUtils.isBlank(ct)) return new ContentType("application","octet-stream", null);
        String[] parts = ct.split(";",2); String[] ts = parts[0].trim().toLowerCase(Locale.ROOT).split("/"); String type=ts.length>0?ts[0]:"application"; String subtype=ts.length>1?ts[1]:"octet-stream";
        String charset=null; if(parts.length>1){ String[] kv=parts[1].trim().split("=",2); if(kv.length==2 && kv[0].trim().equalsIgnoreCase("charset")) charset=kv[1].trim(); }
        return new ContentType(type, subtype, charset);
    }

    /** 构造 Content-Type（可选 charset） */
    public static String build(String type, String subtype, Charset cs) { String base=(type==null?"application":type)+"/"+(subtype==null?"octet-stream":subtype); return cs==null?base:(base+"; charset="+cs.name()); }

    /** 是否匹配（支持全通配与类型通配，如任意类型、type/*） */
    public static boolean matches(String ct, String pattern) {
        ContentType a=parse(ct); ContentType p=parse(pattern);
        boolean typeOk = p.type.equals("*") || a.type.equals(p.type);
        boolean subOk = p.subtype.equals("*") || a.subtype.equals(p.subtype);
        return typeOk && subOk;
    }

    /** 获取字符集（若未声明则返回 UTF-8 作为默认） */
    public static Charset getCharsetOrUtf8(String ct) { ContentType c=parse(ct); return c.charset==null?StandardCharsets.UTF_8:Charset.forName(c.charset); }
}
