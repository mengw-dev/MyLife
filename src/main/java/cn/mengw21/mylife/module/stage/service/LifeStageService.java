package cn.mengw21.mylife.module.stage.service;

import cn.mengw21.mylife.module.stage.dto.LifeStageMediaRequest;
import cn.mengw21.mylife.module.stage.dto.LifeStageOrderRequest;
import cn.mengw21.mylife.module.stage.dto.LifeStageSaveRequest;
import cn.mengw21.mylife.module.stage.vo.LifeStageVO;

import java.util.List;

/**
 * 人生阶段服务（对齐《01-产品需求说明书》第 5 节 + 《02-API接口文档》第 3 节）
 * <p>
 * 所有方法都以服务端登录身份为准做归属校验，不信任客户端传参；
 * 跨用户访问统一返回 404。
 */
public interface LifeStageService {

    /** 阶段列表（按 sortOrder、id 排序） */
    List<LifeStageVO> list();

    /** 阶段详情（含关联照片） */
    LifeStageVO getDetail(Long id);

    /** 创建阶段 */
    Long create(LifeStageSaveRequest request);

    /** 修改阶段 */
    void update(Long id, LifeStageSaveRequest request);

    /**
     * 删除阶段：关联的日记、学习记录和事件保留，stage_id 由外键置空
     */
    void delete(Long id);

    /** 批量调整阶段顺序 */
    void updateOrder(LifeStageOrderRequest request);

    /** 完整替换阶段照片关联（对应 life_stage_media 表） */
    void updateMedia(Long id, LifeStageMediaRequest request);
}