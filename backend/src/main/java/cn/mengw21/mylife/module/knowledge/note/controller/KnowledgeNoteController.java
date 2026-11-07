package cn.mengw21.mylife.module.knowledge.note.controller;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.IdVO;
import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.module.knowledge.note.dto.KnowledgeNoteSaveRequest;
import cn.mengw21.mylife.module.knowledge.note.service.KnowledgeNoteService;
import cn.mengw21.mylife.module.knowledge.note.vo.KnowledgeNoteVO;
import cn.mengw21.mylife.module.study.vo.StudyRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * 知识笔记接口（对齐《02-API接口文档》第 6 节，全部需要登录）
 */
@Tag(name = "知识笔记", description = "笔记分页 / 详情 / 创建 / 修改 / 删除 / 历史学习记录")
@Validated
@RestController
@RequestMapping("/api/v1/knowledge/notes")
@RequiredArgsConstructor
public class KnowledgeNoteController {

    private final KnowledgeNoteService noteService;

    /** 笔记分页 */
    @Operation(summary = "笔记分页查询", description = "支持关键词、分类、标签筛选")
    @GetMapping
    public ApiResponse<PageResponse<KnowledgeNoteVO>> page(
        @Parameter(description = "页码（从 1 开始）") @RequestParam(defaultValue = "1") @Min(1) long page,
        @Parameter(description = "每页条数（1-100）") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
        @Parameter(description = "标题/正文关键词") @RequestParam(required = false) String keyword,
        @Parameter(description = "分类筛选") @RequestParam(required = false) Long categoryId,
        @Parameter(description = "标签名筛选") @RequestParam(required = false) String tag) {
        return ApiResponse.success(noteService.page(page, pageSize, keyword, categoryId, tag));
    }

    /** 创建笔记 */
    @Operation(summary = "创建笔记")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdVO> create(@Valid @RequestBody KnowledgeNoteSaveRequest request) {
        return ApiResponse.success(new IdVO(noteService.create(request)));
    }

    /** 笔记详情 */
    @Operation(summary = "笔记详情")
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeNoteVO> detail(@PathVariable Long id) {
        return ApiResponse.success(noteService.getDetail(id));
    }

    /** 修改笔记 */
    @Operation(summary = "修改笔记", description = "tagNames / mediaIds 表示完整替换对应关联集合")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody KnowledgeNoteSaveRequest request) {
        noteService.update(id, request);
        return ApiResponse.success("笔记已更新", null);
    }

    /** 删除笔记 */
    @Operation(summary = "删除笔记", description = "学习记录保留并解除关联，保留原笔记标题快照")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noteService.delete(id);
        return ApiResponse.success("笔记已删除", null);
    }

    /** 查看历史学习记录 */
    @Operation(summary = "查看历史学习记录", description = "返回关联到该笔记的全部学习记录，按学习日期倒序")
    @GetMapping("/{id}/study-records")
    public ApiResponse<List<StudyRecordVO>> studyRecords(@PathVariable Long id) {
        return ApiResponse.success(noteService.listStudyRecords(id));
    }
}