package cn.mengw21.mylife.module.event.controller;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.IdVO;
import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.module.event.dto.ImportantEventSaveRequest;
import cn.mengw21.mylife.module.event.service.ImportantEventService;
import cn.mengw21.mylife.module.event.vo.ImportantEventVO;
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
 * 重要事件接口（对齐《02-API接口文档》第 8 节，全部需要登录）
 */
@Tag(name = "重要事件", description = "事件分页 / 详情 / 创建 / 修改 / 删除")
@Validated
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class ImportantEventController {

    private final ImportantEventService eventService;

    /** 事件分页 */
    @Operation(summary = "事件分页查询", description = "支持日期范围、阶段、标签筛选")
    @GetMapping
    public ApiResponse<PageResponse<ImportantEventVO>> page(
        @Parameter(description = "页码（从 1 开始）") @RequestParam(defaultValue = "1") @Min(1) long page,
        @Parameter(description = "每页条数（1-100）") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
        @Parameter(description = "事件日期下限") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @Parameter(description = "事件日期上限") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @Parameter(description = "人生阶段筛选") @RequestParam(required = false) Long stageId,
        @Parameter(description = "标签名筛选") @RequestParam(required = false) String tag) {
        return ApiResponse.success(eventService.page(page, pageSize, from, to, stageId, tag));
    }

    /** 创建事件 */
    @Operation(summary = "创建事件", description = "一条事件最多关联一篇日记；stageId / diaryId 均需属于当前用户")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdVO> create(@Valid @RequestBody ImportantEventSaveRequest request) {
        return ApiResponse.success(new IdVO(eventService.create(request)));
    }

    /** 事件详情 */
    @Operation(summary = "事件详情")
    @GetMapping("/{id}")
    public ApiResponse<ImportantEventVO> detail(@PathVariable Long id) {
        return ApiResponse.success(eventService.getDetail(id));
    }

    /** 修改事件 */
    @Operation(summary = "修改事件", description = "tagNames / mediaIds 表示完整替换对应关联集合")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ImportantEventSaveRequest request) {
        eventService.update(id, request);
        return ApiResponse.success("事件已更新", null);
    }

    /** 删除事件 */
    @Operation(summary = "删除事件")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ApiResponse.success("事件已删除", null);
    }
}