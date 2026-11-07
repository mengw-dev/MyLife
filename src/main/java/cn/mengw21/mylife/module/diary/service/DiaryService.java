package cn.mengw21.mylife.module.diary.service;

import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.module.diary.dto.DiarySaveRequest;
import cn.mengw21.mylife.module.diary.vo.DiaryVO;

import java.time.LocalDate;

/**
 * 日记服务（对齐《01-产品需求说明书》第 6 节 + 《02-API接口文档》第 4 节）
 * <p>
 * 业务规则：同一天允许多篇日记；修改日记日期后时间线自动调整归属；
 * 删除日记不删除照片、事件保留并解除关联。
 */
public interface DiaryService {

    /**
     * 日记分页查询（默认 20 条/页，最大 100）
     *
     * @param pageNo   页码（从 1 开始）
     * @param pageSize 每页条数
     * @param keyword  标题/正文关键词，可空
     * @param from     记录日期下限，可空
     * @param to       记录日期上限，可空
     * @param stageId  人生阶段筛选，可空
     * @param tag      标签名筛选，可空
     */
    PageResponse<DiaryVO> page(long pageNo, long pageSize, String keyword, LocalDate from,
                               LocalDate to, Long stageId, String tag);

    /** 日记详情 */
    DiaryVO getDetail(Long id);

    /** 创建日记 */
    Long create(DiarySaveRequest request);

    /** 修改日记（tagNames / mediaIds 完整替换对应关联集合） */
    void update(Long id, DiarySaveRequest request);

    /** 删除日记：关联事件保留并解除关联，照片不删除 */
    void delete(Long id);
}