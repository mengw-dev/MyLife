package cn.mengw21.mylife.module.yearprofile.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 年度资料视图
 *
 * @param id           资料 ID（字符串）
 * @param year         年份
 * @param title        年度标题
 * @param summary      年度简介
 * @param coverMediaId 代表照片 ID
 * @param createdAt    创建时间（UTC）
 * @param updatedAt    修改时间（UTC）
 */
@Schema(description = "年度资料视图")
public record YearProfileVO(

    @Schema(description = "资料 ID", example = "10")
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,

    @Schema(description = "年份", example = "2026")
    Integer year,

    @Schema(description = "年度标题")
    String title,

    @Schema(description = "年度简介")
    String summary,

    @Schema(description = "代表照片 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    Long coverMediaId,

    @Schema(description = "创建时间", example = "2026-11-06T10:00:00Z")
    LocalDateTime createdAt,

    @Schema(description = "修改时间", example = "2026-11-06T10:00:00Z")
    LocalDateTime updatedAt
) {
}