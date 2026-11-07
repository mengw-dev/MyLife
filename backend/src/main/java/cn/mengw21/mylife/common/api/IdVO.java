package cn.mengw21.mylife.common.api;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建类接口的统一返回（仅主键）
 * <p>
 * 数据库主键为 BIGINT，JSON 中统一以字符串传递（《02-API接口文档》1.3），
 * 避免 JavaScript 数字精度问题。所有"创建"接口返回 {@code ApiResponse<IdVO>}。
 *
 * @param id 新记录主键（字符串）
 */
@Schema(description = "创建结果（新记录 ID）")
public record IdVO(

    @Schema(description = "新记录 ID", example = "10001")
    @JsonSerialize(using = ToStringSerializer.class)
    Long id
) {
}