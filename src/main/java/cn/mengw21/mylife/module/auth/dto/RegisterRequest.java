package cn.mengw21.mylife.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求（对齐《02-API接口文档》第 2 节）
 *
 * @param username 登录名，4-20 位字母、数字或下划线
 * @param password 密码，8-64 位
 * @param nickname 昵称，可选
 * @param email    邮箱，可选（系统扩展字段，V1 仅存储不使用）
 */
@Schema(description = "用户注册请求")
public record RegisterRequest(

    @Schema(description = "登录名", example = "testuser")
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名需为 4-20 位字母、数字或下划线")
    String username,

    @Schema(description = "密码（服务端 BCrypt 加密存储）", example = "12345678")
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需为 8-64 位")
    String password,

    @Schema(description = "昵称", example = "测试用户")
    @Size(max = 64, message = "昵称最长 64 字")
    String nickname,

    @Schema(description = "邮箱（扩展字段，可选）", example = "test@example.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 字")
    String email
) {
}