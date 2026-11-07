package cn.mengw21.mylife.module.event.service;

import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.module.event.dto.ImportantEventSaveRequest;
import cn.mengw21.mylife.module.event.vo.ImportantEventVO;

import java.time.LocalDate;

/**
 * 重要事件服务（对齐《01-产品需求说明书》第 8 节 + 《02-API接口文档》第 8 节）
 * <p>
 * 一条事件最多关联一篇日记；删除日记时事件保留。
 */
public interface ImportantEventService {

    /**
     * 事件分页查询
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param from     事件日期下限，可空
     * @param to       事件日期上限，可空
     * @param stageId  人生阶段筛选，可空
     * @param tag      标签名筛选，可空
     */
    PageResponse<ImportantEventVO> page(long pageNo, long pageSize, LocalDate from, LocalDate to,
                                        Long stageId, String tag);

    /** 事件详情 */
    ImportantEventVO getDetail(Long id);

    /** 创建事件（stageId / diaryId 归属校验） */
    Long create(ImportantEventSaveRequest request);

    /** 修改事件（tagNames / mediaIds 完整替换对应关联集合） */
    void update(Long id, ImportantEventSaveRequest request);

    /** 删除事件 */
    void delete(Long id);
}