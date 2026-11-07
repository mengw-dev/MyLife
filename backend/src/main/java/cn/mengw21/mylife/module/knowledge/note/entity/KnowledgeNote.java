package cn.mengw21.mylife.module.knowledge.note.entity;

import cn.mengw21.mylife.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识笔记实体，对应表 {@code knowledge_note}（对齐《03-数据库设计文档》3.6）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_note")
public class KnowledgeNote extends BaseEntity {

    /** 所属用户 */
    private Long userId;

    /** 所属分类 ID */
    private Long categoryId;

    /** 标题 */
    private String title;

    /** Markdown 正文 */
    private String contentMarkdown;
}