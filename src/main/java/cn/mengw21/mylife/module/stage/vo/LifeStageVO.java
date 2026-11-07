package cn.mengw21.mylife.module.stage.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 人生阶段视图
 *
 * @param id          阶段 ID（字符串）
 * @param name        阶段名称
 * @param startDate   开始日期
 * @param endDate     结束日期（null 表示持续中）
 * @param description 阶段介绍
 * @param coverMediaId 封面照片 ID
 * @param sortOrder   排序值
 * @param mediaIds    关联照片 ID 集合（仅详情返回）
 * @param createdAt   创建时间（UTC）
 * @param updatedAt   修改时间（UTC）
 */
@Schema(description = "人生阶段视图")
public record LifeStageVO(

    @Schema(description = "阶段 ID", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,

    @Schema(description = "阶段名称", example = "大学生活")
    String name,

    @Schema(description = "开始日期", example = "2023-09-01")
    LocalDate startDate,

    @Schema(description = "结束日期（null 表示持续中）", example = "2027-06-30")
    LocalDate endDate,

    @Schema(description = "阶段介绍")
    String description,

    @Schema(description = "封面照片 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    Long coverMediaId,

    @Schema(description = "排序值")
    Integer sortOrder,

    @Schema(description = "关联照片 ID 集合")
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    List<Long> mediaIds,

    @Schema(description = "创建时间", example = "2026-11-06T10:00:00Z")
    LocalDateTime createdAt,

    @Schema(description = "修改时间", example = "2026-11-06T10:00:00Z")
    LocalDateTime updatedAt
) {
}