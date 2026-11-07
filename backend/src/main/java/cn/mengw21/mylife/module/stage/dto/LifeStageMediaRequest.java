package cn.mengw21.mylife.module.stage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 设置阶段照片请求（对齐《02-API接口文档》第 3 节 PUT /life-stages/{id}/media）
 * <p>
 * 对应数据库 {@code life_stage_media} 表，mediaIds 表示完整替换全部阶段照片关联。
 *
 * @param mediaIds 照片 ID 集合（完整替换）
 */
@Schema(description = "设置阶段照片请求")
public record LifeStageMediaRequest(

    @Schema(description = "照片 ID 集合（完整替换阶段照片关联）", example = "[\"100\",\"101\"]")
    @Size(max = 50, message = "单阶段照片最多 50 张")
    List<Long> mediaIds
) {
}