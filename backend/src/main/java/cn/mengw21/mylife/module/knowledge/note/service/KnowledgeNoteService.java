package cn.mengw21.mylife.module.knowledge.note.service;

import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.module.knowledge.note.dto.KnowledgeNoteSaveRequest;
import cn.mengw21.mylife.module.knowledge.note.vo.KnowledgeNoteVO;
import cn.mengw21.mylife.module.study.vo.StudyRecordVO;

import java.util.List;

/**
 * 知识笔记服务（对齐《01-产品需求说明书》7.2 + 《02-API接口文档》第 6 节）
 * <p>
 * 删除策略：删除笔记时保留学习记录（note_id 置空、标题快照不丢）。
 */
public interface KnowledgeNoteService {

    /**
     * 笔记分页查询
     *
     * @param pageNo     页码
     * @param pageSize   每页条数
     * @param keyword    标题/正文关键词，可空
     * @param categoryId 分类筛选，可空
     * @param tag        标签名筛选，可空
     */
    PageResponse<KnowledgeNoteVO> page(long pageNo, long pageSize, String keyword, Long categoryId, String tag);

    /** 笔记详情 */
    KnowledgeNoteVO getDetail(Long id);

    /** 创建笔记 */
    Long create(KnowledgeNoteSaveRequest request);

    /** 修改笔记（tagNames / mediaIds 完整替换对应关联集合） */
    void update(Long id, KnowledgeNoteSaveRequest request);

    /** 删除笔记：学习记录保留并解除关联，标题快照不丢 */
    void delete(Long id);

    /** 查看某篇笔记的历史学习记录（按学习日期倒序） */
    List<StudyRecordVO> listStudyRecords(Long id);
}