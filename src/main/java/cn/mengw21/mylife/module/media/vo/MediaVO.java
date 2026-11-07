package cn.mengw21.mylife.module.media.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 照片视图（对齐《02-API接口文档》第 9 节上传成功响应）
 *
 * @param id           照片 ID（字符串）
 * @param originalName 原始文件名
 * @param mimeType     文件 MIME 类型
 * @param sizeBytes    文件大小（字节）
 * @param description  照片描述
 * @param width        图片宽度（无法解析时为 null）
 * @param height       图片高度（无法解析时为 null）
 * @param createdAt    上传时间（UTC）
 */
@Schema(description = "照片视图")
public record MediaVO(

    @Schema(description = "照片 ID", example = "100")
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,

    @Schema(description = "原始文件名", example = "photo.jpg")
    String originalName,

    @Schema(description = "文件 MIME 类型", example = "image/jpeg")
    String mimeType,

    @Schema(description = "文件大小（字节）", example = "102400")
    Long sizeBytes,

    @Schema(description = "照片描述")
    String description,

    @Schema(description = "图片宽度")
    Integer width,

    @Schema(description = "图片高度")
    Integer height,

    @Schema(description = "上传时间", example = "2026-11-06T10:00:00Z")
    LocalDateTime createdAt
) {

    /**
     * 实体 → VO（不暴露 storageKey / sha256 等内部信息）
     */
    public static MediaVO from(cn.mengw21.mylife.module.media.entity.MediaFile media) {
        return new MediaVO(
            media.getId(),
            media.getOriginalName(),
            media.getMimeType(),
            media.getSizeBytes(),
            media.getDescription(),
            media.getWidth(),
            media.getHeight(),
            media.getCreatedAt()
        );
    }
}