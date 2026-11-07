package cn.mengw21.mylife.module.diary.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日记照片关联实体，对应表 {@code diary_media}
 */
@Data
@TableName("diary_media")
public class DiaryMedia {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 日记 ID */
    private Long diaryId;

    /** 照片 ID */
    private Long mediaId;

    /** 关联时间，插入时自动填充（UTC） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}