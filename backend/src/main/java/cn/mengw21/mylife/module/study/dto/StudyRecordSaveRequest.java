package cn.mengw21.mylife.module.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建 / 修改学习记录请求（对齐《02-API接口文档》第 7 节）
 * <p>
 * 一条学习记录最多关联一篇知识笔记；PUT 时 tagNames 表示完整替换关联集合。
 *
 * @param title        学习标题（可选）
 * @param content      学习内容
 * @param understanding 当时理解
 * @param questions    遇到的问题
 * @param studyDate    学习日期
 * @param noteId       关联知识笔记 ID，可空
 * @param stageId      所属人生阶段 ID，可空
 * @param tagNames     标签名集合（完整替换）
 */
@Schema(description = "创建 / 修改学习记录请求")
public record StudyRecordSaveRequest(

    @Schema(description = "学习标题（可选）", example = "学习线程池参数")
    @Size(max = 200, message = "学习标题最长 200 字")
    String title,

    @Schema(description = "学习内容", example = "今天学习了线程池的核心参数。")
    @NotBlank(message = "学习内容不能为空")
    String content,

    @Schema(description = "当时理解", example = "理解了核心线程数和最大线程数的区别。")
    String understanding,

    @Schema(description = "遇到的问题", example = "拒绝策略还需要进一步研究。")
    String questions,

    @Schema(description = "学习日期", example = "2026-11-06")
    @NotNull(message = "学习日期不能为空")
    LocalDate studyDate,

    @Schema(description = "关联知识笔记 ID", example = "100")
    Long noteId,

    @Schema(description = "所属人生阶段 ID", example = "1")
    Long stageId,

    @Schema(description = "标签名集合（完整替换）", example = "[\"Java\"]")
    @Size(max = 20, message = "标签最多 20 个")
    List<String> tagNames
) {
}