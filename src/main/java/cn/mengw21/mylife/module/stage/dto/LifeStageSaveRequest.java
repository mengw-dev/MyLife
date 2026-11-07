package cn.mengw21.mylife.module.stage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 创建 / 修改人生阶段请求（对齐《02-API接口文档》第 3 节）
 *
 * @param name         阶段名称
 * @param startDate    开始日期（必须 ≤ endDate）
 * @param endDate      结束日期，可空（空表示持续中）
 * @param description  阶段介绍
 * @param coverMediaId 封面照片 ID，可空
 * @param sortOrder    排序值
 */
@Schema(description = "创建 / 修改人生阶段请求")
public record LifeStageSaveRequest(

    @Schema(description = "阶段名称", example = "大学生活")
    @NotBlank(message = "阶段名称不能为空")
    @Size(max = 100, message = "阶段名称最长 100 字")
    String name,

    @Schema(description = "开始日期", example = "2023-09-01")
    @NotNull(message = "开始日期不能为空")
    LocalDate startDate,

    @Schema(description = "结束日期，可空（空表示持续中）", example = "2027-06-30")
    LocalDate endDate,

    @Schema(description = "阶段介绍")
    String description,

    @Schema(description = "封面照片 ID", example = "100")
    Long coverMediaId,

    @Schema(description = "排序值，越小越靠前", example = "1")
    Integer sortOrder
) {
}