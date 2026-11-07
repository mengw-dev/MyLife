package cn.mengw21.mylife.module.stage.entity;

import cn.mengw21.mylife.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 人生阶段实体，对应表 {@code life_stage}（对齐《03-数据库设计文档》3.2）
 * <p>
 * 业务规则：start_date 必须 ≤ end_date；end_date 为空表示持续中；允许阶段日期重叠。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("life_stage")
public class LifeStage extends BaseEntity {

    /** 所属用户 */
    private Long userId;

    /** 阶段名称 */
    private String name;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期，可空（空表示持续中；修改时允许置空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate endDate;

    /** 阶段介绍（修改时允许清空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    /** 封面照片 ID，可空（修改时允许置空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long coverMediaId;

    /** 排序值（越小越靠前） */
    private Integer sortOrder;
}