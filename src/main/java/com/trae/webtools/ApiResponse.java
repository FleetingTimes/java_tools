package com.trae.webtools;

/**
 * 统一 API 响应包装
 *
 * 用途：
 * - 在后端接口（REST/RPC）中使用统一的响应结构，便于前端/客户端解析与错误处理。
 * - 对不同业务返回值进行标准化：以 `success` 表示是否成功，以 `message` 承载提示或错误信息，以 `data` 承载具体业务数据。
 *
 * 设计说明：
 * - 类型参数 `T`：表示业务数据的类型（如 DTO、列表、分页结果等）。
 * - 不可变对象：所有字段均为 `final`，构造完成后不可变，有利于线程安全与序列化一致性。
 * - 空值策略：
 *   - 成功场景可将 `message` 置为 `null` 或空字符串，避免重复信息；
 *   - 失败场景通常将 `data` 置为 `null`（或携带错误上下文），并在 `message` 中提供可读提示。
 *
 * 典型使用：
 * - 成功：new ApiResponse<>(true, null, userDto)
 * - 失败：new ApiResponse<>(false, "参数格式错误", null)
 *
 * HTTP 映射建议：
 * - 成功（2xx）：`success=true`，`message` 可空，`data` 承载结果
 * - 业务失败（4xx/5xx）：`success=false`，`message` 填写原因，`data` 可空
 * - 条件响应（304/412 等）：通常不返回业务数据；若需要亦可使用该结构向上层传达原因
 *
 * 序列化与文案：
 * - 文案 `message` 建议使用面向用户的自然语言；若需多语言支持，可在服务端或客户端侧进行 i18n 转换。
 * - 结构适用于 JSON/ProtoBuf 等常见序列化；字段为 `final` 可保证序列化稳定性。
 */
public final class ApiResponse<T> {
    /** 是否成功：true 表示业务成功，false 表示业务失败 */
    public final boolean success;

    /** 提示或错误信息：成功时可为空，失败时应填写可读的原因说明 */
    public final String message;

    /** 业务数据载体：成功时承载具体数据，失败时通常为 null（或携带错误上下文） */
    public final T data;

    /**
     * 构造函数
     * @param success 是否成功
     * @param message 提示或错误信息（成功可为空）
     * @param data    业务数据（失败通常为空）
     */
    public ApiResponse(boolean success, String message, T data) { this.success=success; this.message=message; this.data=data; }
}
