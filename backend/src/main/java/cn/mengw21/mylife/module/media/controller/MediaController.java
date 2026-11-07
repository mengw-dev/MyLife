package cn.mengw21.mylife.module.media.controller;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.module.media.service.MediaContent;
import cn.mengw21.mylife.module.media.service.MediaService;
import cn.mengw21.mylife.module.media.vo.MediaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 照片接口（对齐《02-API接口文档》第 9 节，全部需要登录）
 * <p>
 * 私人照片必须鉴权访问：原图通过本接口鉴权后流式返回，不提供公开静态 URL。
 */
@Tag(name = "照片", description = "上传 / 列表 / 详情 / 原图 / 删除")
@Validated
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private static final int MAX_PAGE_SIZE = 100;

    private final MediaService mediaService;

    /** 上传照片 */
    @Operation(summary = "上传照片", description = "multipart/form-data，字段 file；仅支持 JPEG/PNG/WebP/GIF，单张最大 10MB，服务端校验真实文件签名")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MediaVO> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(mediaService.upload(file));
    }

    /** 照片列表 */
    @Operation(summary = "照片列表", description = "按上传时间倒序分页返回")
    @GetMapping
    public ApiResponse<PageResponse<MediaVO>> page(
        @Parameter(description = "页码（从 1 开始）") @RequestParam(defaultValue = "1") @Min(1) long page,
        @Parameter(description = "每页条数（1-100）") @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) long pageSize) {
        return ApiResponse.success(mediaService.page(page, pageSize));
    }

    /** 照片详情 */
    @Operation(summary = "照片详情")
    @GetMapping("/{id}")
    public ApiResponse<MediaVO> detail(@PathVariable Long id) {
        return ApiResponse.success(mediaService.getDetail(id));
    }

    /** 获取照片原图（登录 + 归属校验） */
    @Operation(summary = "获取照片原图", description = "鉴权后流式返回图片二进制，私人照片不允许公开静态访问")
    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable Long id) {
        MediaContent content = mediaService.getContent(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(content.mimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.originalName() + "\"")
            .body(content.resource());
    }

    /** 删除照片 */
    @Operation(summary = "删除照片", description = "仍被日记/阶段/笔记/事件引用（含封面）时返回 409")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        mediaService.delete(id);
        return ApiResponse.success("照片已删除", null);
    }
}