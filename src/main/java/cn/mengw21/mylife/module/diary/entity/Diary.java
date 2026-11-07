package cn.mengw21.mylife.module.diary.entity;

import cn.mengw21.mylife.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 日记实体，对应表 {@code diary}（对齐《03-数据库设计文档》3.4）
 * <p>
 * 同一天允许多篇日记；日记日期由用户指定，修改日期后时间线自动调整归属。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("diary")
public class Diary extends BaseEntity {

    /** 所属用户 */
    private Long userId;

    /** 人生阶段 ID，可空（修改时允许置空，MyBatis-Plus 默认忽略 null 更新，需覆盖策略） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long stageId;

    /** 标题 */
    private String title;

    /** 正文 */
    private String content;

    /** 记录日期 */
    private LocalDate recordDate;
}