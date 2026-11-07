package cn.mengw21.mylife.module.knowledge.note.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识笔记标签关联实体，对应表 {@code knowledge_note_tag}
 */
@Data
@TableName("knowledge_note_tag")
public class KnowledgeNoteTag {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 笔记 ID */
    private Long noteId;

    /** 标签 ID */
    private Long tagId;

    /** 关联时间，插入时自动填充（UTC） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}