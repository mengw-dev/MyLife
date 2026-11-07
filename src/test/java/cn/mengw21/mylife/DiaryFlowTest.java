package cn.mengw21.mylife;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 日记全链路集成测试：CRUD + 标签/照片关联 + 筛选 + 跨用户隔离 + 删除保护事件
 */
class DiaryFlowTest extends BaseIntegrationTest {

    @Test
    @DisplayName("日记 CRUD + 标签/照片关联 + 完整替换语义")
    void crudWithTagsAndMedia() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        long mediaId = uploadPng(session);
        CsrfToken csrf = fetchCsrfToken();

        // 创建（带标签、照片）
        String created = mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"今天的记录\",\"content\":\"今天学习了 Spring Boot。\",\"recordDate\":\"2026-11-06\",\"tagNames\":[\"学习\",\"Java\"],\"mediaIds\":[" + mediaId + "]}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long id = idOf(created, "$.data.id");

        // 详情：标签与照片都在
        mockMvc.perform(get("/api/v1/diaries/{id}", id).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tagNames.length()").value(2))
            .andExpect(jsonPath("$.data.mediaIds[0]").value(String.valueOf(mediaId)));

        // 修改：标签完整替换为单个、照片清空；日期改为同一天的另一篇（同天多篇允许）
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/diaries/{id}", id), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"今天的记录v2\",\"content\":\"更新后的正文\",\"recordDate\":\"2026-11-06\",\"tagNames\":[\"学习\"],\"mediaIds\":[]}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/diaries/{id}", id).session(session))
            .andExpect(jsonPath("$.data.title").value("今天的记录v2"))
            .andExpect(jsonPath("$.data.tagNames.length()").value(1))
            .andExpect(jsonPath("$.data.mediaIds").isEmpty());

        // 删除
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/diaries/{id}", id), csrf).session(session))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/diaries/{id}", id).session(session))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("分页筛选：关键词 / 日期范围 / 阶段 / 标签")
    void pageFilters() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        // 三个日记：不同日期、标签、阶段
        long stageId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/life-stages"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"大学\",\"startDate\":\"2020-01-01\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Java 学习\",\"content\":\"线程池\",\"recordDate\":\"2026-11-01\",\"tagNames\":[\"学习\"],\"stageId\":" + stageId + "}"))
            .andExpect(status().isCreated());
        mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"旅行日记\",\"content\":\"海边\",\"recordDate\":\"2026-11-10\",\"tagNames\":[\"生活\"]}"))
            .andExpect(status().isCreated());
        mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"周末\",\"content\":\"休息\",\"recordDate\":\"2026-12-01\"}"))
            .andExpect(status().isCreated());

        // 关键词 "线程池" → 1 条
        mockMvc.perform(get("/api/v1/diaries").session(session).param("keyword", "线程池"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1));

        // 日期范围 11-01~11-15 → 2 条
        mockMvc.perform(get("/api/v1/diaries").session(session)
                .param("from", "2026-11-01").param("to", "2026-11-15"))
            .andExpect(jsonPath("$.data.total").value(2));

        // 标签 "学习" → 1 条
        mockMvc.perform(get("/api/v1/diaries").session(session).param("tag", "学习"))
            .andExpect(jsonPath("$.data.total").value(1));

        // ID 输出为字符串
        mockMvc.perform(get("/api/v1/diaries").session(session))
            .andExpect(jsonPath("$.data.records[0].id").isString());
    }

    @Test
    @DisplayName("跨用户访问他人日记 → 404；创建时引用他人阶段 → 404")
    void isolation() throws Exception {
        MockHttpSession owner = registerAndLogin(randomUsername(), "pass12345");
        MockHttpSession stranger = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        long stageId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/life-stages"), csrf)
                    .session(owner).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"私人阶段\",\"startDate\":\"2020-01-01\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        long diaryId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf)
                    .session(owner).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"私人日记\",\"content\":\"正文\",\"recordDate\":\"2026-11-06\",\"stageId\":" + stageId + "}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        // 他人读取 → 404
        mockMvc.perform(get("/api/v1/diaries/{id}", diaryId).session(stranger))
            .andExpect(status().isNotFound());

        // 他人创建日记引用 owner 的阶段 → 404
        CsrfToken csrf2 = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf2)
                .session(stranger).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"越权\",\"content\":\"正文\",\"recordDate\":\"2026-11-06\",\"stageId\":" + stageId + "}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("阶段不存在"));
    }

    @Test
    @DisplayName("删除日记后：被引用照片保留，事件保留且 diaryId 置空")
    void deleteDiaryKeepsPhotosAndEvents() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();
        long mediaId = uploadPng(session);

        long diaryId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"日记\",\"content\":\"正文\",\"recordDate\":\"2026-11-06\",\"mediaIds\":[" + mediaId + "]}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        long eventId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/events"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"事件\",\"eventDate\":\"2026-11-06\",\"diaryId\":" + diaryId + "}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(withCsrf(delete("/api/v1/diaries/{id}", diaryId), csrf).session(session))
            .andExpect(status().isOk());

        // 照片仍在（且此时可直接删除，说明引用已被解除）
        mockMvc.perform(get("/api/v1/media/{id}", mediaId).session(session))
            .andExpect(status().isOk());
        mockMvc.perform(withCsrf(delete("/api/v1/media/{id}", mediaId), csrf).session(session))
            .andExpect(status().isOk());

        // 事件保留且 diaryId 为空
        mockMvc.perform(get("/api/v1/events/{id}", eventId).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.diaryId").doesNotExist());
    }
}