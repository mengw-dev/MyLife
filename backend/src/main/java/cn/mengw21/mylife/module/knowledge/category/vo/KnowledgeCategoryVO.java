package cn.mengw21.mylife.module.knowledge.category.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 知识分类视图（树形节点）
 *
 * @param id        分类 ID（字符串）
 * @param name      分类名称
 * @param parentId  父分类 ID
 * @param sortOrder 排序值
 * @param children  子分类（按 sortOrder 排序）
 */
@Schema(description = "知识分类视图（树形）")
public record KnowledgeCategoryVO(

    @Schema(description = "分类 ID", example = "10")
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,

    @Schema(description = "分类名称", example = "Java")
    String name,

    @Schema(description = "父分类 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    Long parentId,

    @Schema(description = "排序值")
    Integer sortOrder,

    @Schema(description = "子分类列表")
    List<KnowledgeCategoryVO> children
) {
}