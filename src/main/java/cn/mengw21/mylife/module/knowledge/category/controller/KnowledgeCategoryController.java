package cn.mengw21.mylife.module.knowledge.category.controller;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.IdVO;
import cn.mengw21.mylife.module.knowledge.category.dto.KnowledgeCategorySaveRequest;
import cn.mengw21.mylife.module.knowledge.category.service.KnowledgeCategoryService;
import cn.mengw21.mylife.module.knowledge.category.vo.KnowledgeCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识分类接口（对齐《02-API接口文档》第 5 节，全部需要登录）
 */
@Tag(name = "知识分类", description = "多级分类树 / 创建 / 修改 / 删除")
@RestController
@RequestMapping("/api/v1/knowledge/categories")
@RequiredArgsConstructor
public class KnowledgeCategoryController {

    private final KnowledgeCategoryService categoryService;

    /** 获取分类树 */
    @Operation(summary = "获取分类树", description = "返回当前用户的完整多级分类树，按排序值升序")
    @GetMapping
    public ApiResponse<List<KnowledgeCategoryVO>> tree() {
        return ApiResponse.success(categoryService.tree());
    }

    /** 创建分类 */
    @Operation(summary = "创建分类")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdVO> create(@Valid @RequestBody KnowledgeCategorySaveRequest request) {
        return ApiResponse.success(new IdVO(categoryService.create(request)));
    }

    /** 修改分类 */
    @Operation(summary = "修改分类", description = "父分类不能是自身或自己的子分类（防环）")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody KnowledgeCategorySaveRequest request) {
        categoryService.update(id, request);
        return ApiResponse.success("分类已更新", null);
    }

    /** 删除分类 */
    @Operation(summary = "删除分类", description = "存在子分类或知识笔记时返回 409，不允许直接删除")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success("分类已删除", null);
    }
}