package com.trae.webtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统计增强工具：针对整数/长整型列表的统计与分布计算
 */
public final class StatsPlusUtils {
    private StatsPlusUtils() {}

    /** 最小值（null 按 0 计算；空列表返回 0） */
    public static int minInt(List<Integer> list) { if(list==null||list.isEmpty()) return 0; int m=Integer.MAX_VALUE; for(Integer v:list){ int x=v==null?0:v; if(x<m) m=x; } return m; }

    /** 最大值（null 按 0 计算；空列表返回 0） */
    public static int maxInt(List<Integer> list) { if(list==null||list.isEmpty()) return 0; int m=Integer.MIN_VALUE; for(Integer v:list){ int x=v==null?0:v; if(x>m) m=x; } return m; }

    /** 求和（null 按 0 计算） */
    public static long sumInt(List<Integer> list) { long s=0; if(list!=null) for(Integer v:list) s += (v==null?0:v); return s; }

    /** 平均值（空列表返回 NaN） */
    public static double avgInt(List<Integer> list) { if(list==null||list.isEmpty()) return Double.NaN; return sumInt(list)/(double)list.size(); }

    /** 方差（样本 n-1） */
    public static double varianceInt(List<Integer> list) { if(list==null||list.size()<2) return Double.NaN; double mean=avgInt(list); double s=0; for(Integer v:list){ double x=v==null?0:v; s+= (x-mean)*(x-mean); } return s/(list.size()-1); }

    /** 标准差（样本 n-1） */
    public static double stdDevInt(List<Integer> list) { double v=varianceInt(list); return Double.isNaN(v)?Double.NaN:Math.sqrt(v); }

    /** 中位数（空返回 0） */
    public static int medianInt(List<Integer> list) { if(list==null||list.isEmpty()) return 0; List<Integer> a=new ArrayList<>(); for(Integer v:list) a.add(v==null?0:v); Collections.sort(a); int n=a.size(); return (n&1)==1?a.get(n/2):(a.get(n/2-1)+a.get(n/2))/2; }

    /** 百分位（线性插值；p∈[0,100]） */
    public static double percentileInt(List<Integer> list, double p) { if(list==null||list.isEmpty()) return Double.NaN; List<Integer> a=new ArrayList<>(); for(Integer v:list) a.add(v==null?0:v); Collections.sort(a); double idx=(p/100.0)*(a.size()-1); int lo=(int)Math.floor(idx), hi=(int)Math.ceil(idx); if(lo==hi) return a.get(lo); double t=idx-lo; return a.get(lo)*(1-t)+a.get(hi)*t; }

    /** 直方图统计（返回值到频次的映射） */
    public static java.util.Map<Integer,Integer> histogramInt(List<Integer> list) { java.util.Map<Integer,Integer> m=new java.util.LinkedHashMap<>(); if(list!=null) for(Integer v:list){ int x=v==null?0:v; m.put(x, m.getOrDefault(x,0)+1); } return m; }

    /** 去除异常值（超出均值±k*std 的值剔除） */
    public static List<Integer> removeOutliers(List<Integer> list, double k) { List<Integer> out=new ArrayList<>(); if(list==null||list.isEmpty()) return out; double mean=avgInt(list); double sd=stdDevInt(list); for(Integer v:list){ double x=v==null?0:v; if(Double.isNaN(sd) || Math.abs(x-mean)<=k*sd) out.add((int)x); } return out; }
}

