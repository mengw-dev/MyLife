package cn.mengw21.mylife.module.stage.mapper;

import cn.mengw21.mylife.module.stage.entity.LifeStage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人生阶段 Mapper，CRUD 由 MyBatis-Plus BaseMapper 提供
 */
@Mapper
public interface LifeStageMapper extends BaseMapper<LifeStage> {
}