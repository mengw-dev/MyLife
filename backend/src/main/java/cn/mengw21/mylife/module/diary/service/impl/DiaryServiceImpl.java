package cn.mengw21.mylife.module.diary.service.impl;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import cn.mengw21.mylife.common.service.MediaOwnershipGuard;
import cn.mengw21.mylife.common.service.TagService;
import cn.mengw21.mylife.module.diary.dto.DiarySaveRequest;
import cn.mengw21.mylife.module.diary.entity.Diary;
import cn.mengw21.mylife.module.diary.entity.DiaryMedia;
import cn.mengw21.mylife.module.diary.entity.DiaryTag;
import cn.mengw21.mylife.module.diary.mapper.DiaryMapper;
import cn.mengw21.mylife.module.diary.mapper.DiaryMediaMapper;
import cn.mengw21.mylife.module.diary.mapper.DiaryTagMapper;
import cn.mengw21.mylife.module.diary.service.DiaryService;
import cn.mengw21.mylife.module.diary.vo.DiaryVO;
import cn.mengw21.mylife.module.stage.entity.LifeStage;
import cn.mengw21.mylife.module.stage.mapper.LifeStageMapper;
import cn.mengw21.mylife.module.tag.entity.Tag;
import cn.mengw21.mylife.module.tag.mapper.TagMapper;
import cn.mengw21.mylife.security.CurrentUser;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日记服务实现
 * <p>
 * 数据隔离：所有 SQL 自带 user_id 条件；关联的人生阶段 / 照片逐一校验归属（《03-数据库设计文档》6.3）。
 * 分页装配：标签与照片用两次批量查询完成，避免 N+1。
 */
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private static final long DEFAULT_PAGE_SIZE = 20L;

    private final DiaryMapper diaryMapper;
    private final DiaryMediaMapper diaryMediaMapper;
    private final DiaryTagMapper diaryTagMapper;
    private final LifeStageMapper lifeStageMapper;
    private final TagMapper tagMapper;
    private final TagService tagService;
    private final MediaOwnershipGuard mediaOwnershipGuard;

    @Override
    public PageResponse<DiaryVO> page(long pageNo, long pageSize, String keyword, LocalDate from,
                                      LocalDate to, Long stageId, String tag) {
        Long userId = requireLogin();

        var query = Wrappers.<Diary>lambdaQuery().eq(Diary::getUserId, userId);
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(Diary::getTitle, keyword).or().like(Diary::getContent, keyword));
        }
        if (from != null) {
            query.ge(Diary::getRecordDate, from);
        }
        if (to != null) {
            query.le(Diary::getRecordDate, to);
        }
        if (stageId != null) {
            query.eq(Diary::getStageId, stageId);
        }
        if (StringUtils.hasText(tag)) {
            // 标签名筛选：通过关联表 EXISTS 子查询（标签归属由 user_id 保证）
            query.exists("SELECT 1 FROM diary_tag dt JOIN tag t ON dt.tag_id = t.id "
                + "WHERE dt.diary_id = diary.id AND t.user_id = {0} AND t.name = {1}", userId, tag.trim());
        }
        query.orderByDesc(Diary::getRecordDate).orderByDesc(Diary::getId);

        IPage<Diary> result = diaryMapper.selectPage(new Page<>(pageNo, pageSize == 0 ? DEFAULT_PAGE_SIZE : pageSize), query);
        // 先批量装配标签 / 照片，再逐条转 VO（避免 N+1）
        Map<Long, List<String>> tagMap = collectTagNames(result.getRecords());
        Map<Long, List<Long>> mediaMap = collectMediaIds(result.getRecords());
        return PageResponse.of(result, diary -> toVO(diary, tagMap, mediaMap));
    }

    @Override
    public DiaryVO getDetail(Long id) {
        Diary diary = requireOwned(id);
        Map<Long, List<String>> tagMap = collectTagNames(List.of(diary));
        Map<Long, List<Long>> mediaMap = collectMediaIds(List.of(diary));
        return toVO(diary, tagMap, mediaMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DiarySaveRequest request) {
        Long userId = requireLogin();
        requireOwnedStage(userId, request.stageId());
        // 照片归属校验（他人照片 / 不存在 → 404）
        List<Long> mediaIds = request.mediaIds() == null ? List.of() : request.mediaIds();
        mediaOwnershipGuard.requireOwnedList(userId, mediaIds);

        Diary diary = new Diary();
        diary.setUserId(userId);
        applySave(diary, request);
        diaryMapper.insert(diary);

        saveTags(userId, diary.getId(), request.tagNames());
        saveMedia(diary.getId(), mediaIds);
        return diary.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DiarySaveRequest request) {
        Diary diary = requireOwned(id);
        requireOwnedStage(diary.getUserId(), request.stageId());
        List<Long> mediaIds = request.mediaIds() == null ? List.of() : request.mediaIds();
        mediaOwnershipGuard.requireOwnedList(diary.getUserId(), mediaIds);

        applySave(diary, request);
        diaryMapper.updateById(diary);

        // tagNames / mediaIds 表示完整替换对应关联集合
        diaryTagMapper.delete(Wrappers.<DiaryTag>lambdaQuery().eq(DiaryTag::getDiaryId, id));
        diaryMediaMapper.delete(Wrappers.<DiaryMedia>lambdaQuery().eq(DiaryMedia::getDiaryId, id));
        saveTags(diary.getUserId(), id, request.tagNames());
        saveMedia(id, mediaIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Diary diary = requireOwned(id);
        // 关联表随外键级联删除；事件保留并解除 diary_id；被引用的照片不删除
        diaryMapper.deleteById(diary.getId());
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
     * 校验日记存在且属于当前用户（跨用户 → 404，不暴露资源存在性）
     */
    private Diary requireOwned(Long id) {
        Long userId = requireLogin();
        Diary diary = diaryMapper.selectOne(
            Wrappers.<Diary>lambdaQuery()
                .eq(Diary::getId, id)
                .eq(Diary::getUserId, userId)
        );
        if (diary == null) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "日记不存在");
        }
        return diary;
    }

    /**
     * 关联阶段归属校验：外键只保证 ID 存在，归属必须由 Service 校验
     */
    private void requireOwnedStage(Long userId, Long stageId) {
        if (stageId == null) {
            return;
        }
        Long count = lifeStageMapper.selectCount(
            Wrappers.<LifeStage>lambdaQuery()
                .eq(LifeStage::getId, stageId)
                .eq(LifeStage::getUserId, userId)
        );
        if (count == 0) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "阶段不存在");
        }
    }

    private void applySave(Diary diary, DiarySaveRequest request) {
        diary.setTitle(request.title());
        diary.setContent(request.content());
        diary.setRecordDate(request.recordDate());
        diary.setStageId(request.stageId());
    }

    /**
     * 保存标签关联（名称不存在时自动创建）
     */
    private void saveTags(Long userId, Long diaryId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        Map<String, Tag> tags = tagService.loadOrCreate(userId, tagNames);
        tags.values().stream().map(Tag::getId).distinct().forEach(tagId -> {
            DiaryTag link = new DiaryTag();
            link.setDiaryId(diaryId);
            link.setTagId(tagId);
            diaryTagMapper.insert(link);
        });
    }

    /**
     * 保存照片关联（mediaIds 去重后插入）
     */
    private void saveMedia(Long diaryId, List<Long> mediaIds) {
        mediaIds.stream().filter(id -> id != null).distinct().forEach(mediaId -> {
            DiaryMedia link = new DiaryMedia();
            link.setDiaryId(diaryId);
            link.setMediaId(mediaId);
            diaryMediaMapper.insert(link);
        });
    }

    /**
     * 批量装配：diaryId → 标签名列表（两次批量查询，避免 N+1）
     */
    private Map<Long, List<String>> collectTagNames(List<Diary> diaries) {
        List<Long> diaryIds = diaries.stream().map(Diary::getId).toList();
        if (diaryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DiaryTag> links = diaryTagMapper.selectList(
            Wrappers.<DiaryTag>lambdaQuery().in(DiaryTag::getDiaryId, diaryIds));
        if (links.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = links.stream().map(DiaryTag::getTagId).distinct().toList();
        Map<Long, String> tagNameMap = tagMapper.selectBatchIds(tagIds).stream()
            .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));
        return links.stream().collect(Collectors.groupingBy(
            DiaryTag::getDiaryId,
            Collectors.mapping(link -> tagNameMap.get(link.getTagId()), Collectors.toList())
        ));
    }

    /**
     * 批量装配：diaryId → 照片 ID 列表（一次批量查询，避免 N+1）
     */
    private Map<Long, List<Long>> collectMediaIds(List<Diary> diaries) {
        List<Long> diaryIds = diaries.stream().map(Diary::getId).toList();
        if (diaryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return diaryMediaMapper.selectList(
                Wrappers.<DiaryMedia>lambdaQuery().in(DiaryMedia::getDiaryId, diaryIds))
            .stream()
            .collect(Collectors.groupingBy(
                DiaryMedia::getDiaryId,
                Collectors.mapping(DiaryMedia::getMediaId, Collectors.toList())
            ));
    }

    private DiaryVO toVO(Diary diary, Map<Long, List<String>> tagMap, Map<Long, List<Long>> mediaMap) {
        return new DiaryVO(
            diary.getId(),
            diary.getTitle(),
            diary.getContent(),
            diary.getRecordDate(),
            diary.getStageId(),
            tagMap.getOrDefault(diary.getId(), List.of()),
            mediaMap.getOrDefault(diary.getId(), List.of()),
            diary.getCreatedAt(),
            diary.getUpdatedAt()
        );
    }
}