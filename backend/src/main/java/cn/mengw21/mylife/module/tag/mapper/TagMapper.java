package cn.mengw21.mylife.module.tag.mapper;

import cn.mengw21.mylife.module.tag.entity.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签 Mapper，CRUD 由 MyBatis-Plus BaseMapper 提供
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}