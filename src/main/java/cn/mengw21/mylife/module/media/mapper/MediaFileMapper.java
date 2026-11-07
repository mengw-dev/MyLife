package cn.mengw21.mylife.module.media.mapper;

import cn.mengw21.mylife.module.media.entity.MediaFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 媒体文件 Mapper，CRUD 由 MyBatis-Plus BaseMapper 提供
 */
@Mapper
public interface MediaFileMapper extends BaseMapper<MediaFile> {
}