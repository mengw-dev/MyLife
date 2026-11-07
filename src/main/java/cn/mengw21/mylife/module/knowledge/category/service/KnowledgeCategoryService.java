package cn.mengw21.mylife.module.knowledge.category.service;

import cn.mengw21.mylife.module.knowledge.category.dto.KnowledgeCategorySaveRequest;
import cn.mengw21.mylife.module.knowledge.category.vo.KnowledgeCategoryVO;

import java.util.List;

/**
 * 知识分类服务（对齐《01-产品需求说明书》7.1 + 《02-API接口文档》第 5 节）
 * <p>
 * 支持多级嵌套与排序；存在子分类或知识笔记时禁止删除（返回 409）。
 */
public interface KnowledgeCategoryService {

    /** 获取当前用户的完整分类树 */
    List<KnowledgeCategoryVO> tree();

    /** 创建分类（父分类必须属于当前用户） */
    Long create(KnowledgeCategorySaveRequest request);

    /** 修改分类（防止把自己挂到自己或子孙节点下造成环） */
    void update(Long id, KnowledgeCategorySaveRequest request);

    /** 删除分类：存在子分类或知识笔记时返回 409 */
    void delete(Long id);
}