package cn.mengw21.mylife.module.diary.controller;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.IdVO;
import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.module.diary.dto.DiarySaveRequest;
import cn.mengw21.mylife.module.diary.service.DiaryService;
import cn.mengw21.mylife.module.diary.vo.DiaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 日记接口（对齐《02-API接口文档》第 4 节，全部需要登录）
 */
@Tag(name = "日记", description = "日记分页 / 详情 / 创建 / 修改 / 删除")
@Validated
@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    /** 日记分页查询（keyword/日期范围/阶段/标签筛选） */
    @Operation(summary = "日记分页查询", description = "分页默认 20 条、最大 100 条；支持关键词、日期范围、阶段、标签筛选")
    @GetMapping
    public ApiResponse<PageResponse<DiaryVO>> page(
        @Parameter(description = "页码（从 1 开始）") @RequestParam(defaultValue = "1") @Min(1) long page,
        @Parameter(description = "每页条数（1-100）") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
        @Parameter(description = "标题/正文关键词") @RequestParam(required = false) String keyword,
        @Parameter(description = "记录日期下限") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @Parameter(description = "记录日期上限") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @Parameter(description = "人生阶段筛选") @RequestParam(required = false) Long stageId,
        @Parameter(description = "标签名筛选") @RequestParam(required = false) String tag) {
        return ApiResponse.success(diaryService.page(page, pageSize, keyword, from, to, stageId, tag));
    }

    /** 创建日记 */
    @Operation(summary = "创建日记")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdVO> create(@Valid @RequestBody DiarySaveRequest request) {
        return ApiResponse.success(new IdVO(diaryService.create(request)));
    }

    /** 日记详情 */
    @Operation(summary = "日记详情")
    @GetMapping("/{id}")
    public ApiResponse<DiaryVO> detail(@PathVariable Long id) {
        return ApiResponse.success(diaryService.getDetail(id));
    }

    /** 修改日记 */
    @Operation(summary = "修改日记", description = "tagNames / mediaIds 表示完整替换对应关联集合；修改日期后时间线自动调整归属")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody DiarySaveRequest request) {
        diaryService.update(id, request);
        return ApiResponse.success("日记已更新", null);
    }

    /** 删除日记 */
    @Operation(summary = "删除日记", description = "关联的重要事件保留并解除关联；被引用的照片不删除")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        diaryService.delete(id);
        return ApiResponse.success("日记已删除", null);
    }
}