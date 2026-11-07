package cn.mengw21.mylife.module.knowledge.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建 / 修改知识分类请求（对齐《02-API接口文档》第 5 节）
 *
 * @param name      分类名称
 * @param parentId  父分类 ID，可空（空为根分类）
 * @param sortOrder 排序值
 */
@Schema(description = "创建 / 修改知识分类请求")
public record KnowledgeCategorySaveRequest(

    @Schema(description = "分类名称", example = "Java")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称最长 100 字")
    String name,

    @Schema(description = "父分类 ID（空为根分类）", example = "1")
    Long parentId,

    @Schema(description = "排序值，越小越靠前", example = "1")
    Integer sortOrder
) {
}