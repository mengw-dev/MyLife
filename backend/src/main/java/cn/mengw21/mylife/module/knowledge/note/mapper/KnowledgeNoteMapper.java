package cn.mengw21.mylife.module.knowledge.note.mapper;

import cn.mengw21.mylife.module.knowledge.note.entity.KnowledgeNote;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识笔记 Mapper，CRUD 由 MyBatis-Plus BaseMapper 提供
 */
@Mapper
public interface KnowledgeNoteMapper extends BaseMapper<KnowledgeNote> {
}