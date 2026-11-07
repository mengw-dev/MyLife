package cn.mengw21.mylife.module.auth.service.impl;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import cn.mengw21.mylife.module.auth.dto.ChangePasswordRequest;
import cn.mengw21.mylife.module.auth.dto.LoginRequest;
import cn.mengw21.mylife.module.auth.dto.RegisterRequest;
import cn.mengw21.mylife.module.auth.entity.User;
import cn.mengw21.mylife.module.auth.mapper.UserMapper;
import cn.mengw21.mylife.module.auth.service.AuthService;
import cn.mengw21.mylife.module.auth.service.UserCleanupService;
import cn.mengw21.mylife.module.auth.vo.UserVO;
import cn.mengw21.mylife.security.CurrentUser;
import cn.mengw21.mylife.security.LoginAttemptLimiter;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 认证服务实现
 * <p>
 * 关键规则：
 * <ul>
 *     <li>密码只存 BCrypt 密文，任何日志 / 接口不输出明文与密文；</li>
 *     <li>登录失败统一提示，不区分"用户不存在"与"密码错误"（防用户名枚举）；</li>
 *     <li>连续失败触发 {@link LoginAttemptLimiter} 冷却，防暴力登录；</li>
 *     <li>注销账号走 {@link UserCleanupService}，按外键依赖顺序清理全部个人数据。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final UserCleanupService userCleanupService;

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        // 用户名唯一性校验
        Long exists = userMapper.selectCount(
            Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username())
        );
        if (exists > 0) {
            throw new BusinessException(ApiResponse.CONFLICT, "用户名已被注册");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setStatus(User.STATUS_ACTIVE);
        userMapper.insert(user);

        log.info("新用户注册: username={}, userId={}", request.username(), user.getId());
        return UserVO.from(user);
    }

    @Override
    public UserVO login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 防暴力登录：冷却期内直接拒绝
        if (loginAttemptLimiter.isBlocked(request.username())) {
            throw new BusinessException(ApiResponse.UNAUTHORIZED, "登录尝试次数过多，请 15 分钟后再试");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            // Spring Security 6 必须手动保存 SecurityContext 到 Session，
            // 否则下一个请求就"失忆"了
            SecurityContext context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.getContextHolderStrategy().setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);
        } catch (SessionAuthenticationException e) {
            throw new BusinessException(ApiResponse.CONFLICT, "该账号已在其他设备登录");
        } catch (AuthenticationException e) {
            loginAttemptLimiter.recordFailure(request.username());
            // 统一提示，不区分"用户不存在"与"密码错误"，防止用户名枚举
            throw new BusinessException(ApiResponse.UNAUTHORIZED, "用户名或密码错误");
        }

        // 登录成功：清除失败计数并刷新最后登录时间（不参与主流程，失败不回滚登录态）
        loginAttemptLimiter.reset(request.username());
        updateLastLoginAt(request.username());

        return getCurrentUser();
    }

    @Override
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            // 官方推荐：清理 SecurityContext 并注销 Session
            new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);
        }
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = requireLogin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ApiResponse.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        return UserVO.from(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = requireUser();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ApiResponse.PARAM_ERROR, "原密码错误");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);

        // 密码已变，立即注销当前会话，要求重新登录
        logout(httpRequest, httpResponse);
    }

    @Override
    @Transactional
    public void deleteAccount(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = requireUser();

        // 先按依赖顺序清理全部个人数据与媒体文件
        userCleanupService.cleanupAllUserData(user.getId());

        // 再删除用户本体并注销会话
        userMapper.deleteById(user.getId());
        logout(httpRequest, httpResponse);

        log.info("账号已注销: username={}, userId={}", user.getUsername(), user.getId());
    }

    // ---- 内部辅助 ----

    /**
     * 当前登录用户 ID，未登录抛 40101
     */
    private Long requireLogin() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ApiResponse.UNAUTHORIZED, "未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 当前登录用户实体（附带存在性校验）
     */
    private User requireUser() {
        Long userId = requireLogin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ApiResponse.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        return user;
    }

    /**
     * 登录成功后刷新 last_login_at（UTC）
     */
    private void updateLastLoginAt(String username) {
        User user = userMapper.selectOne(
            Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        );
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
            userMapper.updateById(user);
        }
    }
}