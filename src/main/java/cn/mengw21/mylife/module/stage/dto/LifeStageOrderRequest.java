package cn.mengw21.mylife.module.stage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量调整阶段顺序请求（对齐《02-API接口文档》第 3 节 PUT /life-stages/order）
 *
 * @param items 阶段排序项（id + 新的 sortOrder）
 */
@Schema(description = "批量调整阶段顺序请求")
public record LifeStageOrderRequest(

    @Schema(description = "阶段排序项列表")
    @NotEmpty(message = "排序项不能为空")
    List<@Valid OrderItem> items
) {

    /**
     * 单个排序项
     *
     * @param id        阶段 ID
     * @param sortOrder 新的排序值
     */
    @Schema(description = "阶段排序项")
    public record OrderItem(

        @Schema(description = "阶段 ID", example = "1")
        @NotNull(message = "阶段 ID 不能为空")
        Long id,

        @Schema(description = "新的排序值", example = "2")
        @NotNull(message = "排序值不能为空")
        Integer sortOrder
    ) {
    }
}