package cn.mengw21.mylife.module.diary.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 日记视图
 *
 * @param id         日记 ID（字符串）
 * @param title      标题
 * @param content    正文
 * @param recordDate 记录日期
 * @param stageId    所属人生阶段 ID
 * @param tagNames   标签名集合
 * @param mediaIds   相关照片 ID 集合
 * @param createdAt  创建时间（UTC）
 * @param updatedAt  修改时间（UTC）
 */
@Schema(description = "日记视图")
public record DiaryVO(

    @Schema(description = "日记 ID", example = "100")
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,

    @Schema(description = "标题", example = "今天的记录")
    String title,

    @Schema(description = "正文")
    String content,

    @Schema(description = "记录日期", example = "2026-11-06")
    LocalDate recordDate,

    @Schema(description = "所属人生阶段 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    Long stageId,

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