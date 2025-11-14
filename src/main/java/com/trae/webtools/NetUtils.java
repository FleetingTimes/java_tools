package com.trae.webtools;

import java.util.ArrayList;
import java.util.List;

/**
 * 网络工具（IPv4/CIDR 相关，不与现有方法重复）
 *
 * 功能概览：
 * - IPv4 与整数互转：将点分十进制 IPv4 与 32 位无符号整数（存于 long）相互转换，便于位运算与范围比较。
 * - CIDR（Classless Inter-Domain Routing）解析：解析 "addr/prefix" 并计算网络地址、广播地址与主机总数（含网络与广播）。
 * - 地址分类判断：环回（127.0.0.0/8）、组播（224.0.0.0/4）、是否属于子网范围。
 *
 * 使用建议：
 * - 工具基于纯数学与字符串解析，不依赖 DNS 或系统网络栈；适合快速判断与生成。
 * - hostsCount 按标准计算总地址数（含网络与广播），/31、/32 的实际可用主机数需要根据具体场景另行处理。
 * - listSubnetAddresses 在大网段上可能产生巨大的列表，请谨慎使用并考虑分页或范围迭代。
 */
public final class NetUtils {
    private NetUtils() {}

    /**
     * IPv4 文本转为 32 位无符号整数（存于 long）
     * @param ip 点分十进制 IPv4（如 192.168.1.1）
     * @return 32 位无符号整数（存放于 long）
     * @throws IllegalArgumentException 文本不合法或分段不在 0-255 范围
     */
    public static long ipv4ToInt(String ip) {
        String[] parts = ip.split("\\."); if(parts.length!=4) throw new IllegalArgumentException("bad ipv4");
        long r=0; for(int i=0;i<4;i++){ int n=ConvertUtils.safeParseInt(parts[i], -1); if(n<0||n>255) throw new IllegalArgumentException("bad ipv4"); r=(r<<8)|n; }
        return r & 0xFFFFFFFFL;
    }

    /**
     * 32 位无符号整数（存于 long）转为 IPv4 文本
     * @param v 32 位无符号整数（低 32 位有效）
     * @return IPv4 文本（点分十进制）
     */
    public static String intToIpv4(long v) {
        return ((v>>24)&0xFF)+"."+((v>>16)&0xFF)+"."+((v>>8)&0xFF)+"."+(v&0xFF);
    }

    /**
     * 是否为环回地址（127.0.0.0/8）
     * @param ip IPv4 文本
     * @return 是否属于环回网段
     */
    public static boolean isIpv4Loopback(String ip) { long x=ipv4ToInt(ip); return ((x>>24)&0xFF)==127; }

    /**
     * 是否为组播地址（224.0.0.0/4）
     * @param ip IPv4 文本
     * @return 是否属于组播网段（224.0.0.0 - 239.255.255.255）
     */
    public static boolean isIpv4Multicast(String ip) { int a=(int)((ipv4ToInt(ip)>>24)&0xFF); return a>=224 && a<=239; }

    /**
     * 解析 CIDR，如 192.168.1.0/24，返回 [网络整数, 掩码位数]
     * @param cidr 文本（addr/prefix）
     * @return 长度为 2 的数组：[基地址整数，前缀位数]
     * @throws IllegalArgumentException 文本不合法或前缀不在 0-32
     */
    public static long[] parseCidr(String cidr) {
        String[] p = cidr.split("/"); if(p.length!=2) throw new IllegalArgumentException("bad cidr");
        long base = ipv4ToInt(p[0]); int bits = ConvertUtils.safeParseInt(p[1], -1); if(bits<0||bits>32) throw new IllegalArgumentException("bad cidr");
        return new long[]{base, bits};
    }

    /**
     * 计算网络地址（按掩码位数）
     * @param base 基地址整数（IPv4）
     * @param bits 前缀位数（0-32）
     * @return 网络地址整数
     */
    public static long networkAddress(long base, int bits) { long mask = bits==0?0:(0xFFFFFFFFL << (32-bits)) & 0xFFFFFFFFL; return base & mask; }

    /**
     * 计算广播地址（按掩码位数）
     * @param base 基地址整数（IPv4）
     * @param bits 前缀位数（0-32）
     * @return 广播地址整数
     */
    public static long broadcastAddress(long base, int bits) { long mask = bits==0?0:(0xFFFFFFFFL << (32-bits)) & 0xFFFFFFFFL; return (base & mask) | (~mask & 0xFFFFFFFFL); }

    /**
     * 估算主机总数（含网络与广播）
     * @param bits 前缀位数（0-32）
     * @return 地址总数（含网络与广播）
     */
    public static long hostsCount(int bits) { if(bits<0||bits>32) throw new IllegalArgumentException("bad bits"); long size = 1L << (32-bits); return size; }

    /**
     * 判断 IP 是否属于子网（包含网络与广播地址）
     * @param ip IPv4 文本
     * @param cidr 子网（addr/prefix）
     * @return 是否属于该子网范围
     */
    public static boolean inSubnet(String ip, String cidr) {
        long[] r = parseCidr(cidr); long addr = ipv4ToInt(ip); long net = networkAddress(r[0], (int)r[1]); long bc = broadcastAddress(r[0], (int)r[1]); return addr>=net && addr<=bc;
    }

    /**
     * 列举子网内所有地址（可能很大，请谨慎使用）
     * @param cidr 子网（addr/prefix）
     * @return 子网内所有地址的列表（含网络与广播）
     */
    public static List<String> listSubnetAddresses(String cidr) {
        long[] r=parseCidr(cidr); long net=networkAddress(r[0], (int)r[1]); long bc=broadcastAddress(r[0], (int)r[1]); List<String> out=new ArrayList<>();
        for(long x=net; x<=bc; x++) out.add(intToIpv4(x));
        return out;
    }
}
