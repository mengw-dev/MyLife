package cn.mengw21.mylife.module.event.mapper;

import cn.mengw21.mylife.module.event.entity.ImportantEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 重要事件 Mapper，CRUD 由 MyBatis-Plus BaseMapper 提供
 */
@Mapper
public interface ImportantEventMapper extends BaseMapper<ImportantEvent> {
}