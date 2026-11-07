package cn.mengw21.mylife.module.auth.mapper;

import cn.mengw21.mylife.module.auth.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper，CRUD 由 MyBatis-Plus BaseMapper 提供
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}