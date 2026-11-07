package cn.mengw21.mylife.module.yearprofile.service.impl;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import cn.mengw21.mylife.common.service.MediaOwnershipGuard;
import cn.mengw21.mylife.module.yearprofile.dto.YearProfileSaveRequest;
import cn.mengw21.mylife.module.yearprofile.entity.YearProfile;
import cn.mengw21.mylife.module.yearprofile.mapper.YearProfileMapper;
import cn.mengw21.mylife.module.yearprofile.service.YearProfileService;
import cn.mengw21.mylife.module.yearprofile.vo.YearProfileVO;
import cn.mengw21.mylife.security.CurrentUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 年度资料服务实现
 * <p>
 * upsert 依赖 (user_id, year) 唯一索引；并发下唯一键冲突时回退为更新。
 */
@Service
@RequiredArgsConstructor
public class YearProfileServiceImpl implements YearProfileService {

    private final YearProfileMapper yearProfileMapper;
    private final MediaOwnershipGuard mediaOwnershipGuard;

    @Override
    public List<YearProfileVO> list() {
        Long userId = requireLogin();
        return yearProfileMapper.selectList(
                Wrappers.<YearProfile>lambdaQuery()
                    .eq(YearProfile::getUserId, userId)
                    .orderByDesc(YearProfile::getYear)
            ).stream()
            .map(this::toVO)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public YearProfileVO upsert(Integer year, YearProfileSaveRequest request) {
        Long userId = requireLogin();
        // 封面照片归属校验（可选引用）
        mediaOwnershipGuard.requireOwnedOne(userId, request.coverMediaId());

        YearProfile existing = yearProfileMapper.selectOne(
            Wrappers.<YearProfile>lambdaQuery()
                .eq(YearProfile::getUserId, userId)
                .eq(YearProfile::getYear, year)
        );
        if (existing != null) {
            existing.setTitle(request.title());
            existing.setSummary(request.summary());
            existing.setCoverMediaId(request.coverMediaId());
            yearProfileMapper.updateById(existing);
            return toVO(existing);
        }

        YearProfile profile = new YearProfile();
        profile.setUserId(userId);
        profile.setYear(year);
        profile.setTitle(request.title());
        profile.setSummary(request.summary());
        profile.setCoverMediaId(request.coverMediaId());
        try {
            yearProfileMapper.insert(profile);
        } catch (DuplicateKeyException e) {
            // 并发插入撞上 (user_id, year) 唯一索引 → 重查后按更新处理
            YearProfile raced = yearProfileMapper.selectOne(
                Wrappers.<YearProfile>lambdaQuery()
                    .eq(YearProfile::getUserId, userId)
                    .eq(YearProfile::getYear, year)
            );
            if (raced == null) {
                throw new BusinessException(ApiResponse.SYSTEM_ERROR, "年度资料保存失败，请重试");
            }
            raced.setTitle(request.title());
            raced.setSummary(request.summary());
            raced.setCoverMediaId(request.coverMediaId());
            yearProfileMapper.updateById(raced);
            return toVO(raced);
        }
        return toVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer year) {
        Long userId = requireLogin();
        // 不存在时删除 0 行，幂等成功
        yearProfileMapper.delete(Wrappers.<YearProfile>lambdaQuery()
            .eq(YearProfile::getUserId, userId)
            .eq(YearProfile::getYear, year));
    }

    // ---- 内部辅助 ----

    private Long requireLogin() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ApiResponse.UNAUTHORIZED, "未登录或登录已过期");
        }
        return userId;
    }

    private YearProfileVO toVO(YearProfile profile) {
        return new YearProfileVO(
            profile.getId(),
            profile.getYear(),
            profile.getTitle(),
            profile.getSummary(),
            profile.getCoverMediaId(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}