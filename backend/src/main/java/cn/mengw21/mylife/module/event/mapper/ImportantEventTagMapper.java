package cn.mengw21.mylife.module.event.mapper;

import cn.mengw21.mylife.module.event.entity.ImportantEventTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件标签关联 Mapper
 */
@Mapper
public interface ImportantEventTagMapper extends BaseMapper<ImportantEventTag> {
}