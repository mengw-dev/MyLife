package cn.mengw21.mylife.common.api;

import lombok.Data;

/**
 * 全局统一响应封装（对齐《02-API接口文档》1.5 统一响应格式）
 * <p>
 * 所有 Controller 返回值都用 {@code ApiResponse<T>} 包装：
 * <pre>
 * {
 *   "code": 0,             // 0 = 成功，非 0 = 业务错误码
 *   "message": "success",
 *   "data": { }
 * }
 * </pre>
 * 业务错误码采用 5 位数字协议：前 3 位对应 HTTP 状态码语义，后 2 位为业务细分。
 * HTTP 状态码与业务 code 同时返回，前端可任选其一判断。
 *
 * @param <T> 业务数据类型
 */
@Data
public class ApiResponse<T> {

    /** 成功 */
    public static final int SUCCESS = 0;

    /** 参数校验失败（HTTP 400） */
    public static final int PARAM_ERROR = 40001;

    /** 未登录或登录已过期（HTTP 401） */
    public static final int UNAUTHORIZED = 40101;

    /** 无权操作或 CSRF 校验失败（HTTP 403） */
    public static final int FORBIDDEN = 40301;

    /** 资源不存在或不属于当前用户（HTTP 404） */
    public static final int NOT_FOUND = 40401;

    /** 业务冲突，如唯一键重复、资源被引用（HTTP 409） */
    public static final int CONFLICT = 40901;

    /** 文件大小超过限制（HTTP 413） */
    public static final int PAYLOAD_TOO_LARGE = 41301;

    /** 文件类型不支持（HTTP 415） */
    public static final int UNSUPPORTED_MEDIA_TYPE = 41501;

    /** 服务端未知异常（HTTP 500） */
    public static final int SYSTEM_ERROR = 50000;

    /** 业务错误码：0 = 成功 */
    private int code;

    /** 提示信息：成功时为 {@code success} */
    private String message;

    /** 业务数据，失败时可为 null */
    private T data;

    // ---- 成功 ----

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(SUCCESS);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(SUCCESS);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    // ---- 失败 ----

    public static <T> ApiResponse<T> fail(int code, String message) {
        return fail(code, message, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}