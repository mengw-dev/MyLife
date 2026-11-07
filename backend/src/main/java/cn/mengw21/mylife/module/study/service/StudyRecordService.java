package cn.mengw21.mylife.module.study.service;

import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.module.study.dto.StudyRecordSaveRequest;
import cn.mengw21.mylife.module.study.vo.StudyRecordVO;

import java.time.LocalDate;

/**
 * 学习记录服务（对齐《01-产品需求说明书》7.3 + 《02-API接口文档》第 7 节）
 * <p>
 * 业务规则：一条学习记录最多关联一篇知识笔记；关联笔记时冗余保存标题快照，
 * 删除笔记后学习记录保留且快照不变；修改笔记不影响过去的学习记录。
 */
public interface StudyRecordService {

    /**
     * 学习记录分页查询
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param from     学习日期下限，可空
     * @param to       学习日期上限，可空
     * @param noteId   关联笔记筛选，可空
     * @param stageId  人生阶段筛选，可空
     * @param keyword  内容关键词，可空
     * @param tag      标签名筛选，可空
     */
    PageResponse<StudyRecordVO> page(long pageNo, long pageSize, LocalDate from, LocalDate to,
                                     Long noteId, Long stageId, String keyword, String tag);

    /** 学习记录详情 */
    StudyRecordVO getDetail(Long id);

    /** 创建学习记录（关联笔记时写入标题快照） */
    Long create(StudyRecordSaveRequest request);

    /** 修改学习记录（noteId 变更时重新写入快照；置空时保留原快照） */
    void update(Long id, StudyRecordSaveRequest request);

    /** 删除学习记录 */
    void delete(Long id);
}