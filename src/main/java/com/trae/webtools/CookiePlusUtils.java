package com.trae.webtools;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cookie 增强工具：SameSite、构造与解析
 */
public final class CookiePlusUtils {
    private CookiePlusUtils() {}

    /** 构造 Set-Cookie，支持 SameSite (Lax/Strict/None) */
    public static String buildSetCookieWithSameSite(String name, String value, String path, String domain, int maxAgeSeconds, boolean httpOnly, boolean secure, String sameSite) {
        StringBuilder sb=new StringBuilder(); sb.append(name).append('=').append(value==null?"":value);
        if (!StringUtils.isBlank(path)) sb.append("; Path=").append(path);
        if (!StringUtils.isBlank(domain)) sb.append("; Domain=").append(domain);
        if (maxAgeSeconds >= 0) sb.append("; Max-Age=").append(maxAgeSeconds);
        if (secure) sb.append("; Secure"); if (httpOnly) sb.append("; HttpOnly");
        if (!StringUtils.isBlank(sameSite)) sb.append("; SameSite=").append(sameSite);
        return sb.toString();
    }

    /** 解析 Set-Cookie（基础键值，忽略属性大小写） */
    public static Map<String,String> parseSetCookie(String setCookie) {
        Map<String,String> m=new LinkedHashMap<>(); if(StringUtils.isBlank(setCookie)) return m;
        String[] parts=setCookie.split(";\\s*");
        String[] kv=parts[0].split("=",2); m.put(kv[0], kv.length>1?kv[1]:"");
        for(int i=1;i<parts.length;i++){ String p=parts[i]; int idx=p.indexOf('='); if(idx<0) m.put(p.toLowerCase(java.util.Locale.ROOT), ""); else m.put(p.substring(0,idx).toLowerCase(java.util.Locale.ROOT), p.substring(idx+1)); }
        return m;
    }
}

