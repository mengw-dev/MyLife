package cn.mengw21.mylife;

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
 * 重要事件全链路集成测试：CRUD + 日记/阶段关联归属 + 跨用户隔离
 */
class EventFlowTest extends BaseIntegrationTest {

    @Test
    @DisplayName("事件 CRUD + 日记关联 + 照片/标签")
    void crudWithDiaryLink() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();
        long mediaId = uploadPng(session);

        long diaryId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"日记\",\"content\":\"正文\",\"recordDate\":\"2026-11-06\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        long id = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/events"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"完成 MyLife 基础设计\",\"eventDate\":\"2026-11-06\",\"description\":\"完成设计\",\"diaryId\":" + diaryId + ",\"mediaIds\":[" + mediaId + "],\"tagNames\":[\"项目\"]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/v1/events/{id}", id).session(session))
            .andExpect(jsonPath("$.data.diaryId").value(String.valueOf(diaryId)))
            .andExpect(jsonPath("$.data.tagNames[0]").value("项目"))
            .andExpect(jsonPath("$.data.mediaIds[0]").value(String.valueOf(mediaId)));

        // 修改：解除日记关联
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/events/{id}", id), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"完成设计v2\",\"eventDate\":\"2026-11-06\",\"diaryId\":null,\"mediaIds\":[],\"tagNames\":[]}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/events/{id}", id).session(session))
            .andExpect(jsonPath("$.data.title").value("完成设计v2"))
            .andExpect(jsonPath("$.data.diaryId").doesNotExist());

        // 删除
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/events/{id}", id), csrf).session(session))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("事件关联他人日记 → 404；跨用户访问他人事件 → 404")
    void crossUserGuard() throws Exception {
        MockHttpSession owner = registerAndLogin(randomUsername(), "pass12345");
        MockHttpSession stranger = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        long diaryId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf)
                    .session(owner).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"私人日记\",\"content\":\"正文\",\"recordDate\":\"2026-11-06\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        // stranger 建事件引用 owner 的日记 → 404
        CsrfToken csrf2 = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/events"), csrf2)
                .session(stranger).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"越权事件\",\"eventDate\":\"2026-11-06\",\"diaryId\":" + diaryId + "}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("日记不存在"));

        // owner 建事件，stranger 读取 → 404
        long eventId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/events"), csrf)
                    .session(owner).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"私人事件\",\"eventDate\":\"2026-11-06\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(get("/api/v1/events/{id}", eventId).session(stranger))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("事件分页：日期范围 / 阶段 / 标签筛选")
    void pageFilters() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        long stageId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/life-stages"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"大学\",\"startDate\":\"2020-01-01\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(withCsrf(post("/api/v1/events"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"毕业\",\"eventDate\":\"2026-06-30\",\"stageId\":" + stageId + ",\"tagNames\":[\"里程碑\"]}"))
            .andExpect(status().isCreated());
        mockMvc.perform(withCsrf(post("/api/v1/events"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"入职\",\"eventDate\":\"2026-09-01\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/events").session(session).param("from", "2026-06-01").param("to", "2026-07-31"))
            .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/api/v1/events").session(session).param("stageId", String.valueOf(stageId)))
            .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/api/v1/events").session(session).param("tag", "里程碑"))
            .andExpect(jsonPath("$.data.total").value(1));
    }
}