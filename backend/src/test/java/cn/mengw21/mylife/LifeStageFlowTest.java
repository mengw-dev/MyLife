package cn.mengw21.mylife;

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
 * 人生阶段全链路集成测试：CRUD + 排序 + 照片关联 + 跨用户隔离 + 删除解除关联
 */
class LifeStageFlowTest extends BaseIntegrationTest {

    @Test
    @DisplayName("阶段完整 CRUD（创建返回字符串 ID，详情/修改/删除）")
    void crud() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");

        // 创建
        CsrfToken csrf = fetchCsrfToken();
        MvcResult createResult = mockMvc.perform(withCsrf(post("/api/v1/life-stages"), csrf)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"大学生活\",\"startDate\":\"2023-09-01\",\"endDate\":\"2027-06-30\",\"description\":\"我的大学阶段\",\"sortOrder\":1}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").isString())
            .andReturn();
        long id = idOf(createResult.getResponse().getContentAsString(), "$.data.id");

        // 列表含该阶段
        mockMvc.perform(get("/api/v1/life-stages").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("大学生活"));

        // 详情
        mockMvc.perform(get("/api/v1/life-stages/{id}", id).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.description").value("我的大学阶段"));

        // 修改
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/life-stages/{id}", id), csrf)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"大学生活2\",\"startDate\":\"2023-09-01\",\"endDate\":\"2027-06-30\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/life-stages/{id}", id).session(session))
            .andExpect(jsonPath("$.data.name").value("大学生活2"));

        // 删除
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/life-stages/{id}", id), csrf).session(session))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/life-stages/{id}", id).session(session))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("开始日期晚于结束日期 → 400")
    void invalidDateRange() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/life-stages"), csrf)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"错误阶段\",\"startDate\":\"2027-06-30\",\"endDate\":\"2023-09-01\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("开始日期不能晚于结束日期"));
    }

    @Test
    @DisplayName("跨用户访问他人阶段 → 404（不暴露资源存在性）")
    void crossUserAccessReturns404() throws Exception {
        MockHttpSession owner = registerAndLogin(randomUsername(), "pass12345");
        MockHttpSession stranger = registerAndLogin(randomUsername(), "pass12345");

        CsrfToken csrf = fetchCsrfToken();
        String json = mockMvc.perform(withCsrf(post("/api/v1/life-stages"), csrf)
                .session(owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"私人阶段\",\"startDate\":\"2020-01-01\"}"))
            .andReturn().getResponse().getContentAsString();
        long id = idOf(json, "$.data.id");

        mockMvc.perform(get("/api/v1/life-stages/{id}", id).session(stranger))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(40401));

        CsrfToken csrf2 = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/life-stages/{id}", id), csrf2)
                .session(stranger)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"篡改\",\"startDate\":\"2020-01-01\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("批量调整顺序 + 照片关联完整替换")
    void orderAndMedia() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        long a = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/life-stages"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"A\",\"startDate\":\"2020-01-01\",\"sortOrder\":1}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        long b = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/life-stages"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"B\",\"startDate\":\"2020-01-01\",\"sortOrder\":2}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        // 上传一张照片并设置到阶段 A
        long mediaId = uploadPng(session);
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/life-stages/{id}/media", a), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"mediaIds\":[" + mediaId + "]}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/life-stages/{id}", a).session(session))
            .andExpect(jsonPath("$.data.mediaIds[0]").value(String.valueOf(mediaId)));

        // 批量调整顺序：A→3、B→1
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/life-stages/order"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"id\":" + a + ",\"sortOrder\":3},{\"id\":" + b + ",\"sortOrder\":1}]}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/life-stages").session(session))
            .andExpect(jsonPath("$.data[0].name").value("B"))
            .andExpect(jsonPath("$.data[1].name").value("A"));
    }

    @Test
    @DisplayName("删除阶段后日记保留且 stageId 置空")
    void deleteStageKeepsDiaryWithNullStage() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        long stageId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/life-stages"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"大学\",\"startDate\":\"2020-01-01\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        long diaryId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/diaries"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"日记\",\"content\":\"正文\",\"recordDate\":\"2021-05-01\",\"stageId\":" + stageId + "}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(withCsrf(delete("/api/v1/life-stages/{id}", stageId), csrf).session(session))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/diaries/{id}", diaryId).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stageId").doesNotExist());
    }
}