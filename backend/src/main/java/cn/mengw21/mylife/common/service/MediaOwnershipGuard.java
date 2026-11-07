package cn.mengw21.mylife.common.service;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import cn.mengw21.mylife.module.media.entity.MediaFile;
import cn.mengw21.mylife.module.media.mapper.MediaFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 媒体资源归属守卫（《03-数据库设计文档》6.3 数据隔离）
 * <p>
 * 外键只能保证 ID 存在，不能保证关联对象属于同一用户，
 * 因此在建立任何"照片关联"前都必须用本守卫校验媒体归属：
 * 数量不符或存在他人资源时统一返回 404（不暴露资源存在性）。
 */
@Component
@RequiredArgsConstructor
public class MediaOwnershipGuard {

    private final MediaFileMapper mediaFileMapper;

    /**
     * 校验一组照片全部属于当前用户
     *
     * @param userId   当前用户 ID
     * @param mediaIds 照片 ID 集合（可空）
     * @return 去重后的媒体实体列表
     */
    public List<MediaFile> requireOwnedList(Long userId, List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> distinctIds = mediaIds.stream().filter(id -> id != null).distinct().toList();
        List<MediaFile> files = mediaFileMapper.selectBatchIds(distinctIds);
        boolean allOwned = files.size() == distinctIds.size()
            && files.stream().allMatch(file -> file.getUserId().equals(userId));
        if (!allOwned) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "照片不存在");
        }
        return files;
    }

    /**
     * 校验单张照片属于当前用户（用于封面等单选引用）
     *
     * @return 媒体实体；mediaId 为 null 时返回 null
     */
    public MediaFile requireOwnedOne(Long userId, Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaFile file = mediaFileMapper.selectById(mediaId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "照片不存在");
        }
        return file;
    }
}