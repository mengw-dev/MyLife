package cn.mengw21.mylife.module.knowledge.category.service.impl;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import cn.mengw21.mylife.module.knowledge.category.dto.KnowledgeCategorySaveRequest;
import cn.mengw21.mylife.module.knowledge.category.entity.KnowledgeCategory;
import cn.mengw21.mylife.module.knowledge.category.mapper.KnowledgeCategoryMapper;
import cn.mengw21.mylife.module.knowledge.category.service.KnowledgeCategoryService;
import cn.mengw21.mylife.module.knowledge.category.vo.KnowledgeCategoryVO;
import cn.mengw21.mylife.module.knowledge.note.entity.KnowledgeNote;
import cn.mengw21.mylife.module.knowledge.note.mapper.KnowledgeNoteMapper;
import cn.mengw21.mylife.security.CurrentUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识分类服务实现
 */
@Service
@RequiredArgsConstructor
public class KnowledgeCategoryServiceImpl implements KnowledgeCategoryService {

    /** 根节点的分组哨兵（parent_id 为 null 的分类归入此键） */
    private static final Long ROOT_SENTINEL = 0L;

    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeNoteMapper noteMapper;

    @Override
    public List<KnowledgeCategoryVO> tree() {
        Long userId = requireLogin();
        List<KnowledgeCategory> categories = categoryMapper.selectList(
            Wrappers.<KnowledgeCategory>lambdaQuery()
                .eq(KnowledgeCategory::getUserId, userId)
                .orderByAsc(KnowledgeCategory::getSortOrder)
                .orderByAsc(KnowledgeCategory::getId)
        );

        Map<Long, List<KnowledgeCategory>> byParent = categories.stream()
            .collect(Collectors.groupingBy(category -> category.getParentId() == null ? ROOT_SENTINEL : category.getParentId()));
        return buildChildren(ROOT_SENTINEL, byParent, 0);
    }

    @Override
    public Long create(KnowledgeCategorySaveRequest request) {
        Long userId = requireLogin();
        requireOwnedParent(userId, request.parentId());

        KnowledgeCategory category = new KnowledgeCategory();
        category.setUserId(userId);
        applySave(category, request);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void update(Long id, KnowledgeCategorySaveRequest request) {
        KnowledgeCategory category = requireOwned(id);
        Long userId = category.getUserId();

        // 父分类归属校验
        requireOwnedParent(userId, request.parentId());
        // 环校验：新父分类不能是自己或自己的子孙
        if (request.parentId() != null && request.parentId().equals(id)) {
            throw new BusinessException(ApiResponse.PARAM_ERROR, "父分类不能是自身");
        }
        if (request.parentId() != null && isDescendantOf(userId, request.parentId(), id)) {
            throw new BusinessException(ApiResponse.PARAM_ERROR, "父分类不能是自己的子分类");
        }

        applySave(category, request);
        categoryMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KnowledgeCategory category = requireOwned(id);

        // 存在子分类 → 409
        Long childCount = categoryMapper.selectCount(
            Wrappers.<KnowledgeCategory>lambdaQuery()
                .eq(KnowledgeCategory::getUserId, category.getUserId())
                .eq(KnowledgeCategory::getParentId, id)
        );
        if (childCount > 0) {
            throw new BusinessException(ApiResponse.CONFLICT, "存在子分类，无法删除");
        }
        // 存在知识笔记 → 409
        Long noteCount = noteMapper.selectCount(
            Wrappers.<KnowledgeNote>lambdaQuery()
                .eq(KnowledgeNote::getUserId, category.getUserId())
                .eq(KnowledgeNote::getCategoryId, id)
        );
        if (noteCount > 0) {
            throw new BusinessException(ApiResponse.CONFLICT, "分类下存在知识笔记，无法删除");
        }

        categoryMapper.deleteById(id);
    }

    // ---- 内部辅助 ----

    private Long requireLogin() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ApiResponse.UNAUTHORIZED, "未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 校验分类存在且属于当前用户（跨用户 → 404）
     */
    private KnowledgeCategory requireOwned(Long id) {
        Long userId = requireLogin();
        KnowledgeCategory category = categoryMapper.selectOne(
            Wrappers.<KnowledgeCategory>lambdaQuery()
                .eq(KnowledgeCategory::getId, id)
                .eq(KnowledgeCategory::getUserId, userId)
        );
        if (category == null) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    /**
     * 父分类归属校验：外键只保证 ID 存在，归属必须由 Service 校验
     */
    private void requireOwnedParent(Long userId, Long parentId) {
        if (parentId == null) {
            return;
        }
        Long count = categoryMapper.selectCount(
            Wrappers.<KnowledgeCategory>lambdaQuery()
                .eq(KnowledgeCategory::getId, parentId)
                .eq(KnowledgeCategory::getUserId, userId)
        );
        if (count == 0) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "父分类不存在");
        }
    }

    /**
     * 沿 parent_id 链上溯，判断 candidateId 是否是 targetId 的子孙（防环）
     */
    private boolean isDescendantOf(Long userId, Long candidateId, Long targetId) {
        Set<Long> visited = new HashSet<>();
        Long cursor = candidateId;
        while (cursor != null && visited.add(cursor)) {
            if (cursor.equals(targetId)) {
                return true;
            }
            KnowledgeCategory parent = categoryMapper.selectById(cursor);
            if (parent == null || !parent.getUserId().equals(userId)) {
                break;
            }
            cursor = parent.getParentId();
        }
        return false;
    }

    private void applySave(KnowledgeCategory category, KnowledgeCategorySaveRequest request) {
        category.setName(request.name());
        category.setParentId(request.parentId());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    /**
     * 递归组装树节点（限制深度防止脏数据导致栈溢出）
     */
    private List<KnowledgeCategoryVO> buildChildren(Long parentId, Map<Long, List<KnowledgeCategory>> byParent, int depth) {
        List<KnowledgeCategory> children = byParent.getOrDefault(parentId, List.of());
        List<KnowledgeCategoryVO> result = new ArrayList<>(children.size());
        for (KnowledgeCategory child : children) {
            List<KnowledgeCategoryVO> sub = depth >= 20
                ? List.of()
                : buildChildren(child.getId(), byParent, depth + 1);
            result.add(new KnowledgeCategoryVO(child.getId(), child.getName(), child.getParentId(), child.getSortOrder(), sub));
        }
        return result;
    }
}