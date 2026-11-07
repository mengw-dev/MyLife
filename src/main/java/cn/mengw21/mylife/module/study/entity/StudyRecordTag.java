package cn.mengw21.mylife.module.study.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习记录标签关联实体，对应表 {@code study_record_tag}
 */
@Data
@TableName("study_record_tag")
public class StudyRecordTag {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学习记录 ID */
    private Long recordId;

    /** 标签 ID */
    private Long tagId;

    /** 关联时间，插入时自动填充（UTC） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}