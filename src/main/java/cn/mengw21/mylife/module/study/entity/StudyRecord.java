package cn.mengw21.mylife.module.study.entity;

import cn.mengw21.mylife.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 学习记录实体，对应表 {@code study_record}（对齐《03-数据库设计文档》3.7）
 * <p>
 * 与知识笔记分开存储：一篇笔记可关联多条学习记录；
 * 删除笔记后 note_id 置空但保留 note_title_snapshot 标题快照。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("study_record")
public class StudyRecord extends BaseEntity {

    /** 所属用户 */
    private Long userId;

    /** 关联知识笔记 ID，可空（修改时允许置空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long noteId;

    /** 笔记标题快照（笔记删除后保留，修改时允许更新） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String noteTitleSnapshot;

    /** 人生阶段 ID，可空（修改时允许置空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long stageId;

    /** 学习标题，可选（修改时允许清空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String title;

    /** 学习内容 */
    private String content;

    /** 当时理解 */
    private String understanding;

    /** 遇到的问题 */
    private String questions;

    /** 学习日期 */
    private LocalDate studyDate;
}