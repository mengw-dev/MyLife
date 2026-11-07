package cn.mengw21.mylife.module.yearprofile.mapper;

import cn.mengw21.mylife.module.yearprofile.entity.YearProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 年度资料 Mapper，CRUD 由 MyBatis-Plus BaseMapper 提供
 */
@Mapper
public interface YearProfileMapper extends BaseMapper<YearProfile> {
}