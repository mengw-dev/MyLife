package cn.mengw21.mylife.module.auth.vo;

import cn.mengw21.mylife.module.auth.entity.User;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户信息视图（对齐《02-API接口文档》第 2 节当前用户响应）
 * <p>
 * 绝不包含密码哈希；ID 按约定输出字符串避免前端数字精度问题。
 *
 * @param id        用户 ID（字符串）
 * @param username  登录名
 * @param nickname  昵称
 * @param createdAt 注册时间（UTC ISO-8601）
 */
@Schema(description = "用户信息视图")
public record UserVO(

    @Schema(description = "用户 ID", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,

    @Schema(description = "登录名", example = "testuser")
    String username,

    @Schema(description = "昵称", example = "测试用户")
    String nickname,

    @Schema(description = "注册时间", example = "2026-11-06T10:00:00Z")
    LocalDateTime createdAt
) {
    /**
     * 实体 → VO（内部转换，密码等敏感字段不出现在这里）
     */
    public static UserVO from(User user) {
        return new UserVO(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getCreatedAt()
        );
    }
}