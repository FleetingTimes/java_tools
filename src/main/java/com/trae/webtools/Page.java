package com.trae.webtools;

import java.util.List;

/**
 * 分页结果模型
 *
 * 字段含义：
 * - {@link #page} 当前页号（从 1 开始）
 * - {@link #size} 每页大小（>0）
 * - {@link #total} 总记录数（>=0）
 * - {@link #pages} 总页数（>=0）
 * - {@link #items} 当前页数据列表（可能为空，但不为 null）
 *
 * 约定：当 {@code total==0} 时通常 {@code pages==0} 且 {@code items} 为空集合。
 */
public final class Page<T> {
    public final int page;
    public final int size;
    public final int total;
    public final int pages;
    public final List<T> items;
    /**
     * 构造分页结果
     * @param page 当前页号（从 1 开始）
     * @param size 每页大小
     * @param total 总记录数
     * @param pages 总页数
     * @param items 当前页数据列表
     */
    public Page(int page, int size, int total, int pages, List<T> items) { this.page=page; this.size=size; this.total=total; this.pages=pages; this.items=items; }
}
