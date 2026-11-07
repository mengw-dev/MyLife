package cn.mengw21.mylife.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类
 * <p>
 * 所有含审计字段的业务实体继承它，统一获得主键与审计字段。
 * 数据库表需要有对应的列：id、created_at、updated_at（DATETIME(3)）。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;Data
 * &#64;EqualsAndHashCode(callSuper = true)
 * &#64;TableName("diary")
 * public class Diary extends BaseEntity {
 *     private String title;
 * }
 * </pre>
 * 注意：子类必须加 {@code @EqualsAndHashCode(callSuper = true)}，
 * 否则 Lombok 会忽略父类字段，equals/hashCode 不完整。
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键，数据库自增（BIGINT UNSIGNED）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间，插入时由 MyMetaObjectHandler 自动填充（UTC）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间，插入和更新时由 MyMetaObjectHandler 自动填充（UTC）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}