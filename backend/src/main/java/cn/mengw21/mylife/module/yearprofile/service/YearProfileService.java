package cn.mengw21.mylife.module.yearprofile.service;

import cn.mengw21.mylife.module.yearprofile.dto.YearProfileSaveRequest;
import cn.mengw21.mylife.module.yearprofile.vo.YearProfileVO;

import java.util.List;

/**
 * 年度资料服务（对齐《02-API接口文档》第 10 节）
 * <p>
 * 年度资料通过 user_id + year 唯一确定；PUT /year-profiles/{year} 为创建或修改（upsert）。
 */
public interface YearProfileService {

    /** 年度资料列表（按年份倒序） */
    List<YearProfileVO> list();

    /** 创建或修改指定年份的资料（upsert） */
    YearProfileVO upsert(Integer year, YearProfileSaveRequest request);

    /** 删除指定年份的资料（不存在时也视为成功，幂等） */
    void delete(Integer year);
}