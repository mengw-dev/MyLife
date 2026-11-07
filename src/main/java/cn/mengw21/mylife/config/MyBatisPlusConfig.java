package cn.mengw21.mylife.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 * <p>
 * 注册分页插件（需配合 mybatis-plus-jsqlparser 依赖，否则 PaginationInnerInterceptor 找不到）。
 * 逻辑删除、自动填充等插件在业务需要时再追加。
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 分页默认 20 条、最大 100 条（《02-API接口文档》14.7），防止恶意传超大 size
        pagination.setMaxLimit(100L);
        // 溢出总页数后不进行处理（返回空数据），避免 count 查询浪费
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
