package cn.mengw21.mylife.module.auth.controller;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.module.auth.dto.ChangePasswordRequest;
import cn.mengw21.mylife.module.auth.dto.LoginRequest;
import cn.mengw21.mylife.module.auth.dto.RegisterRequest;
import cn.mengw21.mylife.module.auth.service.AuthService;
import cn.mengw21.mylife.module.auth.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证接口（对齐《02-API接口文档》第 2 节）
 * <p>
 * 注册与登录允许匿名访问（见 SecurityConfig PUBLIC_PATHS），
 * 其余接口需要登录。写请求需带 CSRF Token。
 */
@Tag(name = "用户认证", description = "注册 / 登录 / 登出 / 当前用户 / 修改密码")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 用户注册（匿名） */
    @Operation(summary = "用户注册", description = "创建新账号，用户名全局唯一，密码 BCrypt 加密存储")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    /** 用户登录（匿名，成功后 Session 生效） */
    @Operation(summary = "用户登录", description = "Session Cookie 认证，成功后返回当前用户信息")
    @PostMapping("/login")
    public ApiResponse<UserVO> login(@Valid @RequestBody LoginRequest request,
                                     HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        return ApiResponse.success("登录成功", authService.login(request, httpRequest, httpResponse));
    }

    /** 用户登出 */
    @Operation(summary = "用户登出", description = "注销当前会话并清理 SecurityContext")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ApiResponse.success("已退出登录", null);
    }

    /** 获取当前用户信息 */
    @Operation(summary = "获取当前用户", description = "从 Session 中的登录态返回当前用户信息")
    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.success(authService.getCurrentUser());
    }

    /** 修改密码（成功后需重新登录） */
    @Operation(summary = "修改密码", description = "校验原密码后更新，成功即注销当前会话要求重新登录")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        authService.changePassword(request, httpRequest, httpResponse);
        return ApiResponse.success("密码修改成功，请重新登录", null);
    }
}