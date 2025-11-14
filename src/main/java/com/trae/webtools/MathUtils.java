package com.trae.webtools;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/**
 * 数学工具：数论与统计，均为纯 Java 实现
 */
public final class MathUtils {
    private MathUtils() {}

    /** 最大公约数（非负） */
    public static long gcd(long a, long b) { a=Math.abs(a); b=Math.abs(b); while (b!=0){ long t=a%b; a=b; b=t; } return a; }

    /** 最小公倍数（非负） */
    public static long lcm(long a, long b) { if (a==0||b==0) return 0; return Math.abs(a / gcd(a,b) * b); }

    /** 是否为素数（简易试除） */
    public static boolean isPrime(long n) {
        if (n<2) return false; if (n%2==0) return n==2; for (long i=3;i*i<=n;i+=2) if (n%i==0) return false; return true;
    }

    /** 下一个素数（>=n） */
    public static long nextPrime(long n) { long x=Math.max(2,n); while (!isPrime(x)) x++; return x; }

    /** 阶乘（BigInteger） */
    public static BigInteger factorial(int n) { if (n<0) throw new IllegalArgumentException("n<0"); BigInteger r=BigInteger.ONE; for(int i=2;i<=n;i++) r=r.multiply(BigInteger.valueOf(i)); return r; }

    /** 组合数 nCr（BigInteger） */
    public static BigInteger nCr(int n, int r) { if(r<0||r>n) return BigInteger.ZERO; r=Math.min(r,n-r); BigInteger num=BigInteger.ONE,den=BigInteger.ONE; for(int i=1;i<=r;i++){ num=num.multiply(BigInteger.valueOf(n-r+i)); den=den.multiply(BigInteger.valueOf(i)); } return num.divide(den); }

    /** 排列数 nPr（BigInteger） */
    public static BigInteger nPr(int n, int r) { if(r<0||r>n) return BigInteger.ZERO; BigInteger res=BigInteger.ONE; for(int i=0;i<r;i++) res=res.multiply(BigInteger.valueOf(n-i)); return res; }

    /** 幂模计算（快速幂） */
    public static long powMod(long a, long e, long mod) { if (mod<=0) throw new IllegalArgumentException("mod<=0"); long r=1%mod; a%=mod; while(e>0){ if((e&1)==1) r=(r*a)%mod; a=(a*a)%mod; e>>=1; } return r; }

    /** 模逆（扩展欧几里得），若不存在返回 0 */
    public static long modInverse(long a, long mod) { long t=0, newT=1, r=mod, newR=a%mod; while(newR!=0){ long q=r/newR; long tmp=t-q*newT; t=newT; newT=tmp; tmp=r-q*newR; r=newR; newR=tmp; } if(r>1) return 0; if(t<0) t+=mod; return t; }

    /** 样本方差（n-1） */
    public static double variance(List<Double> list) {
        if (list==null||list.size()<2) return Double.NaN; double mean=0; for(Double d:list) mean += d==null?0.0:d; mean/=list.size(); double s=0; for(Double d:list){ double v=(d==null?0.0:d)-mean; s+=v*v; } return s/(list.size()-1);
    }

    /** 样本标准差（n-1） */
    public static double stdDev(List<Double> list) { double v = variance(list); return Double.isNaN(v)?Double.NaN:Math.sqrt(v); }

    /** 百分位（线性插值，p∈[0,100]） */
    public static double percentile(List<Double> list, double p) {
        if (list==null||list.isEmpty()) return Double.NaN; List<Double> a=new ArrayList<>(); for(Double d:list)a.add(d==null?0.0:d); Collections.sort(a); double idx=(p/100.0)*(a.size()-1); int lo=(int)Math.floor(idx), hi=(int)Math.ceil(idx); if(lo==hi) return a.get(lo); double t=idx-lo; return a.get(lo)*(1-t)+a.get(hi)*t;
    }

    /** 众数（出现次数最多的值） */
    public static Double mode(List<Double> list) {
        if (list==null||list.isEmpty()) return null; Map<Double,Integer> m=new LinkedHashMap<>(); for(Double d:list){ double v=d==null?0.0:d; m.put(v,m.getOrDefault(v,0)+1);} Double best=null; int bc=0; for(Map.Entry<Double,Integer> e:m.entrySet()){ if(e.getValue()>bc){ bc=e.getValue(); best=e.getKey(); } } return best;
    }

    /** BigDecimal 钳制到区间 */
    public static BigDecimal clampBigDecimal(BigDecimal v, BigDecimal min, BigDecimal max) { if(min.compareTo(max)>0) throw new IllegalArgumentException("min>max"); if(v.compareTo(min)<0) return min; if(v.compareTo(max)>0) return max; return v; }

    /** BigDecimal 平均值（向半舍入到指定小数位） */
    public static BigDecimal averageBigDecimal(List<BigDecimal> list, int scale) {
        if (list==null||list.isEmpty()) return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        BigDecimal sum=BigDecimal.ZERO; for(BigDecimal b:list) sum=sum.add(b==null?BigDecimal.ZERO:b);
        return sum.divide(BigDecimal.valueOf(list.size()), scale, RoundingMode.HALF_UP);
    }

    /** BigDecimal 求和 */
    public static BigDecimal sumBigDecimal(List<BigDecimal> list) { BigDecimal s=BigDecimal.ZERO; if(list!=null) for(BigDecimal b:list) s=s.add(b==null?BigDecimal.ZERO:b); return s; }

    /** BigDecimal 中位数 */
    public static BigDecimal medianBigDecimal(List<BigDecimal> list, int scale) {
        if(list==null||list.isEmpty()) return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        List<BigDecimal> a=new ArrayList<>(); for(BigDecimal b:list) a.add(b==null?BigDecimal.ZERO:b); a.sort(Comparator.naturalOrder());
        int n=a.size(); if((n&1)==1) return a.get(n/2).setScale(scale, RoundingMode.HALF_UP);
        BigDecimal s=a.get(n/2-1).add(a.get(n/2)); return s.divide(BigDecimal.valueOf(2), scale, RoundingMode.HALF_UP);
    }

    /** 线性插值：在 [x0,x1] 上根据 t∈[0,1] 插值 */
    public static double linearInterpolate(double x0, double x1, double t) { return x0*(1-t)+x1*t; }

    /** 将值归一化到 [0,1] （钳制） */
    public static double normalizeRange(double v, double min, double max) { if(min==max) return 0.0; return Math.max(0.0, Math.min(1.0, (v-min)/(max-min))); }

    /** 将值从 [a,b] 映射到 [c,d] */
    public static double scaleRange(double v, double a, double b, double c, double d) { double t=normalizeRange(v,a,b); return linearInterpolate(c,d,t); }
}

