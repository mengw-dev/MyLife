package cn.mengw21.mylife.module.study.service.impl;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import cn.mengw21.mylife.common.service.TagService;
import cn.mengw21.mylife.module.knowledge.note.entity.KnowledgeNote;
import cn.mengw21.mylife.module.knowledge.note.mapper.KnowledgeNoteMapper;
import cn.mengw21.mylife.module.stage.entity.LifeStage;
import cn.mengw21.mylife.module.stage.mapper.LifeStageMapper;
import cn.mengw21.mylife.module.study.dto.StudyRecordSaveRequest;
import cn.mengw21.mylife.module.study.entity.StudyRecord;
import cn.mengw21.mylife.module.study.entity.StudyRecordTag;
import cn.mengw21.mylife.module.study.mapper.StudyRecordMapper;
import cn.mengw21.mylife.module.study.mapper.StudyRecordTagMapper;
import cn.mengw21.mylife.module.study.service.StudyRecordService;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习记录服务实现
 */
@Service
@RequiredArgsConstructor
public class StudyRecordServiceImpl implements StudyRecordService {

    private final StudyRecordMapper studyRecordMapper;
    private final StudyRecordTagMapper studyRecordTagMapper;
    private final KnowledgeNoteMapper noteMapper;
    private final LifeStageMapper stageMapper;
    private final TagMapper tagMapper;
    private final TagService tagService;

    @Override
    public PageResponse<StudyRecordVO> page(long pageNo, long pageSize, LocalDate from, LocalDate to,
                                            Long noteId, Long stageId, String keyword, String tag) {
        Long userId = requireLogin();

        var query = Wrappers.<StudyRecord>lambdaQuery().eq(StudyRecord::getUserId, userId);
        if (from != null) {
            query.ge(StudyRecord::getStudyDate, from);
        }
        if (to != null) {
            query.le(StudyRecord::getStudyDate, to);
        }
        if (noteId != null) {
            query.eq(StudyRecord::getNoteId, noteId);
        }
        if (stageId != null) {
            query.eq(StudyRecord::getStageId, stageId);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(StudyRecord::getTitle, keyword).or().like(StudyRecord::getContent, keyword));
        }
        if (StringUtils.hasText(tag)) {
            query.exists("SELECT 1 FROM study_record_tag srt JOIN tag t ON srt.tag_id = t.id "
                + "WHERE srt.record_id = study_record.id AND t.user_id = {0} AND t.name = {1}", userId, tag.trim());
        }
        query.orderByDesc(StudyRecord::getStudyDate).orderByDesc(StudyRecord::getId);

        IPage<StudyRecord> result = studyRecordMapper.selectPage(new Page<>(pageNo, pageSize), query);
        Map<Long, List<String>> tagMap = collectTagNames(result.getRecords());
        return PageResponse.of(result, record -> toVO(record, tagMap));
    }

    @Override
    public StudyRecordVO getDetail(Long id) {
        StudyRecord record = requireOwned(id);
        return toVO(record, collectTagNames(List.of(record)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StudyRecordSaveRequest request) {
        Long userId = requireLogin();
        // 关联归属校验：笔记 / 阶段（外键只保证存在，归属必须 Service 校验）
        String noteTitle = requireOwnedNote(userId, request.noteId());
        requireOwnedStage(userId, request.stageId());

        StudyRecord record = new StudyRecord();
        record.setUserId(userId);
        applySave(record, request, noteTitle);
        studyRecordMapper.insert(record);

        saveTags(userId, record.getId(), request.tagNames());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, StudyRecordSaveRequest request) {
        StudyRecord record = requireOwned(id);
        String noteTitle = requireOwnedNote(record.getUserId(), request.noteId());
        requireOwnedStage(record.getUserId(), request.stageId());

        // noteId 有值时重写快照；noteId 置空时保留原快照（历史痕迹不丢）
        applySave(record, request, noteTitle);
        studyRecordMapper.updateById(record);

        // tagNames 表示完整替换关联集合
        studyRecordTagMapper.delete(Wrappers.<StudyRecordTag>lambdaQuery().eq(StudyRecordTag::getRecordId, id));
        saveTags(record.getUserId(), id, request.tagNames());
    }

    @Override
    public void delete(Long id) {
        StudyRecord record = requireOwned(id);
        // 标签关联随外键级联删除
        studyRecordMapper.deleteById(record.getId());
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
     * 校验学习记录存在且属于当前用户（跨用户 → 404）
     */
    private StudyRecord requireOwned(Long id) {
        Long userId = requireLogin();
        StudyRecord record = studyRecordMapper.selectOne(
            Wrappers.<StudyRecord>lambdaQuery()
                .eq(StudyRecord::getId, id)
                .eq(StudyRecord::getUserId, userId)
        );
        if (record == null) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "学习记录不存在");
        }
        return record;
    }

    /**
     * 关联笔记归属校验，返回笔记标题（用于快照）；未关联返回 null
     */
    private String requireOwnedNote(Long userId, Long noteId) {
        if (noteId == null) {
            return null;
        }
        KnowledgeNote note = noteMapper.selectOne(
            Wrappers.<KnowledgeNote>lambdaQuery()
                .eq(KnowledgeNote::getId, noteId)
                .eq(KnowledgeNote::getUserId, userId)
        );
        if (note == null) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "知识笔记不存在");
        }
        return note.getTitle();
    }

    /**
     * 关联阶段归属校验
     */
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

    private void applySave(StudyRecord record, StudyRecordSaveRequest request, String noteTitleSnapshot) {
        record.setTitle(request.title());
        record.setContent(request.content());
        record.setUnderstanding(request.understanding());
        record.setQuestions(request.questions());
        record.setStudyDate(request.studyDate());
        record.setNoteId(request.noteId());
        // 关联了笔记才覆盖快照；置空时保留原快照
        if (request.noteId() != null) {
            record.setNoteTitleSnapshot(noteTitleSnapshot);
        }
        record.setStageId(request.stageId());
    }

    private void saveTags(Long userId, Long recordId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        Map<String, Tag> tags = tagService.loadOrCreate(userId, tagNames);
        tags.values().stream().map(Tag::getId).distinct().forEach(tagId -> {
            StudyRecordTag link = new StudyRecordTag();
            link.setRecordId(recordId);
            link.setTagId(tagId);
            studyRecordTagMapper.insert(link);
        });
    }

    /**
     * 批量装配：recordId → 标签名列表（避免 N+1）
     */
    private Map<Long, List<String>> collectTagNames(List<StudyRecord> records) {
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

    private StudyRecordVO toVO(StudyRecord record, Map<Long, List<String>> tagMap) {
        return new StudyRecordVO(
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
        );
    }
}