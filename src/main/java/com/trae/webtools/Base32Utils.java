package com.trae.webtools;

/**
 * Base32 编解码（RFC4648）
 *
 * 特性：
 * - 使用标准字母表（A-Z 2-7，大写），不包含分隔符与空格
 * - 编码不使用填充（=）；解码时忽略填充与空白
 * - 面向整段数据的简易实现，适合短标识与配置编码
 *
 * 注意：
 * - 与 Base32 Hex（另一个字母表）不兼容
 * - 若需与他语言库交互，请确认是否使用填充与大小写规范
 */
public final class Base32Utils {
    private Base32Utils() {}

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    /**
     * 编码字节为 Base32 文本
     * @param data 原始字节数组
     * @return Base32 文本（无填充）
     */
    public static String encode(byte[] data) {
        if (data == null || data.length == 0) return "";
        StringBuilder sb = new StringBuilder((data.length*8+4)/5);
        int buffer=0, bitsLeft=0;
        for (byte b : data) {
            buffer = (buffer<<8) | (b & 0xFF); bitsLeft += 8;
            while (bitsLeft >= 5) { int idx=(buffer>>(bitsLeft-5)) & 0x1F; sb.append(ALPHABET[idx]); bitsLeft -= 5; }
        }
        if (bitsLeft > 0) { int idx=(buffer<<(5-bitsLeft)) & 0x1F; sb.append(ALPHABET[idx]); }
        return sb.toString();
    }

    /**
     * 解码 Base32 文本为字节
     * @param text Base32 文本（大小写不敏感；解码时忽略空白与 = ）
     * @return 原始字节数组
     */
    public static byte[] decode(String text) {
        if (text == null || text.isEmpty()) return new byte[0];
        String t = text.replace("=", "").replaceAll("\\s+", "").toUpperCase(java.util.Locale.ROOT);
        int buffer=0, bitsLeft=0; java.io.ByteArrayOutputStream baos=new java.io.ByteArrayOutputStream();
        for (int i=0;i<t.length();i++) { int v=indexOf(t.charAt(i)); if (v<0) continue; buffer=(buffer<<5)|v; bitsLeft+=5; if(bitsLeft>=8){ baos.write((buffer>>(bitsLeft-8)) & 0xFF); bitsLeft-=8; } }
        return baos.toByteArray();
    }

    private static int indexOf(char c) { if(c>='A'&&c<='Z') return c-'A'; if(c>='2'&&c<='7') return c-'2'+26; return -1; }
}
