package cn.mengw21.mylife.module.event.service.impl;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import cn.mengw21.mylife.common.service.MediaOwnershipGuard;
import cn.mengw21.mylife.common.service.TagService;
import cn.mengw21.mylife.module.diary.entity.Diary;
import cn.mengw21.mylife.module.diary.mapper.DiaryMapper;
import cn.mengw21.mylife.module.event.dto.ImportantEventSaveRequest;
import cn.mengw21.mylife.module.event.entity.EventMedia;
import cn.mengw21.mylife.module.event.entity.ImportantEvent;
import cn.mengw21.mylife.module.event.entity.ImportantEventTag;
import cn.mengw21.mylife.module.event.mapper.EventMediaMapper;
import cn.mengw21.mylife.module.event.mapper.ImportantEventMapper;
import cn.mengw21.mylife.module.event.mapper.ImportantEventTagMapper;
import cn.mengw21.mylife.module.event.service.ImportantEventService;
import cn.mengw21.mylife.module.event.vo.ImportantEventVO;
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
 * 重要事件服务实现
 */
@Service
@RequiredArgsConstructor
public class ImportantEventServiceImpl implements ImportantEventService {

    private final ImportantEventMapper eventMapper;
    private final EventMediaMapper eventMediaMapper;
    private final ImportantEventTagMapper eventTagMapper;
    private final DiaryMapper diaryMapper;
    private final LifeStageMapper stageMapper;
    private final TagMapper tagMapper;
    private final TagService tagService;
    private final MediaOwnershipGuard mediaOwnershipGuard;

    @Override
    public PageResponse<ImportantEventVO> page(long pageNo, long pageSize, LocalDate from, LocalDate to,
                                               Long stageId, String tag) {
        Long userId = requireLogin();

        var query = Wrappers.<ImportantEvent>lambdaQuery().eq(ImportantEvent::getUserId, userId);
        if (from != null) {
            query.ge(ImportantEvent::getEventDate, from);
        }
        if (to != null) {
            query.le(ImportantEvent::getEventDate, to);
        }
        if (stageId != null) {
            query.eq(ImportantEvent::getStageId, stageId);
        }
        if (StringUtils.hasText(tag)) {
            query.exists("SELECT 1 FROM important_event_tag iet JOIN tag t ON iet.tag_id = t.id "
                + "WHERE iet.event_id = important_event.id AND t.user_id = {0} AND t.name = {1}", userId, tag.trim());
        }
        query.orderByDesc(ImportantEvent::getEventDate).orderByDesc(ImportantEvent::getId);

        IPage<ImportantEvent> result = eventMapper.selectPage(new Page<>(pageNo, pageSize), query);
        Map<Long, List<String>> tagMap = collectTagNames(result.getRecords());
        Map<Long, List<Long>> mediaMap = collectMediaIds(result.getRecords());
        return PageResponse.of(result, event -> toVO(event, tagMap, mediaMap));
    }

    @Override
    public ImportantEventVO getDetail(Long id) {
        ImportantEvent event = requireOwned(id);
        return toVO(event, collectTagNames(List.of(event)), collectMediaIds(List.of(event)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ImportantEventSaveRequest request) {
        Long userId = requireLogin();
        // 关联归属校验：阶段 / 日记（外键只保证 ID 存在）
        requireOwnedStage(userId, request.stageId());
        requireOwnedDiary(userId, request.diaryId());
        List<Long> mediaIds = request.mediaIds() == null ? List.of() : request.mediaIds();
        mediaOwnershipGuard.requireOwnedList(userId, mediaIds);

        ImportantEvent event = new ImportantEvent();
        event.setUserId(userId);
        applySave(event, request);
        eventMapper.insert(event);

        saveTags(userId, event.getId(), request.tagNames());
        saveMedia(event.getId(), mediaIds);
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ImportantEventSaveRequest request) {
        ImportantEvent event = requireOwned(id);
        requireOwnedStage(event.getUserId(), request.stageId());
        requireOwnedDiary(event.getUserId(), request.diaryId());
        List<Long> mediaIds = request.mediaIds() == null ? List.of() : request.mediaIds();
        mediaOwnershipGuard.requireOwnedList(event.getUserId(), mediaIds);

        applySave(event, request);
        eventMapper.updateById(event);

        // tagNames / mediaIds 表示完整替换对应关联集合
        eventTagMapper.delete(Wrappers.<ImportantEventTag>lambdaQuery().eq(ImportantEventTag::getEventId, id));
        eventMediaMapper.delete(Wrappers.<EventMedia>lambdaQuery().eq(EventMedia::getEventId, id));
        saveTags(event.getUserId(), id, request.tagNames());
        saveMedia(id, mediaIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ImportantEvent event = requireOwned(id);
        // 关联表随外键级联删除
        eventMapper.deleteById(event.getId());
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
     * 校验事件存在且属于当前用户（跨用户 → 404）
     */
    private ImportantEvent requireOwned(Long id) {
        Long userId = requireLogin();
        ImportantEvent event = eventMapper.selectOne(
            Wrappers.<ImportantEvent>lambdaQuery()
                .eq(ImportantEvent::getId, id)
                .eq(ImportantEvent::getUserId, userId)
        );
        if (event == null) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "事件不存在");
        }
        return event;
    }

    private void requireOwnedStage(Long userId, Long stageId) {
        if (stageId == null) {
            return;
        }
        Long count = stageMapper.selectCount(
            Wrappers.<LifeStage>lambdaQuery()
                .eq(LifeStage::getId, stageId)
                .eq(LifeStage::getUserId, userId)
        );
        if (count == 0) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "阶段不存在");
        }
    }

    private void requireOwnedDiary(Long userId, Long diaryId) {
        if (diaryId == null) {
            return;
        }
        Long count = diaryMapper.selectCount(
            Wrappers.<Diary>lambdaQuery()
                .eq(Diary::getId, diaryId)
                .eq(Diary::getUserId, userId)
        );
        if (count == 0) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "日记不存在");
        }
    }

    private void applySave(ImportantEvent event, ImportantEventSaveRequest request) {
        event.setTitle(request.title());
        event.setEventDate(request.eventDate());
        event.setDescription(request.description());
        event.setStageId(request.stageId());
        event.setDiaryId(request.diaryId());
    }

    private void saveTags(Long userId, Long eventId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        Map<String, Tag> tags = tagService.loadOrCreate(userId, tagNames);
        tags.values().stream().map(Tag::getId).distinct().forEach(tagId -> {
            ImportantEventTag link = new ImportantEventTag();
            link.setEventId(eventId);
            link.setTagId(tagId);
            eventTagMapper.insert(link);
        });
    }

    private void saveMedia(Long eventId, List<Long> mediaIds) {
        mediaIds.stream().filter(id -> id != null).distinct().forEach(mediaId -> {
            EventMedia link = new EventMedia();
            link.setEventId(eventId);
            link.setMediaId(mediaId);
            eventMediaMapper.insert(link);
        });
    }

    private Map<Long, List<String>> collectTagNames(List<ImportantEvent> events) {
        List<Long> eventIds = events.stream().map(ImportantEvent::getId).toList();
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ImportantEventTag> links = eventTagMapper.selectList(
            Wrappers.<ImportantEventTag>lambdaQuery().in(ImportantEventTag::getEventId, eventIds));
        if (links.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = links.stream().map(ImportantEventTag::getTagId).distinct().toList();
        Map<Long, String> tagNameMap = tagMapper.selectBatchIds(tagIds).stream()
            .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));
        return links.stream().collect(Collectors.groupingBy(
            ImportantEventTag::getEventId,
            Collectors.mapping(link -> tagNameMap.get(link.getTagId()), Collectors.toList())
        ));
    }

    private Map<Long, List<Long>> collectMediaIds(List<ImportantEvent> events) {
        List<Long> eventIds = events.stream().map(ImportantEvent::getId).toList();
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return eventMediaMapper.selectList(
                Wrappers.<EventMedia>lambdaQuery().in(EventMedia::getEventId, eventIds))
            .stream()
            .collect(Collectors.groupingBy(
                EventMedia::getEventId,
                Collectors.mapping(EventMedia::getMediaId, Collectors.toList())
            ));
    }

    private ImportantEventVO toVO(ImportantEvent event, Map<Long, List<String>> tagMap, Map<Long, List<Long>> mediaMap) {
        return new ImportantEventVO(
            event.getId(),
            event.getTitle(),
            event.getEventDate(),
            event.getDescription(),
            event.getStageId(),
            event.getDiaryId(),
            mediaMap.getOrDefault(event.getId(), List.of()),
            tagMap.getOrDefault(event.getId(), List.of()),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
}