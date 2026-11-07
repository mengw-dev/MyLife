package cn.mengw21.mylife;

import cn.mengw21.mylife.module.auth.dto.LoginRequest;
import cn.mengw21.mylife.module.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试基类
 * <p>
 * 跑在真实 MySQL 上（测试专用库 mylife_test，见 application-test.yaml），
 * Flyway 自动建表，全链路与生产一致。
 * 每个测试方法执行前清空全部业务表与测试上传目录，测试间互不影响。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "classpath:sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class BaseIntegrationTest {

    /** 测试上传目录（与 application-test.yaml 的 mylife.upload-dir 一致） */
    protected static final String TEST_UPLOAD_DIR = "target/test-uploads";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void cleanUploadDir() throws Exception {
        Path dir = Path.of(TEST_UPLOAD_DIR);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // 测试目录清理失败不影响主流程
                }
            });
        }
    }

    // ---- CSRF 辅助：完全模拟真实前端 ----

    /**
     * 像浏览器一样先 GET /api/v1/auth/csrf 拿到 token
     */
    protected CsrfToken fetchCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf")).andReturn();
        return (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
    }

    /**
     * 给写请求带上 X-XSRF-TOKEN 请求头 + XSRF-TOKEN Cookie
     */
    protected MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder builder, CsrfToken token) {
        return builder
            .header(token.getHeaderName(), token.getToken())
            .cookie(new Cookie("XSRF-TOKEN", token.getToken()));
    }

    // ---- 用户辅助 ----

    /**
     * 注册并登录，返回已认证的 Session
     */
    protected MockHttpSession registerAndLogin(String username, String password) throws Exception {
        CsrfToken csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    new RegisterRequest(username, password, "昵称" + username, null))))
            .andExpect(status().isCreated());

        csrf = fetchCsrfToken();
        MvcResult loginResult = mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LoginRequest(username, password))))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        if (session == null) {
            throw new IllegalStateException("登录后 Session 为空，可能登录未持久化 SecurityContext");
        }
        return session;
    }

    /**
     * 生成不重复的测试用户名（测试间数据天然隔离）
     */
    protected String randomUsername() {
        return "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /**
     * 从 JSON 响应中按路径取 ID（契约规定 ID 为字符串，防精度丢失）
     */
    protected long idOf(String json, String path) {
        return Long.parseLong(JsonPath.<String>read(json, path));
    }

    /**
     * 取当前用户 ID（字符串 → long）
     */
    protected long currentUserId(MockHttpSession session) throws Exception {
        String json = mockMvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return idOf(json, "$.data.id");
    }

    // ---- 媒体辅助 ----

    /**
     * 上传一张测试 PNG（1x1 透明像素，真实 PNG 魔数可通过签名校验）
     */
    protected long uploadPng(MockHttpSession session) throws Exception {
        return uploadBytes(session, pngBytes(), "test.png");
    }

    /**
     * 上传任意字节（用于测试伪造文件等场景）
     */
    protected long uploadBytes(MockHttpSession session, byte[] bytes, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/png", bytes);
        CsrfToken csrf = fetchCsrfToken();
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/v1/media");
        builder.file(file);
        MvcResult result = mockMvc.perform(withCsrf(builder, csrf)
                .session(session))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
        return idOf(result.getResponse().getContentAsString(), "$.data.id");
    }

    /**
     * 1x1 透明 PNG 字节
     */
    protected byte[] pngBytes() {
        return Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
    }
}