package cn.mengw21.mylife.module.diary.mapper;

import cn.mengw21.mylife.module.diary.entity.DiaryTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日记标签关联 Mapper
 */
@Mapper
public interface DiaryTagMapper extends BaseMapper<DiaryTag> {
}