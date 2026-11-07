package cn.mengw21.mylife.module.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建 / 修改重要事件请求（对齐《02-API接口文档》第 8 节）
 * <p>
 * 一条事件最多关联一篇日记；PUT 时 tagNames 与 mediaIds 表示完整替换对应关联集合。
 *
 * @param title      事件标题
 * @param eventDate  事件日期
 * @param description 事件描述
 * @param stageId    所属人生阶段，可空
 * @param diaryId    关联日记，可空
 * @param mediaIds   相关照片 ID 集合（完整替换）
 * @param tagNames   标签名集合（完整替换）
 */
@Schema(description = "创建 / 修改重要事件请求")
public record ImportantEventSaveRequest(

    @Schema(description = "事件标题", example = "完成 MyLife 基础设计")
    @NotBlank(message = "事件标题不能为空")
    @Size(max = 200, message = "事件标题最长 200 字")
    String title,

    @Schema(description = "事件日期", example = "2026-11-06")
    @NotNull(message = "事件日期不能为空")
    LocalDate eventDate,

    @Schema(description = "事件描述", example = "完成个人生活记录系统的设计。")
    String description,

    @Schema(description = "所属人生阶段 ID", example = "1")
    Long stageId,

    @Schema(description = "关联日记 ID", example = "100")
    Long diaryId,

    @Schema(description = "相关照片 ID 集合（完整替换）")
    @Size(max = 50, message = "照片最多 50 张")
    List<Long> mediaIds,

    @Schema(description = "标签名集合（完整替换）", example = "[\"项目\"]")
    @Size(max = 20, message = "标签最多 20 个")
    List<String> tagNames
) {
}