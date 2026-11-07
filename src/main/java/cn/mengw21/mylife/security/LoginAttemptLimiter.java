package cn.mengw21.mylife.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防暴力尝试限流器（PRD 12 安全要求 9：防止暴力登录）
 * <p>
 * 单体架构下的进程内实现：同一用户名在时间窗口内连续失败超过上限后，
 * 在冷却期内直接拒绝登录（无论密码是否正确），防止口令枚举。
 * 多实例部署时需要替换为集中式计数（V1 不引入 Redis，暂用进程内存即可）。
 */
@Component
public class LoginAttemptLimiter {

    /** 失败次数上限 */
    private static final int MAX_ATTEMPTS = 5;

    /** 冷却窗口（秒） */
    private static final long LOCK_SECONDS = 15 * 60L;

    /** 失败窗口（秒）：连续失败才累计，成功登录即清零 */
    private static final long WINDOW_SECONDS = 15 * 60L;

    /** username → 失败记录 */
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * 是否处于冷却期
     *
     * @param username 登录名
     * @return true 表示已被临时锁定
     */
    public boolean isBlocked(String username) {
        Attempt attempt = attempts.get(username);
        if (attempt == null) {
            return false;
        }
        Instant now = Instant.now();
        if (now.isAfter(attempt.lastFailAt.plusSeconds(LOCK_SECONDS))) {
            attempts.remove(username);
            return false;
        }
        return attempt.count >= MAX_ATTEMPTS;
    }

    /**
     * 记录一次登录失败
     */
    public void recordFailure(String username) {
        Instant now = Instant.now();
        Attempt attempt = attempts.compute(username, (key, old) -> {
            if (old == null || now.isAfter(old.lastFailAt.plusSeconds(WINDOW_SECONDS))) {
                return new Attempt(1, now);
            }
            return new Attempt(old.count + 1, now);
        });
        if (attempt.count >= MAX_ATTEMPTS && attempt.lastFailAt.equals(now)) {
            attempts.put(username, attempt);
        }
    }

    /**
     * 登录成功，清除该用户名的失败记录
     */
    public void reset(String username) {
        attempts.remove(username);
    }

    /**
     * 失败记录：连续失败次数 + 最近一次失败时间
     */
    private record Attempt(int count, Instant lastFailAt) {
    }
}