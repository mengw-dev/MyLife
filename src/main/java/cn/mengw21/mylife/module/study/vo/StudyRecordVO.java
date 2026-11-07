package cn.mengw21.mylife.module.study.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习记录视图
 *
 * @param id                学习记录 ID（字符串）
 * @param title             学习标题
 * @param content           学习内容
 * @param understanding     当时理解
 * @param questions         遇到的问题
 * @param studyDate         学习日期
 * @param noteId            关联知识笔记 ID
 * @param noteTitleSnapshot 笔记标题快照（笔记删除后仍保留）
 * @param stageId           所属人生阶段 ID
 * @param tagNames          标签名集合
 * @param createdAt         创建时间（UTC）
 * @param updatedAt         修改时间（UTC）
 */
@Schema(description = "学习记录视图")
public record StudyRecordVO(

    @Schema(description = "学习记录 ID", example = "100")
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,

    @Schema(description = "学习标题")
    String title,

    @Schema(description = "学习内容")
    String content,

    @Schema(description = "当时理解")
    String understanding,

    @Schema(description = "遇到的问题")
    String questions,

    @Schema(description = "学习日期", example = "2026-11-06")
    LocalDate studyDate,

    @Schema(description = "关联知识笔记 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    Long noteId,

    @Schema(description = "笔记标题快照")
    String noteTitleSnapshot,

    @Schema(description = "所属人生阶段 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    Long stageId,

    @Schema(description = "标签名集合")
    List<String> tagNames,

    @Schema(description = "创建时间", example = "2026-11-06T10:00:00Z")
    LocalDateTime createdAt,

    @Schema(description = "修改时间", example = "2026-11-06T10:00:00Z")
    LocalDateTime updatedAt
) {
}