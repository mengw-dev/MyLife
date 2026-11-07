package cn.mengw21.mylife.module.stage.service.impl;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import cn.mengw21.mylife.common.service.MediaOwnershipGuard;
import cn.mengw21.mylife.module.stage.dto.LifeStageMediaRequest;
import cn.mengw21.mylife.module.stage.dto.LifeStageOrderRequest;
import cn.mengw21.mylife.module.stage.dto.LifeStageSaveRequest;
import cn.mengw21.mylife.module.stage.entity.LifeStage;
import cn.mengw21.mylife.module.stage.entity.LifeStageMedia;
import cn.mengw21.mylife.module.stage.mapper.LifeStageMapper;
import cn.mengw21.mylife.module.stage.mapper.LifeStageMediaMapper;
import cn.mengw21.mylife.module.stage.service.LifeStageService;
import cn.mengw21.mylife.module.stage.vo.LifeStageVO;
import cn.mengw21.mylife.security.CurrentUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 人生阶段服务实现
 */
@Service
@RequiredArgsConstructor
public class LifeStageServiceImpl implements LifeStageService {

    private final LifeStageMapper lifeStageMapper;
    private final LifeStageMediaMapper lifeStageMediaMapper;
    private final MediaOwnershipGuard mediaOwnershipGuard;

    @Override
    public List<LifeStageVO> list() {
        Long userId = requireLogin();
        List<LifeStage> stages = lifeStageMapper.selectList(
            Wrappers.<LifeStage>lambdaQuery()
                .eq(LifeStage::getUserId, userId)
                .orderByAsc(LifeStage::getSortOrder)
                .orderByAsc(LifeStage::getId)
        );
        return stages.stream().map(stage -> toVO(stage, null)).toList();
    }

    @Override
    public LifeStageVO getDetail(Long id) {
        LifeStage stage = requireOwned(id);
        List<Long> mediaIds = lifeStageMediaMapper.selectList(
                Wrappers.<LifeStageMedia>lambdaQuery().eq(LifeStageMedia::getStageId, id))
            .stream().map(LifeStageMedia::getMediaId).toList();
        return toVO(stage, mediaIds);
    }

    @Override
    public Long create(LifeStageSaveRequest request) {
        Long userId = requireLogin();
        validateDateRange(request.startDate(), request.endDate());
        // 封面照片归属校验（可选引用）
        mediaOwnershipGuard.requireOwnedOne(userId, request.coverMediaId());

        LifeStage stage = new LifeStage();
        stage.setUserId(userId);
        applySave(stage, request);
        lifeStageMapper.insert(stage);
        return stage.getId();
    }

    @Override
    public void update(Long id, LifeStageSaveRequest request) {
        LifeStage stage = requireOwned(id);
        validateDateRange(request.startDate(), request.endDate());
        mediaOwnershipGuard.requireOwnedOne(requireLogin(), request.coverMediaId());

        applySave(stage, request);
        lifeStageMapper.updateById(stage);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LifeStage stage = requireOwned(id);
        // 阶段照片关联随外键级联删除；日记/学习记录/事件的 stage_id 由外键置空
        lifeStageMapper.deleteById(stage.getId());
    }

    @Override
    @Transactional
    public void updateOrder(LifeStageOrderRequest request) {
        Long userId = requireLogin();
        // 先校验全部阶段归属，防止批量更新中混入他人资源
        List<Long> ids = request.items().stream().map(LifeStageOrderRequest.OrderItem::id).toList();
        Long ownedCount = lifeStageMapper.selectCount(
            Wrappers.<LifeStage>lambdaQuery()
                .eq(LifeStage::getUserId, userId)
                .in(LifeStage::getId, ids)
        );
        if (ownedCount != ids.stream().distinct().count()) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "阶段不存在");
        }
        request.items().forEach(item -> {
            LifeStage stage = new LifeStage();
            stage.setId(item.id());
            stage.setSortOrder(item.sortOrder());
            lifeStageMapper.updateById(stage);
        });
    }

    @Override
    @Transactional
    public void updateMedia(Long id, LifeStageMediaRequest request) {
        LifeStage stage = requireOwned(id);
        // 照片归属校验（不存在的照片或他人照片 → 404）
        List<Long> mediaIds = request.mediaIds() == null ? List.of() : request.mediaIds();
        mediaOwnershipGuard.requireOwnedList(stage.getUserId(), mediaIds);

        // 完整替换关联集合
        lifeStageMediaMapper.delete(Wrappers.<LifeStageMedia>lambdaQuery()
            .eq(LifeStageMedia::getStageId, id));
        mediaIds.stream().distinct().forEach(mediaId -> {
            LifeStageMedia link = new LifeStageMedia();
            link.setStageId(id);
            link.setMediaId(mediaId);
            lifeStageMediaMapper.insert(link);
        });
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
     * 校验阶段存在且属于当前用户（跨用户 → 404，不暴露资源存在性）
     */
    private LifeStage requireOwned(Long id) {
        Long userId = requireLogin();
        LifeStage stage = lifeStageMapper.selectOne(
            Wrappers.<LifeStage>lambdaQuery()
                .eq(LifeStage::getId, id)
                .eq(LifeStage::getUserId, userId)
        );
        if (stage == null) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "阶段不存在");
        }
        return stage;
    }

    /**
     * 业务规则：start_date 必须 ≤ end_date（end_date 为空表示持续中）
     */
    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ApiResponse.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
    }

    private void applySave(LifeStage stage, LifeStageSaveRequest request) {
        stage.setName(request.name());
        stage.setStartDate(request.startDate());
        stage.setEndDate(request.endDate());
        stage.setDescription(request.description());
        stage.setCoverMediaId(request.coverMediaId());
        stage.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private LifeStageVO toVO(LifeStage stage, List<Long> mediaIds) {
        return new LifeStageVO(
            stage.getId(),
            stage.getName(),
            stage.getStartDate(),
            stage.getEndDate(),
            stage.getDescription(),
            stage.getCoverMediaId(),
            stage.getSortOrder(),
            mediaIds,
            stage.getCreatedAt(),
            stage.getUpdatedAt()
        );
    }
}