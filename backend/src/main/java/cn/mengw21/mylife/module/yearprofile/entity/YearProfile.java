package cn.mengw21.mylife.module.yearprofile.entity;

import cn.mengw21.mylife.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 年度资料实体，对应表 {@code year_profile}（对齐《03-数据库设计文档》3.3）
 * <p>
 * 通过 user_id + year 唯一确定；没有年度资料时系统仍可通过业务数据生成年份总览。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("year_profile")
public class YearProfile extends BaseEntity {

    /** 所属用户 */
    private Long userId;

    /** 年份 */
    private Integer year;

    /** 年度标题（修改时允许清空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String title;

    /** 年度简介（修改时允许清空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;

    /** 代表照片 ID，可空（修改时允许置空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long coverMediaId;
}