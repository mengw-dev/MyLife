package cn.mengw21.mylife.module.knowledge.note.mapper;

import cn.mengw21.mylife.module.knowledge.note.entity.KnowledgeNoteTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识笔记标签关联 Mapper
 */
@Mapper
public interface KnowledgeNoteTagMapper extends BaseMapper<KnowledgeNoteTag> {
}