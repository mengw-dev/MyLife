package cn.mengw21.mylife.module.auth.service.impl;

import cn.mengw21.mylife.module.auth.service.UserCleanupService;
import cn.mengw21.mylife.module.diary.entity.Diary;
import cn.mengw21.mylife.module.diary.mapper.DiaryMapper;
import cn.mengw21.mylife.module.event.entity.ImportantEvent;
import cn.mengw21.mylife.module.event.mapper.ImportantEventMapper;
import cn.mengw21.mylife.module.knowledge.category.entity.KnowledgeCategory;
import cn.mengw21.mylife.module.knowledge.category.mapper.KnowledgeCategoryMapper;
import cn.mengw21.mylife.module.knowledge.note.entity.KnowledgeNote;
import cn.mengw21.mylife.module.knowledge.note.mapper.KnowledgeNoteMapper;
import cn.mengw21.mylife.module.media.entity.MediaFile;
import cn.mengw21.mylife.module.media.mapper.MediaFileMapper;
import cn.mengw21.mylife.module.media.service.MediaStorageService;
import cn.mengw21.mylife.module.stage.entity.LifeStage;
import cn.mengw21.mylife.module.stage.mapper.LifeStageMapper;
import cn.mengw21.mylife.module.study.entity.StudyRecord;
import cn.mengw21.mylife.module.study.mapper.StudyRecordMapper;
import cn.mengw21.mylife.module.tag.entity.Tag;
import cn.mengw21.mylife.module.tag.mapper.TagMapper;
import cn.mengw21.mylife.module.yearprofile.entity.YearProfile;
import cn.mengw21.mylife.module.yearprofile.mapper.YearProfileMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户数据清理实现
 * <p>
 * 删除顺序严格遵守外键依赖：
 * <ol>
 *     <li>先删关联业务（学习、事件、日记、笔记、分类、阶段、年度资料），关联表随外键级联清理；</li>
 *     <li>再删媒体记录与素材文件、标签；</li>
 *     <li>最后删除用户本体（由调用方执行）。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupServiceImpl implements UserCleanupService {

    private final StudyRecordMapper studyRecordMapper;
    private final ImportantEventMapper importantEventMapper;
    private final DiaryMapper diaryMapper;
    private final KnowledgeNoteMapper knowledgeNoteMapper;
    private final KnowledgeCategoryMapper knowledgeCategoryMapper;
    private final LifeStageMapper lifeStageMapper;
    private final YearProfileMapper yearProfileMapper;
    private final MediaFileMapper mediaFileMapper;
    private final TagMapper tagMapper;
    private final MediaStorageService mediaStorageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupAllUserData(Long userId) {
        // 先取出该用户全部媒体文件（数据库记录删除后再清理物理文件，
        // 保证"数据库事务"与"文件系统"分离时即使文件删除失败也不影响数据一致性）
        List<MediaFile> mediaFiles = mediaFileMapper.selectList(
            Wrappers.<MediaFile>lambdaQuery().eq(MediaFile::getUserId, userId));

        // 1. 学习记录 / 重要事件 / 日记（关联表随外键级联删除）
        studyRecordMapper.delete(Wrappers.<StudyRecord>lambdaQuery().eq(StudyRecord::getUserId, userId));
        importantEventMapper.delete(Wrappers.<ImportantEvent>lambdaQuery().eq(ImportantEvent::getUserId, userId));
        diaryMapper.delete(Wrappers.<Diary>lambdaQuery().eq(Diary::getUserId, userId));

        // 2. 知识笔记 / 分类 / 阶段 / 年度资料
        knowledgeNoteMapper.delete(Wrappers.<KnowledgeNote>lambdaQuery().eq(KnowledgeNote::getUserId, userId));
        knowledgeCategoryMapper.delete(Wrappers.<KnowledgeCategory>lambdaQuery().eq(KnowledgeCategory::getUserId, userId));
        lifeStageMapper.delete(Wrappers.<LifeStage>lambdaQuery().eq(LifeStage::getUserId, userId));
        yearProfileMapper.delete(Wrappers.<YearProfile>lambdaQuery().eq(YearProfile::getUserId, userId));

        // 3. 媒体记录与标签（media_tag 随媒体 / 标签外键级联删除）
        mediaFileMapper.delete(Wrappers.<MediaFile>lambdaQuery().eq(MediaFile::getUserId, userId));
        tagMapper.delete(Wrappers.<Tag>lambdaQuery().eq(Tag::getUserId, userId));

        // 4. 物理文件（事务外尽力而为，失败只记日志不影响注销）
        mediaFiles.forEach(media -> {
            boolean deleted = mediaStorageService.deleteQuietly(media.getStorageKey());
            if (!deleted) {
                log.warn("注销清理：媒体文件删除失败，storageKey={}", media.getStorageKey());
            }
        });

        log.info("用户数据清理完成，userId={}，mediaFiles={}", userId, mediaFiles.size());
    }
}