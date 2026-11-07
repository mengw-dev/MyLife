package cn.mengw21.mylife.module.yearprofile.controller;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.module.yearprofile.dto.YearProfileSaveRequest;
import cn.mengw21.mylife.module.yearprofile.service.YearProfileService;
import cn.mengw21.mylife.module.yearprofile.vo.YearProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 年度资料接口（对齐《02-API接口文档》第 10 节，全部需要登录）
 */
@Tag(name = "年度资料", description = "年度资料列表 / 创建或修改 / 删除")
@Validated
@RestController
@RequestMapping("/api/v1/year-profiles")
@RequiredArgsConstructor
public class YearProfileController {

    private final YearProfileService yearProfileService;

    /** 年度资料列表 */
    @Operation(summary = "年度资料列表", description = "按年份倒序返回当前用户的年度资料")
    @GetMapping
    public ApiResponse<List<YearProfileVO>> list() {
        return ApiResponse.success(yearProfileService.list());
    }

    /** 创建或修改年度资料（upsert，user_id + year 唯一） */
    @Operation(summary = "创建或修改年度资料", description = "同一 (用户, 年份) 唯一，重复 PUT 为修改")
    @PutMapping("/{year}")
    public ApiResponse<YearProfileVO> upsert(
        @PathVariable @Min(1900) @Max(3000) Integer year,
        @Valid @RequestBody YearProfileSaveRequest request) {
        return ApiResponse.success(yearProfileService.upsert(year, request));
    }

    /** 删除年度资料 */
    @Operation(summary = "删除年度资料", description = "不存在时删除 0 行，幂等成功")
    @DeleteMapping("/{year}")
    public ApiResponse<Void> delete(@PathVariable @Min(1900) @Max(3000) Integer year) {
        yearProfileService.delete(year);
        return ApiResponse.success("年度资料已删除", null);
    }
}