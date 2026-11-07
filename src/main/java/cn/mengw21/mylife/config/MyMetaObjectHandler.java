package cn.mengw21.mylife.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * MyBatis-Plus 自动填充处理器
 * <p>
 * 配合 {@code BaseEntity} 上的 {@code @TableField(fill = ...)}，
 * 插入时自动写 createdAt / updatedAt，更新时自动刷新 updatedAt。
 * 时间统一使用 UTC（对齐《02-API接口文档》1.4 时间戳规范）。
 * <p>
 * 业务代码里不需要手动 set 这两个字段。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now(ZoneOffset.UTC));
    }
}