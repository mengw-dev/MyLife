package cn.mengw21.mylife.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

/**
 * 分页响应封装（对齐《02-API接口文档》1.5 分页格式）
 * <p>
 * <pre>
 * {
 *   "records": [],
 *   "page": 1,
 *   "pageSize": 20,
 *   "total": 0,
 *   "pages": 0
 * }
 * </pre>
 *
 * @param records  当前页数据
 * @param page     当前页码（从 1 开始）
 * @param pageSize 每页条数
 * @param total    总记录数
 * @param pages    总页数
 */
public record PageResponse<T>(List<T> records, long page, long pageSize, long total, long pages) {

    /**
     * 直接把 MyBatis-Plus 的分页对象转成统一结构
     *
     * @param page MyBatis-Plus 分页结果
     */
    public static <T> PageResponse<T> of(IPage<T> page) {
        return new PageResponse<>(
            page.getRecords(),
            page.getCurrent(),
            page.getSize(),
            page.getTotal(),
            page.getPages()
        );
    }

    /**
     * 把分页结果里的实体转成 VO 后再封装
     *
     * @param page      MyBatis-Plus 分页结果
     * @param converter 实体 → VO 的转换函数，context 中合理拆分 N+1 的批量查询
     */
    public static <E, T> PageResponse<T> of(IPage<E> page, Function<E, T> converter) {
        List<T> records = page.getRecords().stream().map(converter).toList();
        return new PageResponse<>(
            records,
            page.getCurrent(),
            page.getSize(),
            page.getTotal(),
            page.getPages()
        );
    }
}