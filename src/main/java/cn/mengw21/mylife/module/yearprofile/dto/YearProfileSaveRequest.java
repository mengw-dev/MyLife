package cn.mengw21.mylife.module.yearprofile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 创建 / 修改年度资料请求（对齐《02-API接口文档》第 10 节）
 *
 * @param title        年度标题
 * @param summary      年度简介
 * @param coverMediaId 代表照片 ID，可空
 */
@Schema(description = "创建 / 修改年度资料请求")
public record YearProfileSaveRequest(

    @Schema(description = "年度标题", example = "技术探索与成长")
    @Size(max = 120, message = "年度标题最长 120 字")
    String title,

    @Schema(description = "年度简介", example = "这一年开始系统学习 Java。")
    String summary,

    @Schema(description = "代表照片 ID", example = "100")
    Long coverMediaId
) {
}