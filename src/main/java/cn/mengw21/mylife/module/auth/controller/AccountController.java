package cn.mengw21.mylife.module.auth.controller;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号注销接口（对齐《02-API接口文档》第 2 节 DELETE /account）
 * <p>
 * 注销会清理当前用户的全部业务数据与媒体文件（含引用关系），
 * 只删除本人数据，不影响其他用户。
 */
@Tag(name = "用户认证", description = "注册 / 登录 / 登出 / 当前用户 / 修改密码 / 注销账号")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService authService;

    /** 注销账号（登录后） */
    @Operation(summary = "注销账号", description = "清理当前用户的全部个人数据与媒体文件后删除账号，并使会话失效")
    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.deleteAccount(httpRequest, httpResponse);
        return ApiResponse.success("账号已注销", null);
    }
}