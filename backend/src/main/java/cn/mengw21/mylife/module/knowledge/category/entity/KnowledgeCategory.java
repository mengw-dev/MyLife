package cn.mengw21.mylife.module.knowledge.category.entity;

import cn.mengw21.mylife.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识分类实体，对应表 {@code knowledge_category}（对齐《03-数据库设计文档》3.5）
 * <p>
 * 通过 parent_id 实现多级嵌套；存在子分类或知识笔记时不允许直接删除。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_category")
public class KnowledgeCategory extends BaseEntity {

    /** 所属用户 */
    private Long userId;

    /** 父分类 ID，可空（空为根分类，修改时允许置空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 排序值（越小越靠前） */
    private Integer sortOrder;
}