package cn.mengw21.mylife.common.service;

import cn.mengw21.mylife.module.tag.entity.Tag;

import java.util.List;
import java.util.Map;

/**
 * 标签公共服务 — 各业务模块（日记 / 笔记 / 学习记录 / 事件）共享
 * <p>
 * {@code tagNames} 属于"名称直写"参数：入库时按 (user_id, name) 查找，
 * 不存在则创建；返回 名称 → 标签 映射供业务拼关联表。
 */
public interface TagService {

    /**
     * 按名称取回（必要时创建）当前用户的标签
     *
     * @param userId 当前用户 ID
     * @param names  标签名列表（内部会做 trim、去空、去重）
     * @return 名称 → 标签 映射
     */
    Map<String, Tag> loadOrCreate(Long userId, List<String> names);
}