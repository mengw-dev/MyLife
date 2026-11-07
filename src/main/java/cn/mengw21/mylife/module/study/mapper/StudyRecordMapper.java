package cn.mengw21.mylife.module.study.mapper;

import cn.mengw21.mylife.module.study.entity.StudyRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习记录 Mapper，CRUD 由 MyBatis-Plus BaseMapper 提供
 */
@Mapper
public interface StudyRecordMapper extends BaseMapper<StudyRecord> {
}