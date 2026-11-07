package cn.mengw21.mylife.module.knowledge.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建 / 修改知识笔记请求（对齐《02-API接口文档》第 6 节）
 * <p>
 * PUT 时 tagNames 与 mediaIds 表示完整替换对应关联集合。
 *
 * @param title           标题
 * @param contentMarkdown Markdown 正文
 * @param categoryId      所属分类（必须属于当前用户）
 * @param tagNames        标签名集合（完整替换）
 * @param mediaIds        相关照片 ID 集合（完整替换）
 */
@Schema(description = "创建 / 修改知识笔记请求")
public record KnowledgeNoteSaveRequest(

    @Schema(description = "标题", example = "Java 线程池")
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 字")
    String title,

    @Schema(description = "Markdown 正文", example = "# Java 线程池\n\n线程池的核心参数...")
    @NotBlank(message = "正文不能为空")
    String contentMarkdown,

    @Schema(description = "所属分类 ID", example = "10")
    @NotNull(message = "所属分类不能为空")
    Long categoryId,

    @Schema(description = "标签名集合（完整替换）", example = "[\"Java\",\"并发\"]")
    @Size(max = 20, message = "标签最多 20 个")
    List<String> tagNames,

    @Schema(description = "相关照片 ID 集合（完整替换）")
    @Size(max = 50, message = "照片最多 50 张")
    List<Long> mediaIds
) {
}