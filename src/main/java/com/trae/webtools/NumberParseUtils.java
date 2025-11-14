package com.trae.webtools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * 数值解析与判断工具（不与现有 NumberUtils 重复）
 */
public final class NumberParseUtils {
    private NumberParseUtils() {}

    /** 解析百分数字符串（如 "34.5%" -> 34.5；无%时按原值） */
    public static double parsePercent(String s, double def) { if(s==null) return def; String t=s.trim(); try{ if(t.endsWith("%")) return Double.parseDouble(t.substring(0,t.length()-1)); return Double.parseDouble(t); }catch(Exception e){ return def; } }

    /** 解析人类可读大小到字节（如 "10KB"、"1.5MB"、"2G"） */
    public static long parseHumanSize(String s, long def) { if(s==null) return def; String t=s.trim().toUpperCase(Locale.ROOT); try{ double v=Double.parseDouble(t.replaceAll("[^0-9.]+","")); long m=1; if(t.contains("TB")) m=1L<<40; else if(t.contains("GB")||t.endsWith("G")) m=1L<<30; else if(t.contains("MB")||t.endsWith("M")) m=1L<<20; else if(t.contains("KB")||t.endsWith("K")) m=1L<<10; else m=1; return (long)(v*m); }catch(Exception e){ return def; } }

    /** 解析十六进制为整数（前缀可选 0x） */
    public static int parseHexInt(String s, int def) { if(s==null) return def; String t=s.trim(); if(t.startsWith("0x")||t.startsWith("0X")) t=t.substring(2); try{ return Integer.parseUnsignedInt(t, 16); }catch(Exception e){ return def; } }

    /** 解析无符号长整型（十进制） */
    public static long parseUnsignedLong(String s, long def) { if(s==null) return def; try{ return Long.parseUnsignedLong(s.trim()); }catch(Exception e){ return def; } }

    /** 尝试解析 BigDecimal（失败返回默认值） */
    public static BigDecimal parseBigDecimal(String s, BigDecimal def) { if(s==null) return def; try{ return new BigDecimal(s.trim()); }catch(Exception e){ return def; } }

    /** 判断是否整数文本 */
    public static boolean isIntegerString(String s) { return s!=null && s.matches("^[+-]?\\d+$"); }

    /** 判断是否小数文本 */
    public static boolean isDecimalString(String s) { return s!=null && s.matches("^[+-]?\\d*(?:\\.\\d+)?$"); }

    /** 默认转换 int */
    public static int toIntDefault(String s, int def) { return ConvertUtils.safeParseInt(s, def); }

    /** 默认转换 long */
    public static long toLongDefault(String s, long def) { return ConvertUtils.safeParseLong(s, def); }

    /** 默认转换 double */
    public static double toDoubleDefault(String s, double def) { return ConvertUtils.safeParseDouble(s, def); }

    /** 安全比较（返回 -1/0/1） */
    public static int safeCompare(double a, double b) { return Double.compare(a, b); }

    /** 判断是否在区间（闭区间） */
    public static boolean inRange(double v, double min, double max) { if(min>max) return false; return v>=min && v<=max; }

    /** 近似相等（误差 eps） */
    public static boolean nearEquals(double a, double b, double eps) { return Math.abs(a-b) <= Math.max(0.0, eps); }

    /** 求和（int 列表） */
    public static int sumInts(java.util.List<Integer> list) { int s=0; if(list!=null) for(Integer i:list) s += (i==null?0:i); return s; }

    /** 平均值（int 列表，保留小数位 scale） */
    public static BigDecimal avgInts(java.util.List<Integer> list, int scale) { if(list==null||list.isEmpty()) return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP); BigDecimal sum=BigDecimal.ZERO; for(Integer i:list) sum=sum.add(BigDecimal.valueOf(i==null?0:i)); return sum.divide(BigDecimal.valueOf(list.size()), scale, RoundingMode.HALF_UP); }

    /** 中位数（int 列表） */
    public static int medianInts(java.util.List<Integer> list) { if(list==null||list.isEmpty()) return 0; java.util.List<Integer> a=new java.util.ArrayList<>(); for(Integer i:list) a.add(i==null?0:i); java.util.Collections.sort(a); int n=a.size(); return (n&1)==1?a.get(n/2):(a.get(n/2-1)+a.get(n/2))/2; }

    /** 求百分比（numerator/denominator*100；分母为 0 返回 0） */
    public static double percentOf(double numerator, double denominator) { return denominator==0.0?0.0:(numerator/denominator)*100.0; }
}

