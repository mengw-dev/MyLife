package cn.mengw21.mylife.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求（对齐《02-API接口文档》第 2 节 PUT /auth/password）
 *
 * @param oldPassword 原密码
 * @param newPassword 新密码，8-64 位
 */
@Schema(description = "修改密码请求")
public record ChangePasswordRequest(

    @Schema(description = "原密码")
    @NotBlank(message = "原密码不能为空")
    String oldPassword,

    @Schema(description = "新密码")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度需为 8-64 位")
    String newPassword
) {
}