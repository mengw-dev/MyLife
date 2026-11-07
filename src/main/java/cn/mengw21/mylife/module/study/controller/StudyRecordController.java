package cn.mengw21.mylife.module.study.controller;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.IdVO;
import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.module.study.dto.StudyRecordSaveRequest;
import cn.mengw21.mylife.module.study.service.StudyRecordService;
import cn.mengw21.mylife.module.study.vo.StudyRecordVO;
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
 * 学习记录接口（对齐《02-API接口文档》第 7 节，全部需要登录）
 */
@Tag(name = "学习记录", description = "学习记录分页 / 详情 / 创建 / 修改 / 删除")
@Validated
@RestController
@RequestMapping("/api/v1/study-records")
@RequiredArgsConstructor
public class StudyRecordController {

    private final StudyRecordService studyRecordService;

    /** 学习记录分页查询 */
    @Operation(summary = "学习记录分页查询", description = "支持日期范围、笔记、阶段、关键词、标签筛选")
    @GetMapping
    public ApiResponse<PageResponse<StudyRecordVO>> page(
        @Parameter(description = "页码（从 1 开始）") @RequestParam(defaultValue = "1") @Min(1) long page,
        @Parameter(description = "每页条数（1-100）") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
        @Parameter(description = "学习日期下限") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @Parameter(description = "学习日期上限") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @Parameter(description = "关联笔记筛选") @RequestParam(required = false) Long noteId,
        @Parameter(description = "人生阶段筛选") @RequestParam(required = false) Long stageId,
        @Parameter(description = "内容关键词") @RequestParam(required = false) String keyword,
        @Parameter(description = "标签名筛选") @RequestParam(required = false) String tag) {
        return ApiResponse.success(studyRecordService.page(page, pageSize, from, to, noteId, stageId, keyword, tag));
    }

    /** 创建学习记录 */
    @Operation(summary = "创建学习记录", description = "关联知识笔记时自动写入笔记标题快照")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdVO> create(@Valid @RequestBody StudyRecordSaveRequest request) {
        return ApiResponse.success(new IdVO(studyRecordService.create(request)));
    }

    /** 学习记录详情 */
    @Operation(summary = "学习记录详情")
    @GetMapping("/{id}")
    public ApiResponse<StudyRecordVO> detail(@PathVariable Long id) {
        return ApiResponse.success(studyRecordService.getDetail(id));
    }

    /** 修改学习记录 */
    @Operation(summary = "修改学习记录", description = "noteId 变更时重写标题快照；置空时保留原快照")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody StudyRecordSaveRequest request) {
        studyRecordService.update(id, request);
        return ApiResponse.success("学习记录已更新", null);
    }

    /** 删除学习记录 */
    @Operation(summary = "删除学习记录")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studyRecordService.delete(id);
        return ApiResponse.success("学习记录已删除", null);
    }
}