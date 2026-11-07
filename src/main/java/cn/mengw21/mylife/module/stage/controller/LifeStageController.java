package cn.mengw21.mylife.module.stage.controller;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.IdVO;
import cn.mengw21.mylife.module.stage.dto.LifeStageMediaRequest;
import cn.mengw21.mylife.module.stage.dto.LifeStageOrderRequest;
import cn.mengw21.mylife.module.stage.dto.LifeStageSaveRequest;
import cn.mengw21.mylife.module.stage.service.LifeStageService;
import cn.mengw21.mylife.module.stage.vo.LifeStageVO;
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
 * 人生阶段接口（对齐《02-API接口文档》第 3 节，全部需要登录）
 */
@Tag(name = "人生阶段", description = "阶段 CRUD / 排序 / 照片关联")
@RestController
@RequestMapping("/api/v1/life-stages")
@RequiredArgsConstructor
public class LifeStageController {

    private final LifeStageService lifeStageService;

    /** 阶段列表 */
    @Operation(summary = "阶段列表", description = "返回当前用户的全部人生阶段，按排序值升序")
    @GetMapping
    public ApiResponse<List<LifeStageVO>> list() {
        return ApiResponse.success(lifeStageService.list());
    }

    /** 创建阶段 */
    @Operation(summary = "创建阶段", description = "开始日期必须不晚于结束日期；结束日期为空表示持续中")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdVO> create(@Valid @RequestBody LifeStageSaveRequest request) {
        return ApiResponse.success(new IdVO(lifeStageService.create(request)));
    }

    /** 阶段详情 */
    @Operation(summary = "阶段详情", description = "含该阶段关联的照片 ID 集合")
    @GetMapping("/{id}")
    public ApiResponse<LifeStageVO> detail(@PathVariable Long id) {
        return ApiResponse.success(lifeStageService.getDetail(id));
    }

    /** 修改阶段 */
    @Operation(summary = "修改阶段")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody LifeStageSaveRequest request) {
        lifeStageService.update(id, request);
        return ApiResponse.success("阶段已更新", null);
    }

    /** 删除阶段 */
    @Operation(summary = "删除阶段", description = "关联的日记、学习记录和事件保留，stage_id 置空")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        lifeStageService.delete(id);
        return ApiResponse.success("阶段已删除", null);
    }

    /** 批量调整顺序 */
    @Operation(summary = "批量调整顺序")
    @PutMapping("/order")
    public ApiResponse<Void> updateOrder(@Valid @RequestBody LifeStageOrderRequest request) {
        lifeStageService.updateOrder(request);
        return ApiResponse.success("顺序已更新", null);
    }

    /** 设置阶段照片（V1.0 补充接口，对应 life_stage_media 表） */
    @Operation(summary = "设置阶段照片", description = "mediaIds 表示完整替换该阶段的全部照片关联")
    @PutMapping("/{id}/media")
    public ApiResponse<Void> updateMedia(@PathVariable Long id, @Valid @RequestBody LifeStageMediaRequest request) {
        lifeStageService.updateMedia(id, request);
        return ApiResponse.success("照片已更新", null);
    }
}