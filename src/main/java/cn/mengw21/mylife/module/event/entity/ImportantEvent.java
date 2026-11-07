package cn.mengw21.mylife.module.event.entity;

import cn.mengw21.mylife.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 重要事件实体，对应表 {@code important_event}（对齐《03-数据库设计文档》3.8）
 * <p>
 * 一条事件最多关联一篇日记；删除日记后事件保留，diary_id 置空。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("important_event")
public class ImportantEvent extends BaseEntity {

    /** 所属用户 */
    private Long userId;

    /** 人生阶段 ID，可空（修改时允许置空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long stageId;

    /** 关联日记 ID，可空（修改时允许置空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long diaryId;

    /** 事件标题 */
    private String title;

    /** 事件描述 */
    private String description;

    /** 事件日期 */
    private LocalDate eventDate;
}