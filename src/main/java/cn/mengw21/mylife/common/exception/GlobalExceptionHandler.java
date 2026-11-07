package cn.mengw21.mylife.common.exception;

import cn.mengw21.mylife.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

/**
 * 全局异常处理器
 * <p>
 * 所有异常统一转成 {@link ApiResponse}，HTTP 状态码与业务 code 语义一致
 * （如 40401 → HTTP 404）。禁止将异常堆栈返回给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return build(e.getCode(), e.getMessage(), null);
    }

    /**
     * {@code @Valid} 校验失败（@RequestBody 参数）
     * <p>
     * 按《02-API接口文档》1.5 返回 data.fieldErrors 明细列表。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ParamErrorData>> handleValidationException(MethodArgumentNotValidException e) {
        List<FieldErrorVO> fieldErrors = e.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldErrorVO(error.getField(), error.getDefaultMessage()))
            .toList();
        return build(ApiResponse.PARAM_ERROR, "参数校验失败", new ParamErrorData(fieldErrors));
    }

    /** {@code @Validated} 校验失败（普通方法参数，如 @RequestParam/@PathVariable） */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        return build(ApiResponse.PARAM_ERROR, e.getMessage(), null);
    }

    /** 请求体无法解析（JSON 格式错误、类型不匹配等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        return build(ApiResponse.PARAM_ERROR, "请求体格式错误", null);
    }

    /** 上传文件超过 multipart 大小限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return build(ApiResponse.PAYLOAD_TOO_LARGE, "文件大小超过限制（最大 10MB）", null);
    }

    /** 兜底 — 未知系统异常（记录日志，不向客户端暴露细节） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return build(ApiResponse.SYSTEM_ERROR, "系统繁忙，请稍后再试", null);
    }

    /**
     * 组装响应：业务 code 前 3 位即 HTTP 状态码语义。
     * 无法解析时按 HTTP 400 处理。
     */
    private <T> ResponseEntity<ApiResponse<T>> build(int code, String message, T data) {
        HttpStatus status = HttpStatus.resolve(code / 100);
        if (status == null || !status.isError()) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(ApiResponse.fail(code, message, data));
    }

    /**
     * 字段校验错误明细（fieldErrors 元素）
     *
     * @param field   校验失败的字段名
     * @param message 校验失败原因
     */
    public record FieldErrorVO(String field, String message) {
    }

    /**
     * 参数校验失败响应数据（对齐《02-API接口文档》1.5：data 内包 fieldErrors 列表）
     *
     * @param fieldErrors 字段错误明细
     */
    public record ParamErrorData(List<FieldErrorVO> fieldErrors) {
    }
}