package cn.mengw21.mylife.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具
 * <p>
 * 直接读取 Spring Security 的 {@code SecurityContext}，不额外维护 ThreadLocal，
 * 避免内存泄漏和状态不同步。
 * <p>
 * 业务 Service 层必须用这里的 ID 做数据隔离，严禁信任客户端传入的 userId。
 * <pre>
 * Long userId = CurrentUser.getUserId();
 * if (userId == null) {
 *     throw new BusinessException(Result.UNAUTHORIZED, "未登录");
 * }
 * </pre>
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * 获取当前登录用户
     *
     * @return 未登录或主体类型不匹配时返回 null
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser loginUser ? loginUser : null;
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 未登录时返回 null
     */
    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getId();
    }
}
