package cn.mengw21.mylife.module.knowledge.note.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识笔记视图
 *
 * @param id            笔记 ID（字符串）
 * @param title         标题
 * @param contentMarkdown Markdown 正文
 * @param categoryId    所属分类 ID
 * @param tagNames      标签名集合
 * @param mediaIds      相关照片 ID 集合
 * @param createdAt     创建时间（UTC）
 * @param updatedAt     修改时间（UTC）
 */
@Schema(description = "知识笔记视图")
public record KnowledgeNoteVO(

    @Schema(description = "笔记 ID", example = "100")
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,

    @Schema(description = "标题", example = "Java 线程池")
    String title,

    @Schema(description = "Markdown 正文")
    String contentMarkdown,

    @Schema(description = "所属分类 ID", example = "10")
    @JsonSerialize(using = ToStringSerializer.class)
    Long categoryId,

    @Schema(description = "标签名集合")
    List<String> tagNames,

    @Schema(description = "相关照片 ID 集合")
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    List<Long> mediaIds,

    @Schema(description = "创建时间", example = "2026-11-06T10:00:00Z")
    LocalDateTime createdAt,

    @Schema(description = "修改时间", example = "2026-11-06T10:00:00Z")
    LocalDateTime updatedAt
) {
}