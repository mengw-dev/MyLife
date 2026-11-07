package cn.mengw21.mylife.module.knowledge.note.service.impl;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import cn.mengw21.mylife.common.service.MediaOwnershipGuard;
import cn.mengw21.mylife.common.service.TagService;
import cn.mengw21.mylife.module.knowledge.category.entity.KnowledgeCategory;
import cn.mengw21.mylife.module.knowledge.category.mapper.KnowledgeCategoryMapper;
import cn.mengw21.mylife.module.knowledge.note.dto.KnowledgeNoteSaveRequest;
import cn.mengw21.mylife.module.knowledge.note.entity.KnowledgeNote;
import cn.mengw21.mylife.module.knowledge.note.entity.KnowledgeNoteMedia;
import cn.mengw21.mylife.module.knowledge.note.entity.KnowledgeNoteTag;
import cn.mengw21.mylife.module.knowledge.note.mapper.KnowledgeNoteMapper;
import cn.mengw21.mylife.module.knowledge.note.mapper.KnowledgeNoteMediaMapper;
import cn.mengw21.mylife.module.knowledge.note.mapper.KnowledgeNoteTagMapper;
import cn.mengw21.mylife.module.knowledge.note.service.KnowledgeNoteService;
import cn.mengw21.mylife.module.knowledge.note.vo.KnowledgeNoteVO;
import cn.mengw21.mylife.module.study.entity.StudyRecord;
import cn.mengw21.mylife.module.study.entity.StudyRecordTag;
import cn.mengw21.mylife.module.study.mapper.StudyRecordMapper;
import cn.mengw21.mylife.module.study.mapper.StudyRecordTagMapper;
import cn.mengw21.mylife.module.study.vo.StudyRecordVO;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识笔记服务实现
 */
@Service
@RequiredArgsConstructor
public class KnowledgeNoteServiceImpl implements KnowledgeNoteService {

    private final KnowledgeNoteMapper noteMapper;
    private final KnowledgeNoteMediaMapper noteMediaMapper;
    private final KnowledgeNoteTagMapper noteTagMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final StudyRecordTagMapper studyRecordTagMapper;
    private final TagMapper tagMapper;
    private final TagService tagService;
    private final MediaOwnershipGuard mediaOwnershipGuard;

    @Override
    public PageResponse<KnowledgeNoteVO> page(long pageNo, long pageSize, String keyword, Long categoryId, String tag) {
        Long userId = requireLogin();

        var query = Wrappers.<KnowledgeNote>lambdaQuery().eq(KnowledgeNote::getUserId, userId);
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(KnowledgeNote::getTitle, keyword).or().like(KnowledgeNote::getContentMarkdown, keyword));
        }
        if (categoryId != null) {
            query.eq(KnowledgeNote::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(tag)) {
            query.exists("SELECT 1 FROM knowledge_note_tag knt JOIN tag t ON knt.tag_id = t.id "
                + "WHERE knt.note_id = knowledge_note.id AND t.user_id = {0} AND t.name = {1}", userId, tag.trim());
        }
        // 对齐索引 (user_id, category_id, updated_at, id)
        query.orderByDesc(KnowledgeNote::getUpdatedAt).orderByDesc(KnowledgeNote::getId);

        IPage<KnowledgeNote> result = noteMapper.selectPage(new Page<>(pageNo, pageSize), query);
        Map<Long, List<String>> tagMap = collectTagNames(result.getRecords());
        Map<Long, List<Long>> mediaMap = collectMediaIds(result.getRecords());
        return PageResponse.of(result, note -> toVO(note, tagMap, mediaMap));
    }

    @Override
    public KnowledgeNoteVO getDetail(Long id) {
        KnowledgeNote note = requireOwned(id);
        return toVO(note, collectTagNames(List.of(note)), collectMediaIds(List.of(note)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(KnowledgeNoteSaveRequest request) {
        Long userId = requireLogin();
        requireOwnedCategory(userId, request.categoryId());
        List<Long> mediaIds = request.mediaIds() == null ? List.of() : request.mediaIds();
        mediaOwnershipGuard.requireOwnedList(userId, mediaIds);

        KnowledgeNote note = new KnowledgeNote();
        note.setUserId(userId);
        applySave(note, request);
        noteMapper.insert(note);

        saveTags(userId, note.getId(), request.tagNames());
        saveMedia(note.getId(), mediaIds);
        return note.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, KnowledgeNoteSaveRequest request) {
        KnowledgeNote note = requireOwned(id);
        requireOwnedCategory(note.getUserId(), request.categoryId());
        List<Long> mediaIds = request.mediaIds() == null ? List.of() : request.mediaIds();
        mediaOwnershipGuard.requireOwnedList(note.getUserId(), mediaIds);

        applySave(note, request);
        noteMapper.updateById(note);

        // tagNames / mediaIds 表示完整替换对应关联集合
        noteTagMapper.delete(Wrappers.<KnowledgeNoteTag>lambdaQuery().eq(KnowledgeNoteTag::getNoteId, id));
        noteMediaMapper.delete(Wrappers.<KnowledgeNoteMedia>lambdaQuery().eq(KnowledgeNoteMedia::getNoteId, id));
        saveTags(note.getUserId(), id, request.tagNames());
        saveMedia(id, mediaIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KnowledgeNote note = requireOwned(id);
        // 学习记录保留（note_id / stage 外键置空由数据库完成），标题快照不丢
        noteMapper.deleteById(note.getId());
    }

    @Override
    public List<StudyRecordVO> listStudyRecords(Long id) {
        KnowledgeNote note = requireOwned(id);
        List<StudyRecord> records = studyRecordMapper.selectList(
            Wrappers.<StudyRecord>lambdaQuery()
                .eq(StudyRecord::getUserId, note.getUserId())
                .eq(StudyRecord::getNoteId, id)
                .orderByDesc(StudyRecord::getStudyDate)
                .orderByDesc(StudyRecord::getId)
        );
        Map<Long, List<String>> tagMap = collectStudyTagNames(records);
        return records.stream()
            .map(record -> new StudyRecordVO(
                record.getId(),
                record.getTitle(),
                record.getContent(),
                record.getUnderstanding(),
                record.getQuestions(),
                record.getStudyDate(),
                record.getNoteId(),
                record.getNoteTitleSnapshot(),
                record.getStageId(),
                tagMap.getOrDefault(record.getId(), List.of()),
                record.getCreatedAt(),
                record.getUpdatedAt()
            ))
            .toList();
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
     * 校验笔记存在且属于当前用户（跨用户 → 404）
     */
    private KnowledgeNote requireOwned(Long id) {
        Long userId = requireLogin();
        KnowledgeNote note = noteMapper.selectOne(
            Wrappers.<KnowledgeNote>lambdaQuery()
                .eq(KnowledgeNote::getId, id)
                .eq(KnowledgeNote::getUserId, userId)
        );
        if (note == null) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "知识笔记不存在");
        }
        return note;
    }

    /**
     * 分类归属校验（外键只保证 ID 存在）
     */
    private void requireOwnedCategory(Long userId, Long categoryId) {
        Long count = categoryMapper.selectCount(
            Wrappers.<KnowledgeCategory>lambdaQuery()
                .eq(KnowledgeCategory::getId, categoryId)
                .eq(KnowledgeCategory::getUserId, userId)
        );
        if (count == 0) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "分类不存在");
        }
    }

    private void applySave(KnowledgeNote note, KnowledgeNoteSaveRequest request) {
        note.setTitle(request.title());
        note.setContentMarkdown(request.contentMarkdown());
        note.setCategoryId(request.categoryId());
    }

    private void saveTags(Long userId, Long noteId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        Map<String, Tag> tags = tagService.loadOrCreate(userId, tagNames);
        tags.values().stream().map(Tag::getId).distinct().forEach(tagId -> {
            KnowledgeNoteTag link = new KnowledgeNoteTag();
            link.setNoteId(noteId);
            link.setTagId(tagId);
            noteTagMapper.insert(link);
        });
    }

    private void saveMedia(Long noteId, List<Long> mediaIds) {
        mediaIds.stream().filter(id -> id != null).distinct().forEach(mediaId -> {
            KnowledgeNoteMedia link = new KnowledgeNoteMedia();
            link.setNoteId(noteId);
            link.setMediaId(mediaId);
            noteMediaMapper.insert(link);
        });
    }

    /**
     * 批量装配：noteId → 标签名列表（避免 N+1）
     */
    private Map<Long, List<String>> collectTagNames(List<KnowledgeNote> notes) {
        List<Long> noteIds = notes.stream().map(KnowledgeNote::getId).toList();
        if (noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<KnowledgeNoteTag> links = noteTagMapper.selectList(
            Wrappers.<KnowledgeNoteTag>lambdaQuery().in(KnowledgeNoteTag::getNoteId, noteIds));
        if (links.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = links.stream().map(KnowledgeNoteTag::getTagId).distinct().toList();
        Map<Long, String> tagNameMap = tagMapper.selectBatchIds(tagIds).stream()
            .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));
        return links.stream().collect(Collectors.groupingBy(
            KnowledgeNoteTag::getNoteId,
            Collectors.mapping(link -> tagNameMap.get(link.getTagId()), Collectors.toList())
        ));
    }

    /**
     * 批量装配：noteId → 照片 ID 列表
     */
    private Map<Long, List<Long>> collectMediaIds(List<KnowledgeNote> notes) {
        List<Long> noteIds = notes.stream().map(KnowledgeNote::getId).toList();
        if (noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return noteMediaMapper.selectList(
                Wrappers.<KnowledgeNoteMedia>lambdaQuery().in(KnowledgeNoteMedia::getNoteId, noteIds))
            .stream()
            .collect(Collectors.groupingBy(
                KnowledgeNoteMedia::getNoteId,
                Collectors.mapping(KnowledgeNoteMedia::getMediaId, Collectors.toList())
            ));
    }

    /**
     * 批量装配：学习记录 → 标签名列表
     */
    private Map<Long, List<String>> collectStudyTagNames(List<StudyRecord> records) {
        List<Long> recordIds = records.stream().map(StudyRecord::getId).toList();
        if (recordIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StudyRecordTag> links = studyRecordTagMapper.selectList(
            Wrappers.<StudyRecordTag>lambdaQuery().in(StudyRecordTag::getRecordId, recordIds));
        if (links.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = links.stream().map(StudyRecordTag::getTagId).distinct().toList();
        Map<Long, String> tagNameMap = tagMapper.selectBatchIds(tagIds).stream()
            .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));
        return links.stream().collect(Collectors.groupingBy(
            StudyRecordTag::getRecordId,
            Collectors.mapping(link -> tagNameMap.get(link.getTagId()), Collectors.toList())
        ));
    }

    private KnowledgeNoteVO toVO(KnowledgeNote note, Map<Long, List<String>> tagMap, Map<Long, List<Long>> mediaMap) {
        return new KnowledgeNoteVO(
            note.getId(),
            note.getTitle(),
            note.getContentMarkdown(),
            note.getCategoryId(),
            tagMap.getOrDefault(note.getId(), List.of()),
            mediaMap.getOrDefault(note.getId(), List.of()),
            note.getCreatedAt(),
            note.getUpdatedAt()
        );
    }
}