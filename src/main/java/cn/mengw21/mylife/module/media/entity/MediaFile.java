package cn.mengw21.mylife.module.media.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 媒体文件实体，对应表 {@code media_file}（对齐《03-数据库设计文档》3.9）
 * <p>
 * 实际文件存储在服务器本地私有目录，数据库只保存文件元数据。
 */
@Data
@TableName("media_file")
public class MediaFile {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 */
    private Long userId;

    /** 服务端生成的文件存储标识（相对私有目录的路径） */
    private String storageKey;

    /** 原始文件名 */
    private String originalName;

    /** 文件 MIME 类型 */
    private String mimeType;

    /** 文件大小（字节） */
    private Long sizeBytes;

    /** 文件 SHA-256 校验值 */
    private String sha256;

    /** 图片宽度（无法解析时为 null） */
    private Integer width;

    /** 图片高度（无法解析时为 null） */
    private Integer height;

    /** 照片描述 */
    private String description;

    /** 上传时间，插入时自动填充（UTC） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}