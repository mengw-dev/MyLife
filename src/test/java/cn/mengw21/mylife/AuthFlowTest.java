package cn.mengw21.mylife;

import cn.mengw21.mylife.module.auth.dto.ChangePasswordRequest;
import cn.mengw21.mylife.module.auth.dto.LoginRequest;
import cn.mengw21.mylife.module.auth.dto.RegisterRequest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证全链路集成测试：注册 → 登录 → me → 改密 → 退出 + 注销账号（对齐《02-API接口文档》第 2 节）
 */
class AuthFlowTest extends BaseIntegrationTest {

    @Test
    @DisplayName("注册(201)-重复注册(409)-登录-me(字符串ID)-退出 完整流程")
    void registerLoginMeLogout() throws Exception {
        String username = randomUsername();
        String password = "pass12345";

        // 1. 注册成功 → 201 + code 0 + data.id 为字符串
        CsrfToken csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new RegisterRequest(username, password, "测试昵称", null))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").isString());

        // 2. 重复注册 → 409 + 40901
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new RegisterRequest(username, password, null, null))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(40901));

        // 3. 登录成功（Session 生效）→ 200 + code 0
        csrf = fetchCsrfToken();
        MvcResult loginResult = mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LoginRequest(username, password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // 4. 带着登录 Session 查 me（不含密码字段）
        mockMvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.data.id").isString());

        // 5. 退出登录
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/logout"), csrf).session(session))
            .andExpect(status().isOk());

        // 6. 退出后 me → 401
        mockMvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("密码错误登录返回 401，且不区分用户是否存在（防用户名枚举）")
    void wrongPasswordReturns401() throws Exception {
        String username = randomUsername();
        registerAndLogin(username, "pass12345");

        CsrfToken csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LoginRequest(username, "wrong-password"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(40101))
            .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        // 用户不存在 → 同一个提示
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LoginRequest("ghost_user_xxx", "whatever123"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("连续失败登录触发防暴力冷却（PRD 12 安全要求 9）")
    void tooManyFailuresTriggersLock() throws Exception {
        String username = randomUsername();
        registerAndLogin(username, "pass12345");

        // 连续 5 次错误密码
        for (int i = 0; i < 5; i++) {
            CsrfToken csrf = fetchCsrfToken();
            mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(new LoginRequest(username, "wrong-password"))))
                .andExpect(status().isUnauthorized());
        }

        // 第 6 次即使密码正确也被冷却拦截
        CsrfToken csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LoginRequest(username, "pass12345"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("登录尝试次数过多，请 15 分钟后再试"));
    }

    @Test
    @DisplayName("PUT /auth/password 修改密码后旧密码失效、新密码可登录")
    void changePasswordInvalidatesOldPassword() throws Exception {
        String username = randomUsername();
        MockHttpSession session = registerAndLogin(username, "pass12345");

        // 改密码（成功即注销当前会话）
        CsrfToken csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/auth/password"), csrf)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    new ChangePasswordRequest("pass12345", "newpass6789"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        // 旧密码登录 → 401
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LoginRequest(username, "pass12345"))))
            .andExpect(status().isUnauthorized());

        // 新密码登录 → 200
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LoginRequest(username, "newpass6789"))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /account 注销账号：业务数据与照片文件全部清理，之后无法再登录")
    void deleteAccountCleansAllUserData() throws Exception {
        String username = randomUsername();
        MockHttpSession session = registerAndLogin(username, "pass12345");
        long userId = currentUserId(session);

        // 造一点业务数据：人生阶段 + 日记 + 照片
        CsrfToken csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/life-stages"), csrf)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"大学\",\"startDate\":\"2020-09-01\"}"))
            .andExpect(status().isCreated());
        mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"第一篇\",\"content\":\"正文\",\"recordDate\":\"2026-11-06\"}"))
            .andExpect(status().isCreated());
        long mediaId = uploadPng(session);

        // 注销账号
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/account"), csrf).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        // 会话已失效 → 401
        mockMvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isUnauthorized());

        // 重新登录 → 401（账号已不存在）
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LoginRequest(username, "pass12345"))))
            .andExpect(status().isUnauthorized());

        // 已上传的照片物理文件被清理（目录可能残留空文件夹，但不应再有文件）
        java.nio.file.Path userDir = java.nio.file.Path.of(TEST_UPLOAD_DIR, String.valueOf(userId));
        org.springframework.util.Assert.isTrue(
            !java.nio.file.Files.exists(userDir)
                || java.nio.file.Files.list(userDir).findAny().isEmpty(),
            "注销后该用户的媒体文件应被清理"
        );
        // 说明：mediaId 变量用于可读性（终止未使用告警）
        org.springframework.util.Assert.isTrue(mediaId > 0, "上传应成功");
    }

    @Test
    @DisplayName("非法注册参数返回 400 与 fieldErrors 明细")
    void invalidRegisterPayloadReturns400() throws Exception {
        CsrfToken csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new RegisterRequest("ab", "short", null, null))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(40001))
            .andExpect(jsonPath("$.data.fieldErrors").isArray());
    }
}