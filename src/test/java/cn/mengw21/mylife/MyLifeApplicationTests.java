package cn.mengw21.mylife;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 基础框架集成测试
 * <p>
 * 验证基础设施：认证拦截、CSRF、统一异常、参数校验（对齐《02-API接口文档》1.5/1.6）。
 */
class MyLifeApplicationTests extends BaseIntegrationTest {

    @Test
    @DisplayName("Spring 上下文加载成功")
    void contextLoads() {
    }

    @Test
    @DisplayName("未登录访问受保护接口返回 401 JSON（code=40101）")
    void unauthenticatedRequestReturns401Json() throws Exception {
        mockMvc.perform(get("/api/v1/health/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(40101))
            .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    @DisplayName("CSRF 端点允许匿名访问并下发 XSRF-TOKEN Cookie（code=0）")
    void csrfEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @DisplayName("业务异常转换成 HTTP 400（code=40001）")
    void businessExceptionReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/test/business-error").with(user("tester")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(40001))
            .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @DisplayName("参数校验失败转换成 HTTP 400 并返回 fieldErrors 明细")
    void validationFailureReturns400() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/test/validate"), fetchCsrfToken())
                .with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(40001))
            .andExpect(jsonPath("$.message").value("参数校验失败"))
            .andExpect(jsonPath("$.data.fieldErrors[0].field").value("title"))
            .andExpect(jsonPath("$.data.fieldErrors[0].message").value("标题不能为空"));
    }
}