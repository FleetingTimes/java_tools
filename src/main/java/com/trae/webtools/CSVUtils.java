package com.trae.webtools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 简易 CSV 解析与构造（RFC4180 子集）
 *
 * 支持：逗号分隔，双引号包裹字段，双引号转义为两个双引号。
 */
public final class CSVUtils {
    private CSVUtils() {}

    /** 解析一行 CSV 为字段列表 */
    public static List<String> parseCsvLine(String line) {
        if (line == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false; boolean esc = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inStr) {
                if (esc) { cur.append('"'); esc = false; }
                else if (c == '"') esc = true;
                else cur.append(c);
            } else {
                if (c == '"') inStr = true;
                else if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
                else cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    /** 解析 CSV 文本为多行字段列表 */
    public static List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        if (text == null || text.isEmpty()) return rows;
        String[] lines = text.split("\r?\n");
        for (String l : lines) rows.add(parseCsvLine(l));
        return rows;
    }

    /** 构造一行 CSV（必要时引用并转义） */
    public static String buildCsvLine(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String f : fields) {
            if (!first) sb.append(',');
            String v = f == null ? "" : f;
            boolean needQuote = v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"");
            if (needQuote) sb.append('"').append(v.replace("\"", "\"\"")).append('"'); else sb.append(v);
            first = false;
        }
        return sb.toString();
    }

    /** 构造 CSV 文本 */
    public static String buildCsv(List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        boolean firstLine = true;
        for (List<String> row : rows) {
            if (!firstLine) sb.append('\n');
            sb.append(buildCsvLine(row));
            firstLine = false;
        }
        return sb.toString();
    }

    /** 将字符串按 CSV 规则安全转义 */
    public static String escapeCsv(String s) { return buildCsvLine(java.util.Collections.singletonList(s)); }

    /** 读取 CSV 文件 */
    public static List<List<String>> readCsv(Path path, Charset cs) throws IOException { return parseCsv(IOUtils.readFileString(path, cs)); }

    /** 写入 CSV 文件 */
    public static void writeCsv(Path path, List<List<String>> rows, Charset cs) throws IOException { IOUtils.writeFileString(path, buildCsv(rows), cs); }

    /** 将 List<Map> 转为 CSV（使用首行的键作为头部） */
    public static String toCsv(List<Map<String,String>> rows) {
        if (rows == null || rows.isEmpty()) return "";
        LinkedHashSet<String> headers = new LinkedHashSet<>(rows.get(0).keySet());
        List<List<String>> out = new ArrayList<>();
        out.add(new ArrayList<>(headers));
        for (Map<String,String> row : rows) {
            List<String> line = new ArrayList<>(headers.size());
            for (String h : headers) line.add(row.getOrDefault(h, ""));
            out.add(line);
        }
        return buildCsv(out);
    }

    /** 将 CSV 文本转为 List<Map>（首行作为头部） */
    public static List<Map<String,String>> fromCsv(String text) {
        List<List<String>> rows = parseCsv(text);
        if (rows.isEmpty()) return Collections.emptyList();
        List<String> headers = rows.get(0);
        List<Map<String,String>> out = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            Map<String,String> m = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                String h = headers.get(j);
                String v = j < r.size() ? r.get(j) : "";
                m.put(h, v);
            }
            out.add(m);
        }
        return out;
    }

    /** 简单猜测分隔符（逗号/分号/制表符） */
    public static char detectCsvDelimiter(String line) {
        if (line == null) return ',';
        if (line.indexOf('\t') >= 0) return '\t';
        if (line.indexOf(';') >= 0) return ';';
        return ',';
    }
}

