package cn.mengw21.mylife.common.exception;

import cn.mengw21.mylife.common.api.ApiResponse;
import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 业务校验失败统一抛这个异常，由 {@code GlobalExceptionHandler} 转成
 * 对应 HTTP 状态码 + 统一响应体（业务 code 与 HTTP 状态码语义一致）。
 * <p>
 * 使用示例：
 * <pre>
 * throw new BusinessException(ApiResponse.NOT_FOUND, "日记不存在");
 * throw new BusinessException(ApiResponse.CONFLICT, "照片仍被引用，无法删除");
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码（协议见 {@link ApiResponse}） */
    private final int code;

    /**
     * 默认按参数错误（40001）抛出。
     */
    public BusinessException(String message) {
        this(ApiResponse.PARAM_ERROR, message);
    }

    /**
     * @param code    业务错误码
     * @param message 对用户可读的提示信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}