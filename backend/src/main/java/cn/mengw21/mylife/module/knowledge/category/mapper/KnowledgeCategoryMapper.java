package cn.mengw21.mylife.module.knowledge.category.mapper;

import cn.mengw21.mylife.module.knowledge.category.entity.KnowledgeCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识分类 Mapper，CRUD 由 MyBatis-Plus BaseMapper 提供
 */
@Mapper
public interface KnowledgeCategoryMapper extends BaseMapper<KnowledgeCategory> {
}