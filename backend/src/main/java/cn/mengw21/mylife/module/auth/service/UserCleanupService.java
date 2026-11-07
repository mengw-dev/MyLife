package cn.mengw21.mylife.module.auth.service;

/**
 * 用户数据清理服务（《03-数据库设计文档》6.2 删除策略：用户）
 * <p>
 * 注销账号时由专门的业务按依赖顺序清理该用户的全部数据与媒体文件，
 * 之后再删除用户本体；不覆盖其他用户的数据。
 */
public interface UserCleanupService {

    /**
     * 删除指定用户的全部个人业务数据（含关系表、媒体文件物理文件）
     *
     * @param userId 用户 ID
     */
    void cleanupAllUserData(Long userId);
}