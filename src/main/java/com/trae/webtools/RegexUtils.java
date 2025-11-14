package com.trae.webtools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则工具（不与现有方法重复）
 *
 * 提供安全编译、匹配判断、全部分组提取、替换与保留/移除、通配符转正则、
 * 选项控制（多行/点任意）等。
 */
public final class RegexUtils {
    private RegexUtils() {}

    /** 安全编译正则（失败抛出 IllegalArgumentException） */
    public static Pattern compileSafe(String regex, int flags) { try { return Pattern.compile(regex, flags); } catch (Exception e) { throw new IllegalArgumentException("invalid regex", e); } }

    /** 是否匹配整个字符串 */
    public static boolean matches(String regex, String input) { return Pattern.compile(regex).matcher(input==null?"":input).matches(); }

    /** 查找所有匹配的第 1 个分组（若无分组则返回完整匹配） */
    public static List<String> findAllGroup1(String regex, String input) {
        List<String> out=new ArrayList<>(); Matcher m=Pattern.compile(regex).matcher(input==null?"":input); while(m.find()) out.add(m.groupCount()>=1?m.group(1):m.group(0)); return out;
    }

    /** 查找所有匹配的完整文本 */
    public static List<String> findAll(String regex, String input) { List<String> out=new ArrayList<>(); Matcher m=Pattern.compile(regex).matcher(input==null?"":input); while(m.find()) out.add(m.group(0)); return out; }

    /** 替换所有匹配为指定文本 */
    public static String replaceAll(String regex, String input, String replacement) { return Pattern.compile(regex).matcher(input==null?"":input).replaceAll(replacement==null?"":replacement); }

    /** 保留匹配部分，移除其他 */
    public static String keepOnlyMatches(String regex, String input) { List<String> all=findAll(regex,input); return String.join("", all); }

    /** 移除匹配部分，保留其他 */
    public static String removeMatches(String regex, String input) { return Pattern.compile(regex).matcher(input==null?"":input).replaceAll(""); }

    /** 将通配符表达式（* ?）转换为正则（转义其他字面量） */
    public static String wildcardToRegex(String wildcard) {
        if (wildcard==null) return null; StringBuilder sb=new StringBuilder();
        for(char c:wildcard.toCharArray()){
            if(c=='*') sb.append(".*"); else if(c=='?') sb.append('.'); else { String specials=".^$|()[]{}*+?\\"; if(specials.indexOf(c)>=0) sb.append('\\').append(c); else sb.append(c); }
        }
        return sb.toString();
    }

    /** 启用 DOTALL 模式（点号匹配换行） */
    public static Pattern compileDotAll(String regex) { return Pattern.compile(regex, Pattern.DOTALL); }

    /** 启用 MULTILINE 模式（^$ 匹配行首行尾） */
    public static Pattern compileMultiline(String regex) { return Pattern.compile(regex, Pattern.MULTILINE); }
}
