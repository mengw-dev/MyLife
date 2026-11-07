package cn.mengw21.mylife.module.health;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 受保护的健康检查 — 用来验证 401 / 登录后返回 200
 * <p>
 * 开发阶段用来测 Spring Security 是否生效（非业务接口）。
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    /**
     * 未登录访问 → 401 JSON（由 SecurityConfig 的 authenticationEntryPoint 处理）
     * 登录后访问 → 当前用户 ID
     */
    @GetMapping("/me")
    public ApiResponse<Long> me() {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(userId);
    }
}