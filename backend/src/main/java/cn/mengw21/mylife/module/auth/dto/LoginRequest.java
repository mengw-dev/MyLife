package cn.mengw21.mylife.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求（对齐《02-API接口文档》第 2 节）
 *
 * @param username 登录名
 * @param password 密码
 */
@Schema(description = "用户登录请求")
public record LoginRequest(

    @Schema(description = "登录名", example = "testuser")
    @NotBlank(message = "用户名不能为空")
    String username,

    @Schema(description = "密码", example = "12345678")
    @NotBlank(message = "密码不能为空")
    String password
) {
}