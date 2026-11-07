package cn.mengw21.mylife.module.event.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件照片关联实体，对应表 {@code event_media}
 */
@Data
@TableName("event_media")
public class EventMedia {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件 ID */
    private Long eventId;

    /** 照片 ID */
    private Long mediaId;

    /** 关联时间，插入时自动填充（UTC） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}