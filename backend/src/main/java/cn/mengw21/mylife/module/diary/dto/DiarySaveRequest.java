package cn.mengw21.mylife.module.diary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建 / 修改日记请求（对齐《02-API接口文档》第 4 节）
 * <p>
 * PUT 时 tagNames 与 mediaIds 表示完整替换对应关联集合。
 *
 * @param title      标题
 * @param content    正文
 * @param recordDate 记录日期（时间线归属依据）
 * @param stageId    所属人生阶段，可空
 * @param tagNames   标签名集合（完整替换）
 * @param mediaIds   相关照片 ID 集合（完整替换）
 */
@Schema(description = "创建 / 修改日记请求")
public record DiarySaveRequest(

    @Schema(description = "标题", example = "今天的记录")
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 字")
    String title,

    @Schema(description = "正文", example = "今天学习了 Spring Boot。")
    @NotBlank(message = "正文不能为空")
    String content,

    @Schema(description = "记录日期", example = "2026-11-06")
    @NotNull(message = "记录日期不能为空")
    LocalDate recordDate,

    @Schema(description = "所属人生阶段 ID", example = "1")
    Long stageId,

    @Schema(description = "标签名集合（完整替换）", example = "[\"学习\",\"Java\"]")
    @Size(max = 20, message = "标签最多 20 个")
    List<String> tagNames,

    @Schema(description = "相关照片 ID 集合（完整替换）", example = "[\"100\",\"101\"]")
    @Size(max = 50, message = "照片最多 50 张")
    List<Long> mediaIds
) {
}