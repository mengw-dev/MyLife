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
 * 知识库全链路集成测试：分类树/防环/删除保护 + 笔记 CRUD + 学习记录与快照规则
 */
class KnowledgeFlowTest extends BaseIntegrationTest {

    @Test
    @DisplayName("多级分类树 + 防环校验 + 删除保护（子分类/笔记 → 409）")
    void categoryTreeAndGuard() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        long root = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/knowledge/categories"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"计算机\",\"sortOrder\":1}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        long child = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/knowledge/categories"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Java\",\"parentId\":" + root + ",\"sortOrder\":1}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        // 分类树：root 下挂 Java
        mockMvc.perform(get("/api/v1/knowledge/categories").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].children[0].name").value("Java"));

        // 防环：把 root 挂到自己的子节点 Java 下 → 400
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(put("/api/v1/knowledge/categories/{id}", root), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"计算机\",\"parentId\":" + child + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("父分类不能是自己的子分类"));

        // Java 下建一条笔记
        long noteId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/knowledge/notes"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Java 线程池\",\"contentMarkdown\":\"# 线程池\",\"categoryId\":" + child + "}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        // 有笔记的分类删除 → 409
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/knowledge/categories/{id}", child), csrf).session(session))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("分类下存在知识笔记，无法删除"));

        // 删掉笔记后，root 有子分类 → 409
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/knowledge/notes/{id}", noteId), csrf).session(session))
            .andExpect(status().isOk());
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/knowledge/categories/{id}", root), csrf).session(session))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("存在子分类，无法删除"));

        // 空分类可删
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/knowledge/categories/{id}", child), csrf).session(session))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("删除笔记后学习记录保留：noteId 置空、标题快照保留")
    void deleteNoteKeepsStudyRecordsWithSnapshot() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        long categoryId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/knowledge/categories"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Java\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        long noteId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/knowledge/notes"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Java 线程池\",\"contentMarkdown\":\"# 正文\",\"categoryId\":" + categoryId + "}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        long recordId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/study-records"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"学习了核心参数\",\"understanding\":\"理解了\",\"studyDate\":\"2026-11-06\",\"noteId\":" + noteId + "}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        // 笔记的历史学习记录可见
        mockMvc.perform(get("/api/v1/knowledge/notes/{id}/study-records", noteId).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(String.valueOf(recordId)));

        // 删除笔记
        csrf = fetchCsrfToken();
        mockMvc.perform(withCsrf(delete("/api/v1/knowledge/notes/{id}", noteId), csrf).session(session))
            .andExpect(status().isOk());

        // 学习记录仍在：noteId 置空 + 快照保留
        mockMvc.perform(get("/api/v1/study-records/{id}", recordId).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.noteId").doesNotExist())
            .andExpect(jsonPath("$.data.noteTitleSnapshot").value("Java 线程池"));
    }

    @Test
    @DisplayName("学习记录：关联他人笔记 → 404（归属校验）")
    void studyRecordCrossUserNote() throws Exception {
        MockHttpSession owner = registerAndLogin(randomUsername(), "pass12345");
        MockHttpSession stranger = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        long categoryId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/knowledge/categories"), csrf)
                    .session(owner).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Java\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        long noteId = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/knowledge/notes"), csrf)
                    .session(owner).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"笔记\",\"contentMarkdown\":\"# 正文\",\"categoryId\":" + categoryId + "}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        // stranger 引用 owner 的笔记 → 404
        CsrfToken csrf2 = fetchCsrfToken();
        mockMvc.perform(withCsrf(post("/api/v1/study-records"), csrf2)
                .session(stranger).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"学习内容\",\"studyDate\":\"2026-11-06\",\"noteId\":" + noteId + "}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("知识笔记不存在"));
    }

    @Test
    @DisplayName("笔记分页：关键词 / 分类 / 标签筛选")
    void notePageFilters() throws Exception {
        MockHttpSession session = registerAndLogin(randomUsername(), "pass12345");
        CsrfToken csrf = fetchCsrfToken();

        long cat1 = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/knowledge/categories"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Java\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        long cat2 = idOf(
            mockMvc.perform(withCsrf(post("/api/v1/knowledge/categories"), csrf)
                    .session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Go\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(withCsrf(post("/api/v1/knowledge/notes"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"线程池\",\"contentMarkdown\":\"# 核心参数\",\"categoryId\":" + cat1 + ",\"tagNames\":[\"并发\"]}"))
            .andExpect(status().isCreated());
        mockMvc.perform(withCsrf(post("/api/v1/knowledge/notes"), csrf)
                .session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"goroutine\",\"contentMarkdown\":\"# 协程\",\"categoryId\":" + cat2 + "}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/knowledge/notes").session(session).param("keyword", "核心参数"))
            .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/api/v1/knowledge/notes").session(session).param("categoryId", String.valueOf(cat2)))
            .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/api/v1/knowledge/notes").session(session).param("tag", "并发"))
            .andExpect(jsonPath("$.data.total").value(1));
    }
}