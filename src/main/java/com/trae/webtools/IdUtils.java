package com.trae.webtools;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 随机 ID 与随机数工具
 */
public final class IdUtils {
    private IdUtils() {}

    /** 生成随机 UUID（带连字符） */
    public static String uuid() { return UUID.randomUUID().toString(); }

    /** 生成指定长度的字母数字随机串 */
    public static String randomAlphaNum(int len) {
        final String dict = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(dict.charAt(r.nextInt(dict.length())));
        return sb.toString();
    }

    /** 生成随机整数（闭区间） */
    public static int randomInt(int minInclusive, int maxInclusive) {
        if (minInclusive > maxInclusive) throw new IllegalArgumentException("min>max");
        return new SecureRandom().nextInt(maxInclusive - minInclusive + 1) + minInclusive;
    }
}

