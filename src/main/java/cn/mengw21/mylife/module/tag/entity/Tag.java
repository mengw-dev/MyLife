package cn.mengw21.mylife.module.tag.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签实体，对应表 {@code tag}（对齐《03-数据库设计文档》3.10）
 * <p>
 * 标签通过关联表与各业务数据建立多对多关系；同一用户下标签名唯一。
 */
@Data
@TableName("tag")
public class Tag {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 */
    private Long userId;

    /** 标签名称 */
    private String name;

    /** 创建时间，插入时自动填充（UTC） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}