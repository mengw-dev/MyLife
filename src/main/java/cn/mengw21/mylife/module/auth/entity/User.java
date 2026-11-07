package cn.mengw21.mylife.module.auth.entity;

import cn.mengw21.mylife.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 {@code t_user}（对齐《03-数据库设计文档》3.1）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    /** 正常状态 */
    public static final int STATUS_ACTIVE = 1;

    /** 注销 / 停用状态 */
    public static final int STATUS_DISABLED = 0;

    /** 登录名，唯一 */
    private String username;

    /** BCrypt 密文，任何接口都不允许返回 */
    private String passwordHash;

    /** 昵称 */
    private String nickname;

    /** 邮箱（扩展字段） */
    private String email;

    /** 用户状态：1 正常 / 0 注销或停用 */
    private Integer status;

    /** 最后登录时间（UTC） */
    private LocalDateTime lastLoginAt;
}