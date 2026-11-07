package cn.mengw21.mylife;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 照片全链路集成测试：上传校验 / 鉴权读取 / 引用保护删除 / 跨用户隔离
 */
class MediaFlowTest extends BaseIntegrationTest {

    @Test
    @DisplayName("上传 PNG 成功：返回字符串 ID、MIME、大小，原图可鉴权读取")
    void uploadAndRead() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        byte[] png = pngBytes();

        CsrfToken csrf = fetchCsrfToken();
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", png);
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/v1/media");
        builder.file(file);
        String json = mockMvc.perform(withCsrf(builder, csrf)
                .session(session))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").isString())
            .andExpect(jsonPath("$.data.mimeType").value("image/png"))
            .andReturn().getResponse().getContentAsString();
        long id = idOf(json, "$.data.id");

        // 列表含 1 条
        mockMvc.perform(get("/api/v1/media").session(session))
            .andExpect(jsonPath("$.data.total").value(1));

        // 详情
        mockMvc.perform(get("/api/v1/media/{id}", id).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.originalName").value("photo.png"));

        // 原图：登录后可读取，返回真实字节
        mockMvc.perform(get("/api/v1/media/{id}/content", id).session(session))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andExpect(content().bytes(png));

        // 未登录读取原图 → 401
        mockMvc.perform(get("/api/v1/media/{id}/content", id))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("伪造文件（扩展名与真实内容不符）→ 415 拒绝")
    void rejectFakeFile() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        // .jpg 扩展名，内容其实是文本
        MockMultipartFile fake = new MockMultipartFile("file", "fake.jpg", "image/jpeg",
            "not an image at all".getBytes());
        MockMultipartHttpServletRequestBuilder fakeBuilder = multipart("/api/v1/media");
        fakeBuilder.file(fake);
        mockMvc.perform(withCsrf(fakeBuilder, csrf)
                .session(session))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value(41501));

        // 不被支持的扩展名
        MockMultipartFile bmp = new MockMultipartFile("file", "a.bmp", "image/bmp", pngBytes());
        MockMultipartHttpServletRequestBuilder bmpBuilder = multipart("/api/v1/media");
        bmpBuilder.file(bmp);
        mockMvc.perform(withCsrf(bmpBuilder, csrf)
                .session(session))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("被日记引用的照片删除 → 409；解除引用后可删除")
    void referencedMediaCannotBeDeleted() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();
        long mediaId = uploadPng(session);

        // 建日记引用它
        long diaryId = idOf(
            mockMvc.perform(withCsrf(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/diaries"), csrf)
                    .session(session).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"日记\",\"content\":\"正文\",\"recordDate\":\"2026-11-06\",\"mediaIds\":[" + mediaId + "]}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        // 删除照片 → 409
        mockMvc.perform(withCsrf(delete("/api/v1/media/{id}", mediaId), csrf).session(session))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("照片仍被引用，无法删除"));

        // 解除引用（删除日记）后再删 → 成功
        mockMvc.perform(withCsrf(delete("/api/v1/diaries/{id}", diaryId), csrf).session(session))
            .andExpect(status().isOk());
        mockMvc.perform(withCsrf(delete("/api/v1/media/{id}", mediaId), csrf).session(session))
            .andExpect(status().isOk());

        // 删除后详情 → 404
        mockMvc.perform(get("/api/v1/media/{id}", mediaId).session(session))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("跨用户访问他人照片（详情/原图/删除）→ 404")
    void crossUserIsolation() throws Exception {
        MockHttpSession owner = registerAndLogin(randomUsername(), "pass12345");
        MockHttpSession stranger = registerAndLogin(randomUsername(), "pass12345");
        long mediaId = uploadPng(owner);

        mockMvc.perform(get("/api/v1/media/{id}", mediaId).session(stranger))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/media/{id}/content", mediaId).session(stranger))
            .andExpect(status().isNotFound());

        CsrfToken csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/media/{id}", mediaId), csrf).session(stranger))
            .andExpect(status().isNotFound());
    }
}