package cn.mengw21.mylife.security;

import cn.mengw21.mylife.common.api.ApiResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CSRF Token 获取端点（对齐《02-API接口文档》第 2 节 GET /auth/csrf）
 * <p>
 * 前端在发起首个写操作（POST/PUT/DELETE）之前先 GET 一次，
 * 既能拿到 token，也会触发服务端下发 {@code XSRF-TOKEN} Cookie。
 * 后续请求把 token 放入 {@code X-XSRF-TOKEN} 请求头即可。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class CsrfController {

    /**
     * 获取 CSRF Token（匿名可访问，见 SecurityConfig.PUBLIC_PATHS）
     */
    @GetMapping("/csrf")
    public ApiResponse<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ApiResponse.success(Map.of("token", csrfToken.getToken()));
    }
}