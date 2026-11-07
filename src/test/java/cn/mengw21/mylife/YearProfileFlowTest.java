package cn.mengw21.mylife;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 年度资料集成测试：upsert / 列表 / 删除（user_id + year 唯一）
 */
class YearProfileFlowTest extends BaseIntegrationTest {

    @Test
    @DisplayName("upsert：首次创建，重复 PUT 为修改（唯一键 user_id+year）")
    void upsertIsIdempotent() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        mockMvc.perform(withCsrf(put("/api/v1/year-profiles/2026"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"技术探索与成长\",\"summary\":\"这一年开始系统学习 Java。\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.year").value(2026))
            .andExpect(jsonPath("$.data.id").isString());

        // 再次 PUT 同一年 → 修改
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/year-profiles/2026"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"技术探索\",\"summary\":\"更新简介\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/year-profiles").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].title").value("技术探索"));
    }

    @Test
    @DisplayName("列表按年份倒序 + 删除幂等")
    void listAndDelete() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        mockMvc.perform(withCsrf(put("/api/v1/year-profiles/2024"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"2024 年\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(withCsrf(put("/api/v1/year-profiles/2026"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"2026 年\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/year-profiles").session(session))
            .andExpect(jsonPath("$.data[0].year").value(2026))
            .andExpect(jsonPath("$.data[1].year").value(2024));

        // 删除（存在的与不存在的都幂等成功）
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/year-profiles/2026"), csrf).session(session))
            .andExpect(status().isOk());
        mockMvc.perform(withCsrf(delete("/api/v1/year-profiles/1999"), csrf).session(session))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/year-profiles").session(session))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].year").value(2024));
    }

    @Test
    @DisplayName("年份参数越界 → 400")
    void invalidYear() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/year-profiles/1800"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"远古\"}"))
            .andExpect(status().isBadRequest());
    }
}